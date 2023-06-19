package top.zenyoung.ddns.server.config;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.zenyoung.ddns.common.HostPort;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * DDNS 映射
 *
 * @author young
 */
@Data
public class DnsMapping implements Serializable {
    private static final String SEP = ":", ALL = "*";
    /**
     * 主机地址(默认端口:80)
     */
    private String host;
    /**
     * 映射地址(不填则与主机地址一致)
     */
    private String target;
    /**
     * 映射客户端集合(SN标识)
     */
    private String sn;

    public HostPort getSourceHostPort() {
        return HostPortInner.of(host);
    }

    public HostPort getTargetHostPort() {
        if (!Strings.isNullOrEmpty(target) && target.contains(ALL)) {
            throw new IllegalArgumentException("映射地址必须确定,不能使用模糊匹配!" + target);
        }
        return HostPortInner.of(target);
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE, staticName = "of")
    private static class HostPortInner implements HostPort {
        private final String host;
        private final Integer port;

        public static HostPort of(@Nullable final String hostPort) {
            if (Strings.isNullOrEmpty(hostPort)) {
                return null;
            }
            final List<String> items = Splitter.on(SEP).omitEmptyStrings().trimResults().splitToList(hostPort);
            try {
                String h = items.get(0);
                final Integer p = items.size() > 1 ? Integer.parseInt(items.get(1)) : null;
                if (Objects.isNull(p) && !h.contains(ALL)) {
                    h += ALL;
                }
                return HostPortInner.of(h, p);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("端口号配置错误[" + hostPort + "]=>" + items.get(1));
            }
        }
    }
}
