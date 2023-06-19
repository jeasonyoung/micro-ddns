package top.zenyoung.ddns.server.strategy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.server.codec.OutsideMessage;
import top.zenyoung.ddns.server.utils.OutsideSessionUtils;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

/**
 * 内部(客户端)-数据-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsideDataStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.Data.name()};
    }

    @Async
    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> msg) {
        Optional.ofNullable((ByteBuf) msg.getPayload())
                .ifPresent(body -> {
                    //检查设备ID
                    final DeviceType type = DeviceUtils.parseDeviceType(msg.getDeviceId());
                    Assert.notNull(type, "设备ID不合法:" + msg.getDeviceId());
                    Assert.isTrue(type == DeviceType.Inside, "消息不合法![type: " + type + "]=>" + msg);
                    //连接标识
                    final String tag = msg.getTag();
                    Assert.hasText(tag, "'body.tag'不能为空");
                    //根据连接标签查询
                    final Session outside = OutsideSessionUtils.getByTag(tag);
                    if (Objects.isNull(outside)) {
                        final String hex = ByteBufUtil.prettyHexDump(body);
                        log.warn("{}【{}】外部访问会话已不存在了\r\n {}", msg.getCommand(), tag, hex);
                        return;
                    }
                    final OutsideMessage<?> message = OutsideMessage.createOutsideData(body);
                    //设备ID
                    message.setDeviceId(InsideDeviceUtils.createServerDeviceId(session.getChannelId()));
                    //连接标识
                    message.setTag(tag);
                    //发送给外部访问者
                    outside.send(message);
                    session.readChannelData();
                    outside.readChannelData();
                });
        return null;
    }
}
