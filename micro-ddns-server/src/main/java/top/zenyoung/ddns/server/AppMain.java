package top.zenyoung.ddns.server;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 应用入口
 *
 * @author young
 */
@EnableAsync
@SpringBootApplication
public class AppMain {
    /**
     * 主函数
     *
     * @param args 参数数组
     */
    public static void main(final String[] args) {
        new SpringApplicationBuilder(AppMain.class)
                .web(WebApplicationType.NONE)
                .build()
                .run(args);
    }
}
