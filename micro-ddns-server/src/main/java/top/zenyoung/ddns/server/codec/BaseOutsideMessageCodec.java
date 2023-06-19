package top.zenyoung.ddns.server.codec;

import io.netty.channel.ChannelHandlerContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.util.Assert;
import top.zenyoung.ddns.common.HostPort;
import top.zenyoung.netty.codec.BaseMessageToMessageCodec;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * 外部访问-编解码器基类
 *
 * @param <R> 外部访问协议
 * @author young
 */
public abstract class BaseOutsideMessageCodec<R> extends BaseMessageToMessageCodec<R, OutsideMessage<?>> {
    /**
     * 构建连接标识
     *
     * @param ctx    通道上下文
     * @param target 连接目标
     * @return 连接标识
     */
    protected static String buildTag(@Nonnull final ChannelHandlerContext ctx, @Nonnull final HostPort target) {
        final String channelId = NettyUtils.getChannelId(ctx);
        //地址
        final String host;
        Assert.hasText(host = target.getHost(), "'HostPort.host'不能为空");
        //端口
        final Integer port = Optional.ofNullable(target.getPort()).orElse(HostPort.DEF_PORT);
        //md5处理
        return DigestUtils.md5Hex(channelId + "," + host + ":" + port);
    }
}
