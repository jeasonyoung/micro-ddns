package top.zenyoung.ddns.util;

import com.google.common.base.Joiner;
import org.springframework.util.Assert;
import top.zenyoung.ddns.common.DeviceType;

import javax.annotation.Nonnull;

/**
 * 设备工具类
 *
 * @author young
 */
public class DeviceUtils {
    /**
     * 创建设备ID
     *
     * @param type 设备类型
     * @return 设备ID
     */
    public static String createDeviceId(@Nonnull final DeviceType type, @Nonnull final String... params) {
        return type.getVal() + Joiner.on("").join(params);
    }

    public static DeviceType parseDeviceType(@Nonnull final String deviceId) {
        Assert.hasText(deviceId, "'deviceId'不能为空");
        final String typeVal = deviceId.substring(0, 1);
        return DeviceType.parse(typeVal);
    }
}
