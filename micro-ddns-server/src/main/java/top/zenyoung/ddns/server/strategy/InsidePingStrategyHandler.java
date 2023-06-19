package top.zenyoung.ddns.server.strategy;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.zenyoung.ddns.codec.*;
import top.zenyoung.ddns.server.utils.InsideSessionUtils;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * 内部(客户端)-心跳-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsidePingStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.Ping.name()};
    }

    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> msg) {
        return Optional.ofNullable((PingPayload) msg.getPayload())
                .map(payload -> {
                    //检查设备标识
                    final String sn;
                    if (Strings.isNullOrEmpty(sn = payload.getSn())) {
                        //发送关机响应消息
                        closeHandler(session, false);
                        log.warn("【客户端 deviceId: {}】 SN信息不能为空!", msg.getDeviceId());
                        return null;
                    }
                    //检查设备标识是否已注册
                    if (!InsideSessionUtils.checkSn(sn)) {
                        log.warn("【客户端 deviceId: {}】代理服务端中已丢失SN关联Session,需要客户端重新注册.", msg.getDeviceId());
                        //发送关机响应消息
                        closeHandler(session, true);
                        return null;
                    }
                    //构建心跳响应
                    final PongPayload body = PongPayload.of(payload.getStamp(), System.currentTimeMillis());
                    return InsideMessage.createPong(body);
                })
                .map(message -> {
                    //设备ID
                    message.setDeviceId(InsideDeviceUtils.createServerDeviceId(session.getChannelId()));
                    //连接标识
                    message.setTag(null);
                    //返回数据
                    return message;
                })
                .orElse(null);
    }

    private void closeHandler(@Nonnull final Session session, final boolean reconnect) {
        final ClosePayload body = ClosePayload.of(System.currentTimeMillis(), reconnect);
        final InsideMessage<?> message = InsideMessage.createClose(body);
        session.send(message, f -> {
            //发送成功,关闭连接
            if (f.isSuccess()) {
                NettyUtils.closeOnFlush(f.channel());
            }
        });
    }
}