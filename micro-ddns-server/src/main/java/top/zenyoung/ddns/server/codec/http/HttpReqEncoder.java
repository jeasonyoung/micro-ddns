package top.zenyoung.ddns.server.codec.http;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestEncoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * HTTP请求编码器
 *
 * @author young
 */
@Slf4j
public class HttpReqEncoder extends HttpRequestEncoder {

    /**
     * HTTP请求编码处理
     *
     * @param ctx 通道处理上下文
     * @param req Http请求对象
     * @return ByteBuf数据
     */
    public ByteBuf encode(@Nonnull final ChannelHandlerContext ctx, @Nonnull final HttpRequest req) {
        try {
            final List<Object> outs = Lists.newLinkedList();
            super.encode(ctx, req, outs);
            if (!CollectionUtils.isEmpty(outs)) {
                return Unpooled.copiedBuffer(outs.stream()
                        .map(out -> {
                            if (out instanceof ByteBuf) {
                                return (ByteBuf) out;
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .toArray(ByteBuf[]::new)
                );
            }
        } catch (Throwable e) {
            log.error("HTTP请求编码异常-exp: {}", e.getMessage());
        }
        return null;
    }
}
