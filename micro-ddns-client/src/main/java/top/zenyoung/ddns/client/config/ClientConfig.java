package top.zenyoung.ddns.client.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * DDNS 客户端配置
 *
 * @author young
 */
@EnableAsync
@Configuration
@EnableConfigurationProperties({ClientProperties.class})
public class ClientConfig implements AsyncConfigurer {
    private static final int CORE_POOLS_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 1);

    @Override
    public Executor getAsyncExecutor() {
        //使用Spring内置线程池任务对象
        final ThreadPoolTaskExecutor taskScheduler = new ThreadPoolTaskExecutor();
        //设置线程参数
        taskScheduler.setBeanName("strategy-async");
        taskScheduler.setCorePoolSize(CORE_POOLS_SIZE);
        taskScheduler.setMaxPoolSize(CORE_POOLS_SIZE * 3);
        taskScheduler.setQueueCapacity(64);
        taskScheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        taskScheduler.initialize();
        //返回线程池
        return taskScheduler;
    }
}
