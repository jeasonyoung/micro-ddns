package top.zenyoung.ddns.client.work;

import io.netty.buffer.ByteBuf;
import top.zenyoung.ddns.common.HostPort;

import javax.annotation.Nonnull;

/**
 * 目标客户端接口
 *
 * @author young
 */
public interface TargetClient {

    /**
     * 连接目标客户端
     *
     * @param target 目标地址
     */
    void connect(@Nonnull final HostPort target);

    /**
     * 发送数据到客户端
     *
     * @param data 数据
     */
    void sendData(@Nonnull final ByteBuf data);

    /**
     * 关闭目标客户端
     */
    void close();
}
