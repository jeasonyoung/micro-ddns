package top.zenyoung.ddns.codec;

import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Objects;

/**
 * 协议指令-枚举
 *
 * @author young
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Command implements Serializable {
    /**
     * 注册请求
     */
    RegReq(1, InsideMessageRegReq.class),
    /**
     * 注册响应
     */
    RegRes(2, InsideMessageRegRes.class),
    /**
     * 心跳请求
     */
    Ping(3, InsideMessagePing.class),
    /**
     * 心跳响应
     */
    Pong(4, InsideMessagePong.class),
    /**
     * 连接请求
     */
    SendConnectReq(5, InsideMessageSendConnectReq.class),
    /**
     * 连接响应
     */
    ReceiveConnectRes(6, InsideMessageSendConnectRes.class),
    /**
     * 业务数据
     */
    Data(7, InsideMessageData.class),
    /**
     * 关闭
     */
    Close(8, InsideMessageClose.class);

    private final int val;
    private final Class<? extends InsideMessage<?>> cls;

    public static Command parseByVal(@Nullable final Integer val) {
        if (Objects.nonNull(val) && val > 0) {
            for (final Command cmd : Command.values()) {
                if (val == cmd.val) {
                    return cmd;
                }
            }
        }
        return null;
    }

    public static Command parseByName(@Nullable final String name) {
        if (!Strings.isNullOrEmpty(name)) {
            return Enum.valueOf(Command.class, name);
        }
        return null;
    }
}
