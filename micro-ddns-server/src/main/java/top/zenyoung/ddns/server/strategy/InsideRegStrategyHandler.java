package top.zenyoung.ddns.server.strategy;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.RegResPayload;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.server.utils.InsideSessionUtils;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

/**
 * 内部(客户端)-设备注册-策略处理器
 *
 * @author young
 */
@Component
@RequiredArgsConstructor
public class InsideRegStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.RegReq.name()};
    }

    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> msg) {
        final RegResPayload payload = Optional.ofNullable((String) msg.getPayload())
                .filter(sn -> !Strings.isNullOrEmpty(sn))
                .map(sn -> {
                    //检查设备ID
                    final DeviceType deviceType = DeviceUtils.parseDeviceType(msg.getDeviceId());
                    if (Objects.isNull(deviceType)) {
                        return RegResPayload.of(false, "设备ID不合法:" + msg.getDeviceId());
                    }
                    if (deviceType != DeviceType.Inside) {
                        return RegResPayload.of(false, "设备类型不合法:" + deviceType.name());
                    }
                    //设备已注册
                    InsideSessionUtils.putSn(sn, session);
                    //注册成功处理
                    return RegResPayload.of(true, "成功");
                })
                .orElse(RegResPayload.of(false, "设备标识未传"));
        final InsideMessage<?> message = InsideMessage.createRegRes(payload);
        //设置设备ID
        message.setDeviceId(InsideDeviceUtils.createServerDeviceId(session.getChannelId()));
        //连接标识
        message.setTag(null);
        //响应数据处理
        return message;
    }
}
