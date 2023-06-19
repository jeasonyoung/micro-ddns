package top.zenyoung.ddns.server.strategy;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.ReceiveConnectResPayload;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.server.codec.OutsideMessage;
import top.zenyoung.ddns.server.codec.OutsideResPayload;
import top.zenyoung.ddns.server.utils.OutsideSessionUtils;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 内部(客户端)-接收连接回复-策略处理器
 *
 * @author yangyong
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsideReceiveConnectStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.ReceiveConnectRes.name()};
    }

    @Async
    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> msg) {
        Optional.ofNullable((ReceiveConnectResPayload) msg.getPayload())
                .ifPresent(body -> {
                    //设备类型
                    final DeviceType type = DeviceUtils.parseDeviceType(msg.getDeviceId());
                    Assert.notNull(type, "设备ID不合法:" + msg.getDeviceId());
                    Assert.isTrue(type == DeviceType.Inside, "消息不合法![type: " + type + "]=>" + body);
                    //连接标识
                    final String tag = msg.getTag();
                    Assert.hasText(tag, "'body.tag'不能为空");
                    //加载外部访问
                    final Session outside = OutsideSessionUtils.getByTag(tag);
                    Assert.notNull(outside, "【外部访问: " + tag + "】会话不存在.");
                    log.info("转发【外部访问: {}】连接结果消息: {}", tag, body);
                    //构建连接反馈结果
                    final OutsideResPayload payload = OutsideResPayload.of(body);
                    final OutsideMessage<?> message = OutsideMessage.createOutsideRes(payload);
                    //设置设备ID
                    message.setDeviceId(InsideDeviceUtils.createServerDeviceId(session.getChannelId()));
                    //连接标识
                    message.setTag(tag);
                    //发送消息
                    outside.send(message);
                    //连接成功处理缓存数据
                    if (payload.isRet()) {
                        //发送缓存消息
                        sendOutsideCacheData(session, tag, message.getDeviceId());
                        //读取通道数据
                        session.readChannelData();
                    }
                    outside.readChannelData();
                });
        return null;
    }

    private void sendOutsideCacheData(@Nonnull final Session inside, @Nonnull final String tag, @Nonnull final String deviceId) {
        //发送缓存数据
        final List<ByteBuf> caches = Lists.newLinkedList();
        OutsideSessionUtils.cacheQueueHandler(tag, data -> {
            //检查数据
            if (Objects.nonNull(data) && data.readableBytes() > 0) {
                caches.add(data);
            }
        });
        if (!CollectionUtils.isEmpty(caches)) {
            final ByteBuf data = Unpooled.wrappedBuffer(caches.toArray(new ByteBuf[0]));
            final int readableBytes;
            if ((readableBytes = data.readableBytes()) > 0) {
                final InsideMessage<?> message = InsideMessage.createData(data);
                //设备ID
                message.setDeviceId(deviceId);
                //连接标签
                message.setTag(tag);
                //发送缓存数据
                inside.send(message);
                //连接目标成功,发送缓存数据
                log.info("向【代理客户端: {}】转发请求缓存数据: {}", tag, readableBytes);
            }
        }
    }
}
