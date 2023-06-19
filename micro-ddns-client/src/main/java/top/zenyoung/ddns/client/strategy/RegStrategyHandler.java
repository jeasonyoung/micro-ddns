package top.zenyoung.ddns.client.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.zenyoung.ddns.codec.Command;
import top.zenyoung.ddns.codec.InsideMessage;
import top.zenyoung.ddns.codec.RegResPayload;
import top.zenyoung.netty.handler.BaseStrategyHandler;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * 内部(客户端)-注册响应-策略处理器
 *
 * @author young
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegStrategyHandler extends BaseStrategyHandler<InsideMessage<?>> {

    @Nonnull
    @Override
    public String[] getCommands() {
        return new String[]{Command.RegRes.name()};
    }

    @Override
    public InsideMessage<?> process(@Nonnull final Session session, @Nonnull final InsideMessage<?> req) {
        //解析数据
        Optional.ofNullable((RegResPayload) req.getPayload())
                .ifPresent(body -> {
                    //检查注册结果
                    final boolean ret = body.isRet();
                    log.info("向【代理服务器】进行注册-{} => {}", (ret ? "成功" : "失败"), body);
                    if (ret) {
                        session.readChannelData();
                        return;
                    }
                    //注册失败,关闭连接
                    log.error("向【代理服务器】注册失败,将关闭连接=> {}", body.getMsg());
                    session.close();
                });
        //返回
        return null;
    }
}
