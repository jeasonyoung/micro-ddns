package top.zenyoung.ddns.codec;

import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.springframework.beans.BeanUtils;

import javax.annotation.Nonnull;

/**
 * 内部协议消息-数据传输-内部序列化
 *
 * @author young
 */
public class InsideMessageDataInner extends InsideMessage<byte[]> {
    public static InsideMessageDataInner of(@Nonnull final InsideMessageData data) {
        final InsideMessageDataInner inner = new InsideMessageDataInner();
        BeanUtils.copyProperties(data, inner, "payload");
        inner.setPayload(ByteBufUtil.getBytes(data.getPayload()));
        return inner;
    }

    public InsideMessageData toData() {
        final InsideMessageData data = new InsideMessageData();
        BeanUtils.copyProperties(this, data, "payload");
        data.setPayload(Unpooled.wrappedBuffer(this.getPayload()));
        return data;
    }
}
