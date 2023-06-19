package top.zenyoung.ddns.server.codec;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.util.Assert;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.netty.codec.Message;

import javax.annotation.Nonnull;

/**
 * 外部访问-消息
 *
 * @param <T> 报文消息体类型
 * @author young
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutsideMessage<T> extends InsideMessage<T> implements Message {

    public OutsideCommand getOutsideCommand() {
        final String command;
        Assert.hasText(command = getCommand(), "'command'未赋值");
        return Enum.valueOf(OutsideCommand.class, command);
    }

    private static <K> OutsideMessage<K> create(@Nonnull final OutsideCommand command, @Nonnull final K payload) {
        final OutsideMessage<K> message = new OutsideMessage<>();
        //协议命令
        message.setCommand(command.name());
        //报文数据
        message.setPayload(payload);
        //返回
        return message;
    }

    /**
     * 创建-外部访问请求
     *
     * @param payload 外部访问请求数据
     * @return 外部访问消息
     */
    public static OutsideMessage<OutsideReqPayload> createOutsideReq(@Nonnull final OutsideReqPayload payload) {
        return create(OutsideCommand.OutsideReq, payload);
    }

    /**
     * 创建-外部访问响应
     *
     * @param payload 外部访问响应数据
     * @return 外部访问消息
     */
    public static OutsideMessage<OutsideResPayload> createOutsideRes(@Nonnull final OutsideResPayload payload) {
        return create(OutsideCommand.OutsideRes, payload);
    }

    /**
     * 创建-外部请求数据
     *
     * @param payload 请求数据
     * @return 外部访问消息
     */
    public static OutsideMessage<ByteBuf> createOutsideData(@Nonnull final ByteBuf payload) {
        return create(OutsideCommand.OutsideData, payload);
    }
}
