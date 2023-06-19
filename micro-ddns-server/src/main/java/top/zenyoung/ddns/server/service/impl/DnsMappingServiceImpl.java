package top.zenyoung.ddns.server.service.impl;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.PathMatcher;
import top.zenyoung.ddns.common.HostPort;
import top.zenyoung.ddns.server.config.DnsMapping;
import top.zenyoung.ddns.server.config.ServerProperties;
import top.zenyoung.ddns.server.service.DnsMappingService;
import top.zenyoung.ddns.server.utils.InsideSessionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * DNS映射-服务接口实现
 *
 * @author young
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DnsMappingServiceImpl implements DnsMappingService {
    private static final Map<String, Object> LOCKS = Maps.newHashMap();
    private static final String SN_ANY_PATTERN = InsideSessionUtils.SN_ANY_PATTERN;
    private final Cache<String, DnsMapping> dnsMappingCache = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(5))
            .maximumSize(1500)
            .build();

    private final ServerProperties properties;

    private String buildMappingKey(@Nonnull final HostPort host) {
        return Joiner.on(":").skipNulls().join(host.getHost(), host.getPort());
    }

    private DnsMapping getMapping(@Nonnull final HostPort outside) {
        final String key = buildMappingKey(outside);
        synchronized (LOCKS.computeIfAbsent(key, k -> new Object())) {
            try {
                DnsMapping ret = dnsMappingCache.getIfPresent(key);
                if (Objects.isNull(ret)) {
                    final List<DnsMapping> mappings = properties.getMappings();
                    if (!CollectionUtils.isEmpty(mappings)) {
                        ret = mappings.stream()
                                .filter(m -> {
                                    final String pattern = buildMappingKey(m.getSourceHostPort());
                                    final PathMatcher matcher = new AntPathMatcher();
                                    return !Strings.isNullOrEmpty(pattern) && matcher.match(pattern, key);
                                })
                                .findFirst()
                                .orElse(null);
                        if (Objects.nonNull(ret)) {
                            dnsMappingCache.put(key, ret);
                        }
                    }
                }
                return ret;
            } finally {
                LOCKS.remove(key);
            }
        }
    }

    @Nullable
    @Override
    public DnsMappingInside mappingInside(@Nonnull final HostPort outside) {
        final DnsMapping mapping = getMapping(outside);
        if (Objects.isNull(mapping)) {
            return null;
        }
        //目标设备标识
        final String sn = Optional.ofNullable(mapping.getSn())
                .filter(val -> !Strings.isNullOrEmpty(val))
                .orElse(SN_ANY_PATTERN);
        //映射后访问地址
        final HostPort target = Optional.ofNullable(mapping.getTargetHostPort()).orElse(outside);
        return DnsMappingInside.of(sn, target);
    }
}
