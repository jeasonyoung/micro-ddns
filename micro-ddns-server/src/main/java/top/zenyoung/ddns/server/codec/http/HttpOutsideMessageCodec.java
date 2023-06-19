package top.zenyoung.ddns.server.codec.http;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import top.zenyoung.ddns.common.HostPort;
import top.zenyoung.ddns.server.codec.*;
import top.zenyoung.ddns.server.utils.OutsideDeviceUtils;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * HTTP 外部访问-编解码器
 *
 * @author young
 */
@Slf4j
public class HttpOutsideMessageCodec extends BaseOutsideMessageCodec<Object> {
    private String tag;

    @Override
    protected Object encode(@Nonnull final ChannelHandlerContext ctx, @Nonnull final OutsideMessage<?> msg) {
        //消息类型
        final OutsideCommand cmd = msg.getOutsideCommand();
        Assert.notNull(cmd, msg.getCommand() + ",消息类型解析失败");
        switch (cmd) {
            //连接响应
            case OutsideRes: {
                final OutsideResPayload payload = (OutsideResPayload) msg.getPayload();
                if (Objects.nonNull(payload)) {
                    if (payload.isRet()) {
                        ctx.read();
                        return null;
                    }
                    final String callback = Optional.ofNullable(payload.getMsg())
                            .filter(err -> !Strings.isNullOrEmpty(err))
                            .orElse("请求服务失败!");
                    return Unpooled.wrappedBuffer(callback.getBytes(StandardCharsets.UTF_8));
                }
                log.warn("【{}】消息数据无效.", msg.getCommand());
                return null;
            }
            //响应数据
            case OutsideData: {
                try {
                    //数据消息
                    return msg.getPayload();
                } finally {
                    ctx.read();
                }
            }
            default: {
                log.warn("【{}】非法响应", msg.getCommand());
                return null;
            }
        }
    }

    @Override
    protected OutsideMessage<?> decode(@Nonnull final ChannelHandlerContext ctx, @Nonnull final Object in) {
        //HTTP请求处理
        if (in instanceof FullHttpRequest) {
            final FullHttpRequest httpReq = (FullHttpRequest) in;
            //请求对象
            final OutsideReqPayload payload = parseHttpReq(httpReq);
            if (Objects.isNull(payload)) {
                throw new IllegalArgumentException("解析HTTP请求失败");
            }
            //检查是否为SSL
            final boolean ssl = (443 == payload.getPort() || 8443 == payload.getPort());
            if (ssl) {
                //ssl发送响应
                NettyUtils.writeAndFlush(ctx, createHttpResOk(), f -> {
                    if (f.isSuccess()) {
                        //移除HTTP解码器
                        removeHttpCodecHandler(f.channel().pipeline());
                    }
                });
            } else {
                //普通HTTP处理
                final HttpReqEncoder httpReqEncoder = new HttpReqEncoder();
                final ByteBuf body = httpReqEncoder.encode(ctx, httpReq.retainedDuplicate());
                if (Objects.nonNull(body)) {
                    payload.setBody(body);
                }
                //移除HTTP解码器
                removeHttpCodecHandler(ctx.pipeline());
            }
            final OutsideMessage<?> message = OutsideMessage.createOutsideReq(payload);
            //设备ID
            message.setDeviceId(OutsideDeviceUtils.createDeviceId(ctx));
            //连接标识
            message.setTag(tag = buildTag(ctx, payload));
            //返回消息
            return message;
        }
        //检查数据
        if (in instanceof ByteBuf) {
            //连接标识
            Assert.hasText(tag, "'tag'不能为空");
            //消息报文
            final OutsideMessage<?> message = OutsideMessage.createOutsideData(((ByteBuf) in).retainedDuplicate());
            //设备ID
            message.setDeviceId(OutsideDeviceUtils.createDeviceId(ctx));
            //连接标识
            message.setTag(tag);
            //返回消息
            return message;
        }
        log.warn("消息不可解析=> {}", in);
        return null;
    }

    private OutsideReqPayload parseHttpReq(@Nonnull final HttpRequest req) {
        final HttpHeaders httpHeaders = req.headers();
        log.info("parseHttpReq=> {}", httpHeaders);
        final String hostPort = httpHeaders.get(HttpHeaderNames.HOST), sep = ":";
        if (!Strings.isNullOrEmpty(hostPort)) {
            final List<String> args = Splitter.on(sep).omitEmptyStrings().trimResults().splitToList(hostPort);
            if (!CollectionUtils.isEmpty(args)) {
                final OutsideReqPayload payload = new OutsideReqPayload();
                //主机地址
                payload.setHost(args.get(0));
                //主机端口
                payload.setPort(Optional.of(args)
                        .filter(arg -> arg.size() > 1)
                        .map(arg -> Integer.parseInt(arg.get(1)))
                        .orElse(HostPort.DEF_PORT)
                );
                //返回
                return payload;
            }
        }
        return null;
    }

    private FullHttpResponse createHttpResOk() {
        return new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, Unpooled.EMPTY_BUFFER);
    }

    private void removeHttpCodecHandler(@Nonnull final ChannelPipeline p) {
        //移除HTTP解码器
        p.remove(HttpServerCodec.class);
        p.remove(HttpObjectAggregator.class);
        log.info("移除HTTP相关编解码器后编解码=> {}", p.names());
    }
}
