package top.zenyoung.ddns.client.service.impl;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import top.zenyoung.ddns.client.config.ClientProperties;
import top.zenyoung.ddns.client.service.ConnectedService;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.util.InsideDeviceUtils;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;

/**
 * 连接服务器成功-服务接口实现
 *
 * @author young
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectedServiceImpl implements ConnectedService {
    private final ClientProperties properties;

    @Override
    public void handler(@Nonnull final Channel channel) {
        //获取设备标识
        final String sn = properties.getSn();
        Assert.hasText(sn, "获取设备SN失败!");
        //创建注册请求
        final InsideMessage<?> message = InsideMessage.createRegReq(sn);
        message.setDeviceId(InsideDeviceUtils.createClientDeviceId(channel));
        //发送注册请求
        NettyUtils.writeAndFlush(channel, message, future -> {
            final boolean ret = future.isSuccess();
            log.info("向【代理服务器】发送注册请求-[{}] => {}", ret, message);
            if (ret) {
                //发送成功,则准备读取反馈
                future.channel().read();
            }
        });
    }
}
