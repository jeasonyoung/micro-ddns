package top.zenyoung.ddns.codec;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 注册响应-报文体
 *
 * @author young
 */
@Data
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public class RegResPayload implements Serializable {
    /**
     * 注册结果
     */
    private boolean ret;
    /**
     * 注册消息
     */
    private String msg;
}
