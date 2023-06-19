package top.zenyoung.ddns.client.strategy;

import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import top.zenyoung.ddns.client.util.TargetAttrUtils;
import top.zenyoung.ddns.client.work.TargetClient;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * 内部(客户端)-数据-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.Data.name()};
    }

    @Async
    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> msg) {
        //检查设备ID
        final DeviceType type = DeviceUtils.parseDeviceType(msg.getDeviceId());
        Assert.notNull(type, "设备ID不合法:" + msg.getDeviceId());
        Assert.isTrue(type == DeviceType.Inside, "消息不合法![type: " + type + "]=>" + msg.getDeviceId());
        //连接标识
        final String tag = msg.getTag();
        Assert.hasText(tag, "'body.tag'不能为空");
        //数据处理
        final ByteBuf payload = (ByteBuf) msg.getPayload();
        final int readableBytes;
        if (Objects.nonNull(payload) && (readableBytes = payload.readableBytes()) > 0) {
            log.info("将向【目标服务器: {}】发送数据: {}", tag, readableBytes);
            //目标客户端处理
            final TargetClient client = TargetAttrUtils.getAttrByTag(session, tag);
            if (Objects.isNull(client)) {
                log.warn("关联的【目标服务器: " + tag + "】已不存在!");
                return null;
            }
            //发送数据
            client.sendData(payload);
            //读取通道
            session.readChannelData();
        }
        return null;
    }
}
