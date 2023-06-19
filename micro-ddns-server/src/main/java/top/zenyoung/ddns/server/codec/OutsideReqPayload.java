package top.zenyoung.ddns.server.codec;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.zenyoung.ddns.codec.SendConnectReqPayload;

/**
 * 外部访问-请求报文体
 *
 * @author young
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutsideReqPayload extends SendConnectReqPayload {
    /**
     * 数据报文
     */
    private ByteBuf body;
}
