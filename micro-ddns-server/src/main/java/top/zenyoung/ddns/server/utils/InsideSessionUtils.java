package top.zenyoung.ddns.server.utils;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 内部会话工具类
 *
 * @author young
 */
@Slf4j
public class InsideSessionUtils {
    public static final String SN_ANY_PATTERN = "*";
    private static final Map<String, Object> LOCKS = Maps.newHashMap();
    private static final Map<String, Session> SN_INSIDE = Maps.newConcurrentMap();
    private static final Map<String, Session> TAG_INSIDE = Maps.newConcurrentMap();

    private static final AtomicLong REF_LAST_TRAVERSAL_STAMP = new AtomicLong(0L);
    private static final AtomicBoolean IS_TRAVERSAL_RUN = new AtomicBoolean(false);
    private static final Duration TRAVERSAL_INTERVAL = Duration.ofSeconds(90);

    public static void putSn(@Nonnull final String sn, @Nonnull final Session inside) {
        Assert.hasText(sn, "'SN'不能为空");
        synchronized (LOCKS.computeIfAbsent(sn, k -> new Object())) {
            try {
                SN_INSIDE.put(sn, inside);
            } finally {
                LOCKS.remove(sn);
            }
        }
    }

    public static boolean checkSn(@Nonnull final String sn) {
        Assert.hasText(sn, "'SN'不能为空");
        return SN_INSIDE.containsKey(sn);
    }

    public static Session getBySn(@Nonnull final String sn) {
        Assert.hasText(sn, "'SN'不能为空");
        synchronized (LOCKS.computeIfAbsent(sn, k -> new Object())) {
            try {
                if (SN_ANY_PATTERN.equalsIgnoreCase(sn)) {
                    return SN_INSIDE.values().stream()
                            .findAny()
                            .orElse(null);
                }
                return SN_INSIDE.getOrDefault(sn, null);
            } finally {
                LOCKS.remove(sn);
            }
        }
    }

    public static void putTag(@Nonnull final String tag, @Nonnull final Session inside) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                TAG_INSIDE.put(tag, inside);
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    public static Session getByTag(@Nonnull final String tag) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                return TAG_INSIDE.getOrDefault(tag, null);
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    public static void checkTraversal() {
        final long start = System.currentTimeMillis(), last = REF_LAST_TRAVERSAL_STAMP.get(), interval = TRAVERSAL_INTERVAL.toMillis();
        if (last == 0) {
            REF_LAST_TRAVERSAL_STAMP.set(start);
            return;
        }
        if (start - last <= interval || IS_TRAVERSAL_RUN.get()) {
            return;
        }
        IS_TRAVERSAL_RUN.set(true);
        try {
            //sn
            if (!CollectionUtils.isEmpty(SN_INSIDE)) {
                SN_INSIDE.entrySet().removeIf(entry -> {
                    final Session inside = entry.getValue();
                    return Objects.nonNull(inside) && !inside.isActive();
                });
            }
            //tag
            if (!CollectionUtils.isEmpty(TAG_INSIDE)) {
                TAG_INSIDE.entrySet().removeIf(entry -> {
                    final Session inside = entry.getValue();
                    return Objects.nonNull(inside) && !inside.isActive();
                });
            }
        } finally {
            final long now = System.currentTimeMillis();
            REF_LAST_TRAVERSAL_STAMP.set(now);
            IS_TRAVERSAL_RUN.set(false);
            log.info("执行清理无效会话=> 消耗 {}ms", (now - start));
        }
    }
}
