package top.zenyoung.ddns.codec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.DefaultInstantiatorStrategy;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.util.Assert;
import org.xerial.snappy.Snappy;
import top.zenyoung.netty.codec.BaseByteToMessageCodec;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;

/**
 * 内部协议消息-编解码器
 * <pre>
 *     协议结构
 *     |----------------------------------- 报文头(head) ------------------------|
 *     |-MDDNS(协议标识: 5 bytes)-|-版本(1 byte)-|-指令(1 byte)-|报文长度(4 bytes)--|
 *     |--------------------------------- 报文体(body) -------------------------|
 *     |-----设备ID----|----连接标识----|----------------业务数据-----------------|
 * </pre>
 *
 * @author young
 */
@Slf4j
public class InsideMessageCodec extends BaseByteToMessageCodec<InsideMessage<?>> {
    private static final byte[] MAGIC = {'M', 'D', 'D', 'N', 'S'};
    private static final byte VERSION = 0x01;

    /**
     * 基本报文长度: 魔术长度(5 byte) + 版本长度(1 byte) + 指令长度(1 byte)
     */
    private static final int BASE_HEAD_LENGTH = MAGIC.length + 1 + 1;

    private static final ThreadLocal<Kryo> LOCAL_KRYO = ThreadLocal.withInitial(() -> {
        final Kryo kryo = new Kryo();
        kryo.setReferences(true);
        kryo.setRegistrationRequired(false);
        kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
        ((DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy()).setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
        return kryo;
    });

    private static Kryo getKryoIntance() {
        return LOCAL_KRYO.get();
    }

    public static int getHeaderLen(final int payloadLength) {
        //[魔术长度 + 版本长度(1byte) + 指令长度(1byte)] + 报文体长度
        return BASE_HEAD_LENGTH + computePayloadSize(payloadLength);
    }

    @Override
    protected void encode(@Nonnull final ChannelHandlerContext ctx, @Nonnull final InsideMessage<?> msg, @Nonnull final ByteBuf out) {
        //指令
        final Command command = Command.parseByName(msg.getCommand());
        Assert.notNull(command, msg.getCommand() + ",协议指令不合法");
        //消息处理
        final byte[] data = toBytesHandler(msg);
        final int dataLen = data.length, totals = getHeaderLen(dataLen) + dataLen;
        //申请数据块
        out.ensureWritable(totals);
        //1.写入魔数
        out.writeBytes(MAGIC);
        //2.写入版本
        out.writeByte(VERSION);
        //3.写入指令
        out.writeByte(command.getVal());
        //4.写入载荷长度
        writeRawVarint32(out, dataLen);
        //5.写入载荷数据
        out.writeBytes(data, 0, dataLen);
    }

    private boolean checkMagic(final byte[] magic) {
        final int magicLengh = MAGIC.length;
        if (magic.length != magicLengh) {
            return false;
        }
        for (int i = 0; i < magicLengh; i++) {
            if (magic[i] != MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private DecodeHeader decodeHeader;

    @Override
    protected InsideMessage<?> decode(@Nonnull final ChannelHandlerContext ctx, @Nonnull final ByteBuf in) {
        //检查解码头对象
        if (decodeHeader == null) {
            //1.检查是否符合报文基本长度(基本长度+4位数据长度标识)
            final int min = BASE_HEAD_LENGTH + 4, rd;
            if ((rd = in.readableBytes()) < min) {
                log.warn("可读数据长度[{}]小于协议头标识长度: {}", rd, min);
                return null;
            }
            final int magicLengh = MAGIC.length;
            final byte[] magic = new byte[magicLengh];
            in.readBytes(magic, 0, magicLengh);
            if (!checkMagic(magic)) {
                log.warn("协议头不符合=> {}", magic);
                return null;
            }
            //2.检查版本
            final byte ver = in.readByte();
            if (ver != VERSION) {
                log.warn("版本[{}]不符合.", ver);
                return null;
            }
            //3.协议指令
            final byte cmd = in.readByte();
            final Command command = Command.parseByVal((int) cmd);
            if (Objects.isNull(command)) {
                log.warn("协议指令[{}]不合法.", cmd);
                return null;
            }
            //4.获取载荷长度
            final int preIndex = in.readerIndex();
            final int length = readRawVarint32(in);
            if (preIndex == in.readerIndex()) {
                log.warn("获取载荷长度无效: {}", length);
                return null;
            }
            //初始化解码头
            decodeHeader = DecodeHeader.of(ver, command, length);
        }
        //读取数据
        decodeHeader.readHandler(in);
        //检查是否已读取完成
        if (decodeHeader.checkRead()) {
            final ByteBuf data = decodeHeader.toData();
            final Command command = decodeHeader.getCommand();
            try {
                return fromBytesHandler(data, command);
            } finally {
                decodeHeader = null;
            }
        }
        return null;
    }

    private static byte[] toBytesHandler(@Nonnull final InsideMessage<?> msg) {
        int rawLen = 1, zipLen = 0;
        try {
            //序列化处理
            final byte[] raw = serialize(msg);
            rawLen = Math.max(raw.length, rawLen);
            //压缩处理
            final byte[] zip = compress(raw);
            zipLen = zip.length;
            return zip;
        } finally {
            final int rate = (int) (((rawLen - zipLen) / (double) rawLen) * 100);
            log.info("[{}]压缩前数据: {},压缩后数据: {}, 压缩率: {}.", msg.getCommand(), rawLen, zipLen, rate <= 0 ? "-" : rate + "%");
        }
    }

    private static InsideMessage<?> fromBytesHandler(@Nonnull final ByteBuf in, @Nonnull final Command command) {
        int rawLen = 1, zipLen = 0;
        try {
            final byte[] zip = ByteBufUtil.getBytes(in);
            zipLen = zip.length;
            //解压缩
            final byte[] raw = uncompress(zip);
            rawLen = Math.max(raw.length, rawLen);
            //反序列化处理
            return deserialize(raw, command);
        } finally {
            final int rate = (int) (((rawLen - zipLen) / (double) rawLen) * 100);
            log.info("[{}]解压前数据: {},解压后数据: {}, 压缩率: {}.", command, zipLen, rawLen, rate <= 0 ? "-" : rate + "%");
        }
    }

    private static <T extends InsideMessage<?>> byte[] serialize(@Nonnull final T data) {
        try (final ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            final Output output = new Output(byteStream);
            final Kryo kryo = getKryoIntance();
            if (data instanceof InsideMessageData) {
                kryo.writeObject(output, InsideMessageDataInner.of((InsideMessageData) data));
            } else {
                kryo.writeObject(output, data);
            }
            output.flush();
            return byteStream.toByteArray();
        } catch (Throwable e) {
            log.error("serialize[data: {}]-exp: {}", data, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static InsideMessage<?> deserialize(@Nonnull final byte[] raw, @Nonnull final Command command) {
        if (raw.length == 0) {
            return null;
        }
        try (final ByteArrayInputStream byteStream = new ByteArrayInputStream(raw)) {
            final Input input = new Input(byteStream);
            final Kryo kryo = getKryoIntance();
            if (command == Command.Data) {
                final InsideMessageDataInner inner = kryo.readObjectOrNull(input, InsideMessageDataInner.class);
                return inner == null ? null : inner.toData();
            }
            return kryo.readObjectOrNull(input, command.getCls());
        } catch (Throwable e) {
            log.error("deserialize-exp: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static byte[] compress(@Nonnull final byte[] input) {
        if (input.length == 0) {
            return input;
        }
        try {
            return Snappy.compress(input);
        } catch (Throwable e) {
            throw new RuntimeException("压缩异常:" + e.getMessage());
        }
    }

    private static byte[] uncompress(@Nonnull final byte[] input) {
        if (input.length == 0) {
            return input;
        }
        try {
            return Snappy.uncompress(input);
        } catch (Throwable e) {
            throw new RuntimeException("解压缩异常:" + e.getMessage());
        }
    }

    private static int computePayloadSize(final int value) {
        int count = 0, num = value;
        do {
            num /= 128;
            count += 1;
        } while (num > 0);
        return count;
    }

    private static void writeRawVarint32(@Nonnull final ByteBuf out, final int value) {
        int digit, num = value;
        do {
            digit = num % 128;
            num /= 128;
            if (num > 0) {
                digit |= 0x80;
            }
            out.writeByte(digit & 0xff);
        } while (num > 0);
    }

    private static int readRawVarint32(@Nonnull final ByteBuf out) {
        int length = 0, multiplier = 1;
        short digit;
        do {
            //读取后续字节
            digit = out.readUnsignedByte();
            length += (digit & 127) * multiplier;
            multiplier *= 128;
        } while ((digit & 128) != 0);
        return length;
    }

    @Getter
    @RequiredArgsConstructor(staticName = "of")
    private static class DecodeHeader {
        private final byte ver;
        private final Command command;
        private final int dataBytes;
        private int readableBytes = 0;

        private final List<ByteBuf> items = Lists.newLinkedList();

        public void readHandler(@Nonnull final ByteBuf in) {
            //检查应读取的长度
            final int should = dataBytes - readableBytes;
            if (should > 0) {
                final int max = in.readableBytes();
                final int len = Math.min(should, max);
                if (len > 0) {
                    final ByteBuf data = in.readRetainedSlice(len);
                    if (Objects.nonNull(data)) {
                        items.add(data);
                        readableBytes += len;
                    }
                }
            }
        }

        public boolean checkRead() {
            return this.readableBytes == this.dataBytes;
        }

        public ByteBuf toData() {
            return Unpooled.wrappedBuffer(items.toArray(new ByteBuf[0]));
        }
    }
}
