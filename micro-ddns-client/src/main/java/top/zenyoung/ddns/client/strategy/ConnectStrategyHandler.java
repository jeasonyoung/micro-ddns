package top.zenyoung.ddns.client.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import top.zenyoung.ddns.client.util.TargetAttrUtils;
import top.zenyoung.ddns.client.work.TargetClient;
import top.zenyoung.ddns.client.work.TargetClientDefault;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.ReceiveConnectResPayload;
import top.zenyoung.ddns.codec.SendConnectReqPayload;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.client.config.NettyClientProperties;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 内部(客户端)-连接目标请求-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConnectStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {
    private final NettyClientProperties properties;

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.SendConnectReq.name()};
    }

    @Async
    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> msg) {
        final SendConnectReqPayload body = (SendConnectReqPayload) msg.getPayload();
        if (Objects.nonNull(body)) {
            final String tag;
            Assert.hasText(tag = msg.getTag(), "'body.tag'不能为空");
            final String tHost;
            Assert.hasText(tHost = body.getHost(), "'host'不能为空");
            //设备ID
            final String deviceId = InsideDeviceUtils.createClientDeviceId(session.getChannelId());
            //检查连接地址是否合法
            if (tHost.equalsIgnoreCase(properties.getHost()) && body.getPort().equals(properties.getPort())) {
                final String err = "目标地址不合法=> " + tHost + ":" + body.getPort();
                log.error("连接目标不合法,{}【{}】{}=> {}", msg.getCommand(), tag, body, err);
                final ReceiveConnectResPayload payload = ReceiveConnectResPayload.of(false, "连接失败:" + err);
                final InsideMessage<?> message = InsideMessage.createReceiveConnectRes(payload);
                //设备ID
                message.setDeviceId(deviceId);
                //设备标识
                message.setTag(tag);
                //返回响应报文
                return message;
            }
            log.info("准备连接【目标服务器: {}】=> {}:{}", msg.getTag(), body.getHost(), body.getPort());
            //目标客户端
            final TargetClient client = TargetAttrUtils.putIfAbsent(session, tag, () -> TargetClientDefault.of(properties, session, tag, deviceId));
            //连接访问目标
            client.connect(body);
        }
        return null;
    }
}
