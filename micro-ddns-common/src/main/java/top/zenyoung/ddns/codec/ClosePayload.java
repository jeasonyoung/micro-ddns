package top.zenyoung.ddns.codec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 关闭-报文体
 *
 * @author young
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class ClosePayload implements Serializable {
    /**
     * 时间戳
     */
    private Long stamp;
    /**
     * 是否重连
     */
    private boolean reconnect;
}
