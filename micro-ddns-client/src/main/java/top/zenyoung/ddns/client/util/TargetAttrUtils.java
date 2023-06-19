package top.zenyoung.ddns.client.util;

import com.google.common.collect.Maps;
import io.netty.util.AttributeKey;
import org.springframework.util.Assert;
import top.zenyoung.ddns.client.work.TargetClient;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * AttributeKey 工具类
 *
 * @author young
 */
public class TargetAttrUtils {
    private static final Map<String, Object> LOCKS = Maps.newHashMap();
    private static final String PREFIX = "tag_";

    private static AttributeKey<TargetClient> getKey(@Nonnull final String tag) {
        Assert.hasText(tag, "'tag'不能为空");
        return AttributeKey.valueOf(PREFIX + tag);
    }

    public static TargetClient putIfAbsent(@Nonnull final Session session, @Nonnull final String tag,
                                           @Nonnull final Supplier<TargetClient> createHandler) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                final AttributeKey<TargetClient> attrKey = getKey(tag);
                if (session.hasAttr(attrKey)) {
                    final TargetClient client = session.attr(attrKey).get();
                    if (Objects.nonNull(client)) {
                        return client;
                    }
                }
                final TargetClient client = createHandler.get();
                Assert.notNull(client, "'createHandler'创建TargetClient不能为空");
                session.attr(attrKey).set(client);
                return client;
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    public static TargetClient getAttrByTag(@Nonnull final Session session, @Nonnull final String tag) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                final AttributeKey<TargetClient> attrKey = getKey(tag);
                return session.attr(attrKey).get();
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    public static void cleanAttr(@Nonnull final Session session, @Nonnull final String tag) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                final AttributeKey<TargetClient> attrKey = getKey(tag);
                //检查是否存在
                if (session.hasAttr(attrKey)) {
                    //移除设置
                    session.attr(attrKey).set(null);
                }
            } finally {
                LOCKS.remove(tag);
            }
        }
    }
}
