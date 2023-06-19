package top.zenyoung.ddns.client.config;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.zenyoung.netty.config.BaseProperties;

/**
 * DDNS 客户端配置
 *
 * @author young
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "client")
public class ClientProperties extends BaseProperties {
    /**
     * 设备标识
     */
    private String sn;
    /**
     * 设备标识缓存名
     */
    private String snCacheFile = "ddns-client-sn";
    /**
     * 设备标识缓存路径
     */
    private String snCachePath;
    /**
     * 重启标识
     */
    private boolean reconnect = true;
}
