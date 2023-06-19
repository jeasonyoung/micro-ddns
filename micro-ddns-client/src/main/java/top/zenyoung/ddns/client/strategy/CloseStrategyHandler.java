package top.zenyoung.ddns.client.strategy;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.zenyoung.ddns.client.config.ClientProperties;
import top.zenyoung.ddns.client.util.TargetAttrUtils;
import top.zenyoung.ddns.client.work.TargetClient;
import top.zenyoung.ddns.codec.ClosePayload;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

/**
 * 内部(客户端)-关闭连接-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloseStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {
    private final ClientProperties properties;

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.Close.name()};
    }

    @Async
    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> msg) {
        Optional.ofNullable((ClosePayload) msg.getPayload())
                .ifPresent(body -> {
                    //连接标识
                    final String tag = msg.getTag();
                    if (Strings.isNullOrEmpty(tag)) {
                        //设置是否重启标识
                        properties.setReconnect(body.isReconnect());
                        //服务器已关闭
                        log.warn("【代理服务器】已关闭[isReconnect: {}]=> {}", body.isReconnect(), body);
                        session.close();
                        return;
                    }
                    //关闭客户端连接
                    final TargetClient client = TargetAttrUtils.getAttrByTag(session, tag);
                    if (Objects.nonNull(client)) {
                        client.close();
                        log.info("【代理服务器】已关闭【目标服务器】通道: {}=> {}", tag, client);
                    }
                });
        return null;
    }
}
