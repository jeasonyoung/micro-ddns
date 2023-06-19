package top.zenyoung.ddns.client.work;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import top.zenyoung.ddns.client.util.TargetAttrUtils;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.ReceiveConnectResPayload;
import top.zenyoung.ddns.common.HostPort;
import top.zenyoung.netty.BaseNettyImpl;
import top.zenyoung.netty.client.config.NettyClientProperties;
import top.zenyoung.netty.config.BaseProperties;
import top.zenyoung.netty.session.Session;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

/**
 * 目标客户端接口-默认实现
 *
 * @author young
 */
@Slf4j
@RequiredArgsConstructor(staticName = "of")
public class TargetClientDefault extends BaseNettyImpl<BaseProperties> implements TargetClient, ChannelInboundHandler {
    private final NettyClientProperties properites;
    private final Session inside;
    private final String tag;
    private final String deviceId;

    private Channel inbound;

    @Override
    protected BaseProperties getProperties() {
        return properites;
    }

    @Override
    protected void initChannelPipelineHandler(final int port, @Nonnull final ChannelPipeline pipeline) {
        super.initChannelPipelineHandler(port, pipeline);
        //添加本地处理器
        pipeline.addLast("local", this);
    }

    private void sendConnectRes(@Nullable final ChannelHandlerContext ctx, final boolean ret, final String msg) {
        //连接反馈
        final ReceiveConnectResPayload payload = ReceiveConnectResPayload.of(ret, msg);
        final InsideMessage<?> message = InsideMessage.createReceiveConnectRes(payload);
        //设备ID
        message.setDeviceId(deviceId);
        //设备标识
        message.setTag(tag);
        //发送结果处理
        final ChannelFutureListener futureListener = Objects.isNull(ctx) ? null : f -> {
            //检查是否发送成功
            if (f.isSuccess()) {
                f.channel().read();
                ctx.read();
            }
        };
        //发送消息
        inside.send(message, futureListener);
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        this.inbound = ctx.channel();
        log.info("连接【目标服务器】反馈[tag: {}]=>成功", tag);
        sendConnectRes(ctx, true, "连接成功.");
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        final ByteBuf data;
        final int readableBytes;
        if (Objects.nonNull((data = (ByteBuf) msg)) && (readableBytes = data.readableBytes()) > 0) {
            //向服务器发送数据
            final InsideMessage<?> message = InsideMessage.createData(data);
            //设备ID
            message.setDeviceId(deviceId);
            //连接标识
            message.setTag(tag);
            //发送到服务器
            log.info("【目标服务器:{}】将上行数据: {}", tag, readableBytes);
            inside.send(message);
            inside.readChannelData();
            ctx.read();
        }
    }

    @Override
    public void channelInactive(final ChannelHandlerContext ctx) {
        log.warn("【目标服务器:{}】已关闭通道连接", tag);
        this.close();
    }

    @Override
    public void connect(@Nonnull final HostPort target) {
        final String host = target.getHost();
        final Integer port = target.getPort();
        Assert.hasText(host, "'target.host'值不能为空");
        Assert.notNull(port, "'target.port'值不能为空");
        log.info("开始连接【目标服务器: {}】[{}:{}]...", tag, host, port);
        //创建客户端启动对象
        final Bootstrap bootstrap = new Bootstrap();
        //构建Bootstrap配置
        this.buildBootstrap(bootstrap, () -> IS_EPOLL ? EpollSocketChannel.class : NioSocketChannel.class);
        //连接服务器
        bootstrap.connect(host, port)
                .addListener(future -> {
                    final boolean ret = future.isSuccess();
                    if (!ret) {
                        final String fail = NettyUtils.failMessage(future);
                        log.info("连接【目标服务器: {}】[{}:{}]=>失败,{}", tag, host, port, fail);
                        sendConnectRes(null, false, "连接失败:" + fail);
                    }
                });
    }

    @Override
    public void sendData(@Nonnull final ByteBuf data) {
        log.info("向【目标服务器: {}】将发送数据: {}", tag, data.readableBytes());
        NettyUtils.writeAndFlush(inbound, data);
        inbound.read();
    }

    @Override
    public void close() {
        try {
            //关闭连接
            NettyUtils.closeOnFlush(inbound, f -> log.warn("关闭【目标服务器: {}】连接-{}", tag, f.isSuccess()));
        } finally {
            //移除相关设置
            TargetAttrUtils.cleanAttr(inside, tag);
        }
    }
}
