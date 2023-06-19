package top.zenyoung.ddns.client.service.impl;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Service;
import top.zenyoung.ddns.client.config.ClientProperties;
import top.zenyoung.ddns.client.service.DeviceInitService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

/**
 * 设备初始化-服务接口实现
 *
 * @author young
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceInitServiceImpl implements DeviceInitService {
    private final ClientProperties properties;
    private static final String SN_DEF_CACHE_FILE_NAME = "ddns-client-sn";
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    @Override
    public void preHandler(@Nullable final ApplicationArguments args) {
        try {
            //检查设备号是否已配置
            if (!Strings.isNullOrEmpty(properties.getSn())) {
                return;
            }
            //缓存路径
            final String path = Optional.ofNullable(properties.getSnCachePath())
                    .filter(cp -> !Strings.isNullOrEmpty(cp))
                    .orElse(FileUtils.getTempDirectoryPath());
            //缓存文件名
            final String fileName = Optional.ofNullable(properties.getSnCacheFile())
                    .filter(cf -> !Strings.isNullOrEmpty(cf))
                    .orElse(SN_DEF_CACHE_FILE_NAME);
            //设置设备号
            properties.setSn(loadOrCreateSn(FileUtils.getFile(path, fileName)));
        } finally {
            log.info("加载或生成设备标识完成=> sn: {}", properties.getSn());
        }
    }

    private String loadOrCreateSn(@Nonnull final File snFilePath) {
        try {
            //缓存文件存在,则加载内容
            if (snFilePath.exists()) {
                final String content = FileUtils.readFileToString(snFilePath, UTF8);
                if (!Strings.isNullOrEmpty(content)) {
                    return content;
                }
            }
            //生成新的设备SN
            final String newSn = DigestUtils.sha1Hex(UUID.randomUUID().toString());
            //保存设备SN到缓存文件
            FileUtils.writeStringToFile(snFilePath, newSn, UTF8);
            //返回新的设备SN
            return newSn;
        } catch (Throwable e) {
            log.error("加载或生成设备SN异常(snFilePath: {})-exp: {}", snFilePath, e.getMessage());
        }
        return null;
    }
}