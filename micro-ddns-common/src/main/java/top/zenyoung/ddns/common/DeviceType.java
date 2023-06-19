package top.zenyoung.ddns.common;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;

/**
 * 设备类型-枚举
 *
 * @author young
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum DeviceType {
    /**
     * DDNS 客户端
     */
    Inside("I"),
    /**
     * 访问设备
     */
    Outside("O");

    private final String val;

    public static DeviceType parse(@Nullable final String val) {
        if(!Strings.isNullOrEmpty(val)){
            for(final DeviceType t : DeviceType.values()){
                if(val.equalsIgnoreCase(t.getVal())){
                    return t;
                }
            }
        }
        return null;
    }
}
