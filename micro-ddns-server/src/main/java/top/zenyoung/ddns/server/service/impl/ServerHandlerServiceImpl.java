package top.zenyoung.ddns.server.service.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.PongPayload;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.server.service.ServerHandlerService;
import top.zenyoung.ddns.server.utils.InsideSessionUtils;
import top.zenyoung.ddns.server.utils.OutsideSessionUtils;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.server.handler.BaseServerSocketHandler;
import top.zenyoung.netty.session.Session;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * 服务端-业务处理接口实现
 *
 * @author young
 */
@Slf4j
@Service
@Scope("prototype")
@RequiredArgsConstructor
public class ServerHandlerServiceImpl extends BaseServerSocketHandler<InsideMessage<?>> implements ServerHandlerService {

    @Override
    protected void heartbeatIdleHandle(@Nonnull final ChannelHandlerContext ctx, @Nonnull final Session session,
                                       @Nonnull final IdleState state) {
        //全空闲处理
        if (state == IdleState.ALL_IDLE) {
            final DeviceType deviceType = DeviceUtils.parseDeviceType(session.getDeviceId());
            if (deviceType == DeviceType.Inside) {
                final InsideMessage<?> message = InsideMessage.createPong(PongPayload.of(0L, System.currentTimeMillis()));
                //设备ID
                message.setDeviceId(InsideDeviceUtils.createServerDeviceId(session.getChannelId()));
                //连接标识
                message.setTag(null);
                //发送心跳
                NettyUtils.writeAndFlush(ctx, message);
            }
        }
    }

    @Nullable
    @Override
    protected InsideMessage<?> receivedMessageConvert(@Nonnull final Object msg) {
        return (InsideMessage<?>) msg;
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        Optional.ofNullable(getSession())
                .ifPresent(s -> {
                    final String channelId = NettyUtils.getChannelId(ctx);
                    if (channelId.equalsIgnoreCase(s.getChannelId())) {
                        this.close(s);
                    }
                });
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        final String channelId = NettyUtils.getChannelId(ctx), stackTrace = NettyUtils.getPrintStackTrace(cause);
        log.warn("通道[{}]发生异常-exp: {} \r\n {}", channelId, cause.getMessage(), stackTrace);
    }

    @Override
    protected void close(@Nonnull final Session session) {
        log.warn("通道[{}][{}]发生关闭[active: {}]: {}", session.getChannelId(), session.getDeviceId(), session.isActive(), session);
        final DeviceType deviceType = DeviceUtils.parseDeviceType(session.getDeviceId());
        if (Objects.nonNull(deviceType)) {
            //内部访问处理
            if (deviceType == DeviceType.Inside) {
                session.execute(InsideSessionUtils::checkTraversal);
                return;
            }
            //外部访问处理
            if (deviceType == DeviceType.Outside) {
                //触发清理处理器
                session.execute(OutsideSessionUtils::checkTraversal);
            }
        }
    }
}
