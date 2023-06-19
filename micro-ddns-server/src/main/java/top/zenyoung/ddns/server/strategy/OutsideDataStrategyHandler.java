package top.zenyoung.ddns.server.strategy;

import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.server.codec.OutsideCommand;
import top.zenyoung.ddns.server.codec.OutsideMessage;
import top.zenyoung.ddns.server.utils.InsideSessionUtils;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * 外部(访问)-数据-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutsideDataStrategyHandler extends BaseStrategyHandler<OutsideMessage<?>> {

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{OutsideCommand.OutsideData.name()};
    }

    @Async
    @Override
    public OutsideMessage<?> process(@Nonnull final Session session, @Nonnull final OutsideMessage<?> msg) {
        Optional.ofNullable((ByteBuf) msg.getPayload())
                .ifPresent(body -> {
                    final int readableBytes = body.readableBytes();
                    if (readableBytes > 0) {
                        //检测设备ID
                        final DeviceType type = DeviceUtils.parseDeviceType(msg.getDeviceId());
                        Assert.notNull(type, "设备ID不合法:" + msg.getDeviceId());
                        Assert.isTrue(type == DeviceType.Outside, "消息不合法![type: " + type + "]=>" + msg);
                        //连接标识
                        final String tag = msg.getTag();
                        Assert.hasText(tag, "'body.tag'不能为空");
                        //根据连接标签查询
                        final Session inside = InsideSessionUtils.getByTag(tag);
                        Assert.notNull(inside, "内部客户端会话已不存在了:" + tag);
                        //构建消息
                        final InsideMessage<?> message = InsideMessage.createData(body);
                        //设置设备ID
                        message.setDeviceId(InsideDeviceUtils.createServerDeviceId(inside.getChannelId()));
                        //连接标识
                        message.setTag(tag);
                        //发送消息
                        inside.send(message);
                        //
                        inside.readChannelData();
                        session.readChannelData();
                        log.info("转发【外部访问: {}】数据=> {}", tag, readableBytes);
                    }
                });
        return null;
    }
}
