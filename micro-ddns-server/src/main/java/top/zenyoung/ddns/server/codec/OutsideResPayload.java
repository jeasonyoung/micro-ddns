package top.zenyoung.ddns.server.codec;

import top.zenyoung.ddns.codec.ReceiveConnectResPayload;

import javax.annotation.Nonnull;

/**
 * 外部访问-响应报文体
 *
 * @author young
 */
public class OutsideResPayload extends ReceiveConnectResPayload {

    public static OutsideResPayload of(@Nonnull final ReceiveConnectResPayload body) {
        final OutsideResPayload payload = new OutsideResPayload();
        //响应结果
        payload.setRet(body.isRet());
        //响应消息
        payload.setMsg(body.getMsg());
        //返回
        return payload;
    }
}
