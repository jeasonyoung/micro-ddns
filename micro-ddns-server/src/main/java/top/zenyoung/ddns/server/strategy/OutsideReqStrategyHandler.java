package top.zenyoung.ddns.server.strategy;

import io.netty.buffer.ByteBuf;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.SendConnectReqPayload;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.common.HostPort;
import top.zenyoung.ddns.server.codec.OutsideCommand;
import top.zenyoung.ddns.server.codec.OutsideMessage;
import top.zenyoung.ddns.server.codec.OutsideReqPayload;
import top.zenyoung.ddns.server.codec.OutsideResPayload;
import top.zenyoung.ddns.server.service.DnsMappingService;
import top.zenyoung.ddns.server.utils.InsideSessionUtils;
import top.zenyoung.ddns.server.utils.OutsideSessionUtils;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.server.config.NettyServerProperties;
import top.zenyoung.netty.session.Session;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 外部(访问)-请求-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutsideReqStrategyHandler extends BaseStrategyHandler<OutsideMessage<?>> {
    private final NettyServerProperties serverProperties;
    private final DnsMappingService dnsMappingService;

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{OutsideCommand.OutsideReq.name()};
    }

    @Async
    @Override
    public OutsideMessage<?> process(@Nonnull final Session session, @Nonnull final OutsideMessage<?> msg) {
        final OutsideReqPayload body = (OutsideReqPayload) msg.getPayload();
        if (Objects.nonNull(body)) {
            final DeviceType type = DeviceUtils.parseDeviceType(msg.getDeviceId());
            Assert.notNull(type, "设备ID不合法:" + msg.getDeviceId());
            Assert.isTrue(type == DeviceType.Outside, "消息不合法![type: " + type + "]=>" + body);
            //检查参数
            Assert.hasText(body.getHost(), "映射前'host'不能为空");
            Assert.notNull(body.getPort(), "映射前'port'不能为空");
            final String tag;
            Assert.hasText(tag = msg.getTag(), "'tag'不能为空");
            //检查映射关系
            final DnsMappingService.DnsMappingInside inside = dnsMappingService.mappingInside(body);
            if (Objects.isNull(inside)) {
                denyAccessHandler(session, body, tag);
                log.warn("【{}】访问目标未被映射,禁止访问=>{}", msg.getCommand(), format(body));
                return null;
            }
            //检查访问端口是否合法
            if (!checkAccessPort(body)) {
                denyAccessHandler(session, body, tag);
                log.warn("访问端口【{}】不合法=> {}", msg.getCommand(), body.getPort());
                return null;
            }
            //检查映射后参数
            Assert.hasText(inside.getHost(), "映射后'host'不能为空");
            Assert.hasText(inside.getSn(), "映射后'sn'不能为空");
            Assert.notNull(inside.getPort(), "映射后'port'不能为空");
            //正常访问处理
            accessHandler(session, body, inside, tag);
        }
        return null;
    }

    private boolean checkAccessPort(@Nonnull final HostPort hostPort) {
        final Integer port = hostPort.getPort();
        final List<Integer> denyPorts = serverProperties.getCodec().keySet().stream()
                .filter(p -> Objects.nonNull(p) && p > 0)
                .distinct()
                .collect(Collectors.toList());
        //检查端口是否合法
        return Objects.nonNull(port) && !denyPorts.contains(port);
    }

    private void denyAccessHandler(@Nonnull final Session session, @Nonnull final HostPort body, @Nonnull final String tag) {
        final OutsideResPayload payload = new OutsideResPayload();
        payload.setRet(false);
        payload.setMsg("访问地址不合法:" + format(body));
        final OutsideMessage<?> message = OutsideMessage.createOutsideRes(payload);
        message.setDeviceId(InsideDeviceUtils.createServerDeviceId(session.getChannelId()));
        message.setTag(tag);
        session.send(message, f -> {
            //发送成功关闭连接
            if (f.isSuccess()) {
                NettyUtils.closeOnFlush(f.channel());
            }
        });
    }

    private void accessHandler(@Nonnull final Session session, @Nonnull final OutsideReqPayload body,
                               @Nonnull final DnsMappingService.DnsMappingInside inside, @Nonnull final String tag) {
        //设备标识
        final String sn = inside.getSn();
        //向内部(客户端)发送连接请求
        final Session insideSession = InsideSessionUtils.getBySn(sn);
        Assert.notNull(insideSession, sn + ",设备指令会话不存在");
        //记录连接标识与内部会话的关联
        InsideSessionUtils.putTag(tag, insideSession);
        //外部会话缓存
        OutsideSessionUtils.putTag(tag, session);
        //检查是否有缓存数据
        final ByteBuf data;
        if (Objects.nonNull(data = body.getBody()) && data.readableBytes() > 0) {
            //缓存数据
            OutsideSessionUtils.addCacheQueue(tag, data);
        }
        //构建内部请求消息
        final InsideMessage<?> message = InsideMessage.createSendConnectReq(SendConnectReqPayload.of(inside));
        //设备标识
        message.setDeviceId(InsideDeviceUtils.createServerDeviceId(insideSession.getChannelId()));
        //连接标识
        message.setTag(tag);
        //转发数据
        insideSession.send(message, f -> {
            final String source = format(body), target = format(inside);
            final boolean ret = f.isSuccess();
            log.info("转发【外部访问: {}】连接请求({} => {})  {}", sn, source, target, ret ? "成功" : "失败," + NettyUtils.failMessage(f));
            if (ret) {
                f.channel().read();
                session.readChannelData();
            }
        });
    }

    private String format(@Nonnull final HostPort hostPort) {
        return hostPort.getHost() + ":" + hostPort.getPort();
    }
}
