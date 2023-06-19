package top.zenyoung.ddns.server.strategy;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import top.zenyoung.ddns.codec.ClosePayload;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.server.utils.OutsideSessionUtils;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * 内部(客户端)-关闭-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsideCloseStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {

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
                    //检查设备类型
                    final DeviceType type = DeviceUtils.parseDeviceType(msg.getDeviceId());
                    Assert.notNull(type, msg.getDeviceId() + ",设备标识不合法");
                    //连接标识(连接标识为空表示为客户端通道关闭)
                    final String tag = msg.getTag();
                    if (Strings.isNullOrEmpty(tag)) {
                        return;
                    }
                    //外部访问
                    log.info("访问关闭通知[type: {},tag:{}]: {}, {}", type, tag, msg.getDeviceId(), body.getStamp());
                    OutsideSessionUtils.clean(tag);
                });
        return null;
    }
}
