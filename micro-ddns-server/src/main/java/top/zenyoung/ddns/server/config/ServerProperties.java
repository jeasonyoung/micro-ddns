package top.zenyoung.ddns.server.config;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.zenyoung.netty.config.BaseProperties;

import java.util.List;

/**
 * DDNS 服务器配置
 *
 * @author young
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties(prefix = "server")
public class ServerProperties extends BaseProperties {
    /**
     * DNS映射集合
     */
    private List<DnsMapping> mappings = Lists.newArrayList();
}
