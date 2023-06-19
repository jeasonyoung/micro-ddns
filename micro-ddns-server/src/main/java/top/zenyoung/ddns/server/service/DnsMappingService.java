package top.zenyoung.ddns.server.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import top.zenyoung.ddns.common.HostPort;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * DNS映射-服务接口
 *
 * @author young
 */
public interface DnsMappingService {

    /**
     * 外部访问映射内部
     *
     * @param outside 外部访问
     * @return 映射内部
     */
    @Nullable
    DnsMappingInside mappingInside(@Nonnull final HostPort outside);

    /**
     * 映射内部
     */
    @Getter
    @RequiredArgsConstructor(staticName = "of")
    class DnsMappingInside implements HostPort {
        /**
         * 映射SN(客户端标识)
         */
        private final String sn;
        /**
         * 映射内部地址
         */
        private final String host;
        /**
         * 映射内部端口
         */
        private final Integer port;

        public static DnsMappingInside of(@Nonnull final String sn, @Nonnull final HostPort hostPort) {
            return DnsMappingInside.of(sn, hostPort.getHost(), hostPort.getPort());
        }
    }
}
