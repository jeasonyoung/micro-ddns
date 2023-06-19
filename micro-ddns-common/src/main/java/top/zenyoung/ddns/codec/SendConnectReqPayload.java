package top.zenyoung.ddns.codec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import top.zenyoung.ddns.common.HostPort;

import javax.annotation.Nonnull;
import java.io.Serializable;

/**
 * 连接请求-报文体
 *
 * @author young
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class SendConnectReqPayload implements HostPort, Serializable {
    /**
     * 主机地址
     */
    private String host;
    /**
     * 主机端口
     */
    private Integer port;

    public static SendConnectReqPayload of(@Nonnull final HostPort hostPort) {
        return SendConnectReqPayload.of(hostPort.getHost(), hostPort.getPort());
    }
}
