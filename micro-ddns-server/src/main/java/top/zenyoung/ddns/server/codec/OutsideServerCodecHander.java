package top.zenyoung.ddns.server.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.socksx.SocksPortUnificationServerHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import top.zenyoung.ddns.server.codec.http.HttpOutsideMessageCodec;
import top.zenyoung.ddns.server.codec.socks.SocksOutsideMessageCodec;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * 外部访问-编解码处理器
 *
 * @author young
 */
@Slf4j
@Scope("prototype")
@Component("outsideServerCodec")
public class OutsideServerCodecHander extends ByteToMessageDecoder {
    private final static int CHECK_PROTOCOL_LEN = 5;

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
        //将使用前N个字节来检测协议
        final int len;
        if ((len = in.readableBytes()) < CHECK_PROTOCOL_LEN) {
            log.warn("数据长度小于检查协议最小字节: {}/{}.", len, CHECK_PROTOCOL_LEN);
            return;
        }
        final int readerIndex = in.readerIndex();
        //检查是否为Socks协议
        if (isSocks(in.getByte(readerIndex))) {
            toSocksHandler(ctx);
            return;
        }
        //检查是否为HTTP
        final int magic1 = in.getUnsignedByte(readerIndex),
                magic2 = in.getUnsignedByte(readerIndex + 1);
        if (isHttp(magic1, magic2)) {
            toHttpHanlder(ctx);
            return;
        }
        //未知协议
        log.warn("自动适配解析通信协议失败,关闭连接!");
        in.clear();
        ctx.close();
    }

    private static boolean isSocks(final byte versionVal) {
        final SocksVersion version = SocksVersion.valueOf(versionVal);
        return version != SocksVersion.UNKNOWN;
    }

    protected void toSocksHandler(@Nonnull final ChannelHandlerContext ctx) {
        log.info("[channelId: {}]解析到协议=> socks.", getChannelId(ctx));
        final ChannelPipeline p = ctx.pipeline();
        //OutsideMessage
        p.addAfter(ctx.name(), "socks-to-outside", new SocksOutsideMessageCodec());
        //Socks
        p.addAfter(ctx.name(), "socks", new SocksPortUnificationServerHandler());
        p.remove(this);
    }

    private static boolean isHttp(final int magic1, final int magic2) {
        // GET
        return magic1 == 'G' && magic2 == 'E' ||
                // POST
                magic1 == 'P' && magic2 == 'O' ||
                // PUT
                magic1 == 'P' && magic2 == 'U' ||
                // HEAD
                magic1 == 'H' && magic2 == 'E' ||
                // OPTIONS
                magic1 == 'O' && magic2 == 'P' ||
                // PATCH
                magic1 == 'P' && magic2 == 'A' ||
                // DELETE
                magic1 == 'D' && magic2 == 'E' ||
                // TRACE
                magic1 == 'T' && magic2 == 'R' ||
                // CONNECT
                magic1 == 'C' && magic2 == 'O';
    }

    protected void toHttpHanlder(@Nonnull final ChannelHandlerContext ctx) {
        log.info("[channelId: {}]解析到协议=> http.", getChannelId(ctx));
        final ChannelPipeline p = ctx.pipeline();
        //OutsideMessage
        p.addAfter(ctx.name(), "http-to-outside", new HttpOutsideMessageCodec());
        //HTTP
        p.addAfter(ctx.name(), "aggregator", new HttpObjectAggregator(1048576));
        p.addAfter(ctx.name(), "http-codec", new HttpServerCodec());
        p.remove(this);
    }

    private static String getChannelId(@Nonnull final ChannelHandlerContext ctx) {
        return ctx.channel().id().asShortText();
    }
}
