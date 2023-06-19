package top.zenyoung.ddns.server.utils;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import top.zenyoung.netty.session.Session;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * 外部(访问)会话工具类
 *
 * @author young
 */
@Slf4j
public class OutsideSessionUtils {
    private static final Map<String, Object> LOCKS = Maps.newHashMap();
    private static final Map<String, Session> TAG_OUTSIDE = Maps.newConcurrentMap();
    private static final Map<String, Queue<ByteBuf>> TAG_DATA_CACHE = Maps.newConcurrentMap();

    private static final AtomicLong REF_LAST_TRAVERSAL_STAMP = new AtomicLong(0L);
    private static final AtomicBoolean IS_TRAVERSAL_RUN = new AtomicBoolean(false);
    private static final Duration TRAVERSAL_INTERVAL = Duration.ofSeconds(90);

    public static void putTag(@Nonnull final String tag, @Nonnull final Session outside) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                TAG_OUTSIDE.put(tag, outside);
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    public static Session getByTag(@Nonnull final String tag) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                return TAG_OUTSIDE.getOrDefault(tag, null);
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    public static void addCacheQueue(@Nonnull final String tag, @Nonnull final ByteBuf data) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                final Queue<ByteBuf> queue = TAG_DATA_CACHE.computeIfAbsent(tag, key -> Queues.newLinkedBlockingQueue());
                if (data.readableBytes() > 0) {
                    queue.offer(data);
                }
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    public static Queue<ByteBuf> getCacheQueue(@Nonnull final String tag) {
        Assert.hasText(tag, "'tag'不能为空");
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                return TAG_DATA_CACHE.computeIfAbsent(tag, key -> Queues.newLinkedBlockingQueue());
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    public static void cacheQueueHandler(@Nonnull final String tag, @Nonnull final Consumer<ByteBuf> handler) {
        Assert.hasText(tag, "'tag'不能为空");
        final Queue<ByteBuf> queue = getCacheQueue(tag);
        if (Objects.nonNull(queue)) {
            ByteBuf data;
            while ((data = queue.poll()) != null) {
                handler.accept(data);
            }
        }
    }

    public static void clean(@Nonnull final String tag) {
        Assert.hasText(tag, "'tag'不能为空");
        log.info("清理[outside]=>tag: {}", tag);
        synchronized (LOCKS.computeIfAbsent(tag, k -> new Object())) {
            try {
                //删除tag与Session的关联
                if (!CollectionUtils.isEmpty(TAG_OUTSIDE)) {
                    final Session outside = TAG_OUTSIDE.getOrDefault(tag, null);
                    if (Objects.isNull(outside) || !outside.isActive()) {
                        //删除外部会话
                        TAG_OUTSIDE.remove(tag);
                        //删除缓存队列
                        cleanDataCache(tag);
                    }
                }
            } finally {
                LOCKS.remove(tag);
            }
        }
    }

    private static void cleanDataCache(@Nonnull final String tag) {
        Assert.hasText(tag, "'tag'不能为空");
        if (!CollectionUtils.isEmpty(TAG_DATA_CACHE)) {
            //删除队列
            final Queue<ByteBuf> queue = TAG_DATA_CACHE.remove(tag);
            if (Objects.nonNull(queue) && queue.size() > 0) {
                queue.clear();
            }
        }
    }

    public static synchronized void checkTraversal() {
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
            //检查连接标签
            if (!CollectionUtils.isEmpty(TAG_OUTSIDE)) {
                TAG_OUTSIDE.entrySet().removeIf(entry -> {
                    final Session outside = entry.getValue();
                    if (Objects.isNull(outside) || !outside.isActive()) {
                        cleanDataCache(entry.getKey());
                        return true;
                    }
                    return false;
                });
            }
            //检查数据缓存
            if (!CollectionUtils.isEmpty(TAG_DATA_CACHE)) {
                TAG_DATA_CACHE.entrySet().removeIf(entry -> {
                    final Queue<?> queue = entry.getValue();
                    return Objects.isNull(queue) || queue.size() == 0;
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
