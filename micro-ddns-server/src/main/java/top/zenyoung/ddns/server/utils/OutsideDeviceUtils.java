package top.zenyoung.ddns.server.utils;

import io.netty.channel.ChannelHandlerContext;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.ddns.util.DeviceUtils;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;

/**
 * 外部设备工具类
 *
 * @author young
 */
public class OutsideDeviceUtils {

    /**
     * 创建设备ID
     *
     * @param ctx 通道上下文
     * @return 设备ID
     */
    public static String createDeviceId(@Nonnull final ChannelHandlerContext ctx) {
        final String key = NettyUtils.getChannelId(ctx);
        return DeviceUtils.createDeviceId(DeviceType.Outside, key);
    }
}
