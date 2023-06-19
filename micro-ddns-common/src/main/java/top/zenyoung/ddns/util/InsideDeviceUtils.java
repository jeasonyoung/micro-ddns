package top.zenyoung.ddns.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import top.zenyoung.ddns.common.DeviceType;
import top.zenyoung.netty.util.NettyUtils;

import javax.annotation.Nonnull;

/**
 * 内部设备工具类
 *
 * @author young
 */
public class InsideDeviceUtils {
    private static String createDeviceId(@Nonnull final InsideType type, @Nonnull final String channelId) {
        Assert.hasText(channelId, "'channelId'不能为空");
        return DeviceUtils.createDeviceId(DeviceType.Inside, type.val, channelId);
    }

    public static String createServerDeviceId(@Nonnull final String channelId) {
        Assert.hasText(channelId, "'channelId'不能为空");
        return createDeviceId(InsideType.Server, channelId);
    }

    public static String createClientDeviceId(@Nonnull final String channelId) {
        Assert.hasText(channelId, "'channelId'不能为空");
        return createDeviceId(InsideType.Client, channelId);
    }

    public static String createServerDeviceId(@Nonnull final Channel channel) {
        final String key = NettyUtils.getChannelId(channel);
        return createServerDeviceId(key);
    }

    public static String createClientDeviceId(@Nonnull final Channel channel) {
        final String key = NettyUtils.getChannelId(channel);
        return createClientDeviceId(key);
    }

    public static String createServerDeviceId(@Nonnull final ChannelHandlerContext ctx) {
        return createServerDeviceId(ctx.channel());
    }

    public static String createClientDeviceId(@Nonnull final ChannelHandlerContext ctx) {
        return createClientDeviceId(ctx.channel());
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    private enum InsideType {
        Server("S"),
        Client("C");
        private final String val;
    }
}
