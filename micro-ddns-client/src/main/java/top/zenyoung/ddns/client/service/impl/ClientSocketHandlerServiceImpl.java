package top.zenyoung.ddns.client.service.impl;

import com.google.common.base.Strings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import top.zenyoung.ddns.client.config.ClientProperties;
import top.zenyoung.ddns.client.service.ClientSocketHandlerService;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.InsideMessageCodec;
import top.zenyoung.ddns.codec.PingPayload;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.client.handler.BaseClientSocketHandler;
import top.zenyoung.netty.client.server.NettyClient;
import top.zenyoung.netty.session.Session;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;

/**
 * 客户端-通道处理器-服务接口实现
 *
 * @author young
 */
@Slf4j
@Service
@Scope("prototype")
@RequiredArgsConstructor
public class ClientSocketHandlerServiceImpl extends BaseClientSocketHandler<InsideMessage<?>> implements ClientSocketHandlerService {
    private final ClientProperties properties;
    private final ApplicationContext context;

    @Override
    protected void addCodec(@Nonnull final ChannelHandlerContext ctx) {
        final ChannelPipeline p = ctx.pipeline();
        final String name = ctx.name();
        //2.挂载 协议解析
        p.addBefore(name, "ddns-inside-codec", new InsideMessageCodec());
        //
        log.info("codecs: {}", p.names());
    }

    @Override
    protected void heartbeatIdleHandle(@Nonnull final ChannelHandlerContext ctx,
                                       @Nonnull final Session session,
                                       @Nonnull final IdleState state) {
        //获取设备SN
        final String sn = properties.getSn();
        if (!Strings.isNullOrEmpty(sn)) {
            //创建心跳请求
            final PingPayload payload = PingPayload.of(sn, System.currentTimeMillis());
            final InsideMessage<?> message = InsideMessage.createPing(payload);
            //设备ID
            message.setDeviceId(InsideDeviceUtils.createClientDeviceId(ctx));
            //客户端发送心跳请求
            NettyUtils.writeAndFlush(ctx, message, future -> {
                final boolean ret = future.isSuccess();
                log.info("向【代理服务器】发送心跳请求[{}]=> {}", ret, payload);
                if (ret) {
                    //发送成功,准备读取通道数据
                    future.channel().read();
                }
            });
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        //检查重启标识
        final boolean reconnect = properties.isReconnect();
        if (!reconnect) {
            log.warn("与【代理服务器】的重连标识,已被关闭");
            return;
        }
        //通道关闭,重新连接服务器
        final NettyClient client = context.getBean(NettyClient.class);
        log.info("与【代理服务器】连接被关闭,开始主动重连...");
        client.connectServer();
    }
}
