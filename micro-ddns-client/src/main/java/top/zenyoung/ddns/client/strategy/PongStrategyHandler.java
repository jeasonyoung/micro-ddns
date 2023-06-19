package top.zenyoung.ddns.client.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.PongPayload;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * 内部(客户端)-心跳响应-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PongStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.Pong.name()};
    }

    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> msg) {
        Optional.ofNullable((PongPayload) msg.getPayload())
                .ifPresent(payload -> {
                    final long now = System.currentTimeMillis(), start = payload.getBefore(), callback = payload.getStamp();
                    log.info("接收到【代理服务器】心跳反馈【上行耗时:{}ms,下行耗时:{}ms,总耗时:{}ms】=> {}",
                            (callback - start), (now - callback), (now - start), payload);
                    //准备读取通道数据
                    session.readChannelData();
                });
        return null;
    }
}
