package top.zenyoung.ddns.codec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 心跳响应-报文体
 *
 * @author young
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class PongPayload implements Serializable {
    /**
     * 请求时间戳
     */
    private Long before;
    /**
     * 响应时间戳
     */
    private Long stamp;
}
