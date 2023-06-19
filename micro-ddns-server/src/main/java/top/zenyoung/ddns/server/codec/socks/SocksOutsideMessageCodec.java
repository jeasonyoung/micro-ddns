package top.zenyoung.ddns.server.codec.socks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v4.*;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import top.zenyoung.ddns.server.codec.*;
import top.zenyoung.ddns.server.utils.OutsideDeviceUtils;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Socks 外部访问-编解码器
 *
 * @author young
 */
@Slf4j
public class SocksOutsideMessageCodec extends BaseOutsideMessageCodec<Object> {
    private SocksVersion socksVersion;
    private Socks4CommandType socks4CommandType;
    private Socks5CommandType socks5CommandType;
    private String tag;

    @Override
    protected Object encode(@Nonnull final ChannelHandlerContext ctx, @Nonnull final OutsideMessage<?> in) {
        //命令类型处理
        final OutsideCommand cmd = in.getOutsideCommand();
        Assert.notNull(cmd, in.getCommand() + ",不合法");
        //数据指令
        if (cmd == OutsideCommand.OutsideData) {
            return in.getPayload();
        }
        //响应指令
        if (cmd == OutsideCommand.OutsideRes) {
            final OutsideResPayload payload = (OutsideResPayload) in.getPayload();
            Assert.notNull(socksVersion, "SocksVersion版本数据不存在!");
            //socks4
            if (socksVersion == SocksVersion.SOCKS4a) {
                return socks4Handler(ctx, payload);
            }
            //socks5
            if (socksVersion == SocksVersion.SOCKS5) {
                return socks5Handler(ctx, payload);
            }
            //暂不支持socks版本
            log.warn("暂不支持Socks版本: {}", socksVersion);
            return null;
        }
        log.warn("支持该消息处理[command: {}]=> {}", in.getCommand(), in);
        return null;
    }

    @Override
    protected OutsideMessage<?> decode(@Nonnull final ChannelHandlerContext ctx, @Nonnull final Object in) {
        if (in instanceof SocksMessage) {
            final SocksMessage sm = (SocksMessage) in;
            this.socksVersion = sm.version();
            //socks4
            OutsideReqPayload reqPayload;
            if (this.socksVersion == SocksVersion.SOCKS4a) {
                reqPayload = socks4Handler(sm);
            } else if (this.socksVersion == SocksVersion.SOCKS5) {
                //socks5
                reqPayload = socks5Handler(ctx, sm);
            } else {
                //暂不支持socks版本
                throw new RuntimeException("暂不支持Socks版本:" + sm.version());
            }
            if (Objects.nonNull(reqPayload)) {
                final OutsideMessage<?> message = OutsideMessage.createOutsideReq(reqPayload);
                //设备ID
                message.setDeviceId(OutsideDeviceUtils.createDeviceId(ctx));
                //连接标识
                message.setTag(tag = buildTag(ctx, reqPayload));
                //返回消息
                return message;
            }
            return null;
        }
        if (in instanceof ByteBuf) {
            Assert.hasText(tag, "'tag'不能为空");
            final OutsideMessage<?> message = OutsideMessage.createOutsideData(((ByteBuf) in).retainedDuplicate());
            //设备ID
            message.setDeviceId(OutsideDeviceUtils.createDeviceId(ctx));
            //连接标识
            message.setTag(tag);
            //消息对象
            return message;
        }
        log.warn("非Socks消息,解析失败!");
        return null;
    }

    private Object socks4Handler(@Nonnull final ChannelHandlerContext ctx, @Nonnull final OutsideResPayload payload) {
        //连接成功
        if (payload.isRet()) {
            Socks4CommandResponse cmdRes;
            if (this.socks4CommandType == Socks4CommandType.BIND) {
                final InetSocketAddress socketAddr = NettyUtils.getLocalAddr(ctx.channel());
                final String ip = NettyUtils.getIpAddr(socketAddr);
                final Integer port = NettyUtils.getIpPort(socketAddr);
                cmdRes = new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS, ip, port);
            } else {
                cmdRes = new DefaultSocks4CommandResponse(Socks4CommandStatus.SUCCESS);
            }
            //发送响应消息
            NettyUtils.writeAndFlush(ctx, cmdRes, future -> {
                if (future.isSuccess()) {
                    final Channel ch = future.channel();
                    final ChannelPipeline p = ch.pipeline();
                    //移除socks编解码器
                    p.remove(Socks4ServerDecoder.class);
                    p.remove(Socks4ServerEncoder.class);
                    //socks4协议处理完毕,读取业务数据
                    if (cmdRes.status() == Socks4CommandStatus.SUCCESS) {
                        ch.read();
                    }
                }
            });
            return null;
        }
        //连接失败处理
        return new DefaultSocks4CommandResponse(Socks4CommandStatus.REJECTED_OR_FAILED);
    }

    private Object socks5Handler(@Nonnull final ChannelHandlerContext ctx, @Nonnull final OutsideResPayload payload) {
        //连接成功
        if (payload.isRet()) {
            Socks5CommandResponse cmdRes;
            if (this.socks5CommandType == Socks5CommandType.BIND) {
                final InetSocketAddress socketAddr = NettyUtils.getLocalAddr(ctx.channel());
                final String ip = NettyUtils.getIpAddr(socketAddr);
                final Integer port = NettyUtils.getIpPort(socketAddr);
                cmdRes = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.DOMAIN, ip, port);
            } else {
                cmdRes = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.DOMAIN);
            }
            NettyUtils.writeAndFlush(ctx, cmdRes, future -> {
                if (future.isSuccess()) {
                    final Channel ch = future.channel();
                    final ChannelPipeline p = ch.pipeline();
                    //移除socks编解码器
                    p.remove(Socks5CommandRequestDecoder.class);
                    p.remove(Socks5ServerEncoder.class);
                    //socks5协议处理完毕,读取业务数据
                    if (cmdRes.status() == Socks5CommandStatus.SUCCESS) {
                        ch.read();
                    }
                }
            });
            return null;
        }
        //连接失败
        return new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.DOMAIN);
    }

    private OutsideReqPayload parseSocksReq(@Nonnull final Consumer<OutsideReqPayload> handler) {
        //创建请求报文体
        final OutsideReqPayload payload = new OutsideReqPayload();
        //数据处理
        handler.accept(payload);
        //返回
        return payload;
    }

    private OutsideReqPayload socks4Handler(@Nonnull final SocksMessage sm) {
        //请求数据
        if (sm instanceof Socks4CommandRequest) {
            final Socks4CommandRequest cmdReq = (Socks4CommandRequest) sm;
            this.socks4CommandType = cmdReq.type();
            //创建请求
            return parseSocksReq(req -> {
                req.setHost(cmdReq.dstAddr());
                req.setPort(cmdReq.dstPort());
            });
        }
        return null;
    }

    private OutsideReqPayload socks5Handler(@Nonnull final ChannelHandlerContext ctx, @Nonnull final SocksMessage sm) {
        //Socks5 init
        if (sm instanceof Socks5InitialRequest) {
            final Socks5InitialResponse initRes = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
            NettyUtils.writeAndFlush(ctx, initRes, future -> {
                if (future.isSuccess()) {
                    final ChannelPipeline p = future.channel().pipeline();
                    p.addFirst("socks5-req-cmd", new Socks5CommandRequestDecoder());
                    p.remove(Socks5InitialRequestDecoder.class);
                    //读取数据
                    future.channel().read();
                }
            });
            return null;
        }
        //socks5 cmd req
        if (sm instanceof Socks5CommandRequest) {
            final Socks5CommandRequest cmdReq = (Socks5CommandRequest) sm;
            this.socks5CommandType = cmdReq.type();
            //创建请求
            return parseSocksReq(req -> {
                req.setHost(cmdReq.dstAddr());
                req.setPort(cmdReq.dstPort());
            });
        }
        return null;
    }
}
