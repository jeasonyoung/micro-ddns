package top.zenyoung.ddns.codec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 连接响应-报文体
 *
 * @author young
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ReceiveConnectResPayload implements Serializable {
    /**
     * 连接结果
     */
    private boolean ret;
    /**
     * 连接结果信息
     */
    private String msg;
}
