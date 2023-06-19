package top.zenyoung.ddns.common;

import java.io.Serializable;

/**
 * 地址端口接口
 *
 * @author young
 */
public interface HostPort extends Serializable {
    /**
     * 默认端口
     */
    Integer DEF_PORT = 80;

    /**
     * 获取目标地址
     *
     * @return 目标地址
     */
    String getHost();

    /**
     * 获取目标端口
     *
     * @return 目标端口
     */
    Integer getPort();
}
