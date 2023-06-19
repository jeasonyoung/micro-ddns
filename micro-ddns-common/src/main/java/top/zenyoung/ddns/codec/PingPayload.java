package top.zenyoung.ddns.codec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 心跳请求-报文体
 *
 * @author young
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PingPayload implements Serializable {
    /**
     * 客户端标识
     */
    private String sn;
    /**
     * 时间戳
     */
    private Long stamp;
}
