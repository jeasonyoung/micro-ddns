package top.zenyoung.ddns.codec;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import top.zenyoung.netty.codec.Message;

import javax.annotation.Nonnull;

/**
 * 内部协议消息
 *
 * @param <T> 消息报文体类型
 * @author young
 */
@Data
public class InsideMessage<T> implements Message {
    /**
     * 设备ID
     */
    private String deviceId;
    /**
     * 协议指令
     */
    private String command;
    /**
     * 连接标识
     */
    private String tag;
    /**
     * 报文数据
     */
    private T payload;

    /**
     * 创建-注册请求
     *
     * @param sn 客户端标识
     * @return 协议消息
     */
    public static InsideMessage<?> createRegReq(@Nonnull final String sn) {
        final InsideMessageRegReq message = new InsideMessageRegReq();
        //协议命令
        message.setCommand(Command.RegReq.name());
        //报文数据
        message.setPayload(sn);
        //返回
        return message;
    }

    /**
     * 创建-注册响应
     *
     * @param payload 注册响应数据
     * @return 协议消息
     */
    public static InsideMessage<?> createRegRes(@Nonnull final RegResPayload payload) {
        final InsideMessageRegRes message = new InsideMessageRegRes();
        //协议命令
        message.setCommand(Command.RegRes.name());
        //报文数据
        message.setPayload(payload);
        //返回
        return message;
    }

    /**
     * 创建-心跳请求
     *
     * @param payload 心跳请求数据
     * @return 协议消息
     */
    public static InsideMessage<?> createPing(@Nonnull final PingPayload payload) {
        final InsideMessagePing message = new InsideMessagePing();
        //协议命令
        message.setCommand(Command.Ping.name());
        //报文数据
        message.setPayload(payload);
        //返回
        return message;
    }

    /**
     * 创建-心跳响应
     *
     * @param payload 心跳响应数据
     * @return 协议消息
     */
    public static InsideMessage<?> createPong(@Nonnull final PongPayload payload) {
        final InsideMessagePong message = new InsideMessagePong();
        //协议命令
        message.setCommand(Command.Pong.name());
        //报文数据
        message.setPayload(payload);
        //返回
        return message;
    }

    /**
     * 创建-连接请求
     *
     * @param payload 连接请求数据
     * @return 协议消息
     */
    public static InsideMessage<?> createSendConnectReq(@Nonnull final SendConnectReqPayload payload) {
        final InsideMessageSendConnectReq message = new InsideMessageSendConnectReq();
        //协议命令
        message.setCommand(Command.SendConnectReq.name());
        //报文数据
        message.setPayload(payload);
        //返回
        return message;
    }

    /**
     * 创建-连接响应
     *
     * @param payload 连接响应数据
     * @return 协议消息
     */
    public static InsideMessage<?> createReceiveConnectRes(@Nonnull final ReceiveConnectResPayload payload) {
        final InsideMessageSendConnectRes message = new InsideMessageSendConnectRes();
        //协议命令
        message.setCommand(Command.ReceiveConnectRes.name());
        //报文数据
        message.setPayload(payload);
        //返回
        return message;
    }

    /**
     * 创建-数据传输
     *
     * @param payload 数据
     * @return 协议消息
     */
    public static InsideMessage<?> createData(@Nonnull final ByteBuf payload) {
        final InsideMessageData message = new InsideMessageData();
        //协议命令
        message.setCommand(Command.Data.name());
        //报文数据
        message.setPayload(payload);
        //返回
        return message;
    }

    /**
     * 创建-关闭消息
     *
     * @param payload 关闭数据
     * @return 协议消息
     */
    public static InsideMessage<?> createClose(@Nonnull final ClosePayload payload) {
        final InsideMessageClose message = new InsideMessageClose();
        //协议命令
        message.setCommand(Command.Close.name());
        //报文数据
        message.setPayload(payload);
        //返回
        return message;
    }
}
