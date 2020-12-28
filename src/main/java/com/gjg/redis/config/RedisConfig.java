package com.gjg.redis.config;

import io.lettuce.core.api.StatefulConnection;
import lombok.Setter;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * redis配置类
 *
 * @author gongjiguang
 * @date 2020/12/24
 */
@Setter
@Configuration
@ConfigurationProperties(prefix = "redis")
@EnableCaching
public class RedisConfig {

    private String host;
    private int port;
    private Pool pool;

    @Setter
    static class Pool {
        private int minIdle;
        private int maxIdle;
        private int maxTotal;
        private long maxWaitMillis;
    }

    /**
     * 创建redis连接工厂
     *
     * @param genericObjectPoolConfig redis连接池
     * @return redis连接工厂
     */
    @Bean
    @ConditionalOnBean(name = "poolConfig")
    public LettuceConnectionFactory redisConnectionFactory(GenericObjectPoolConfig<StatefulConnection<String, String>> genericObjectPoolConfig) {
        LettuceClientConfiguration clientConfiguration = LettucePoolingClientConfiguration.builder()
                .poolConfig(genericObjectPoolConfig).build();
        return new LettuceConnectionFactory(new RedisStandaloneConfiguration(host, port), clientConfiguration);
    }

    /**
     * redisTemplate的相关配置
     *
     * @param factory redis连接工厂
     * @return redisTemplate
     */
    @Bean
    @ConditionalOnBean(name = "redisConnectionFactory")
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.setDefaultSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    /**
     * 创建池配置
     *
     * @return 连接池
     */
    @Bean
    public GenericObjectPoolConfig<StatefulConnection<String, String>> poolConfig() {
        GenericObjectPoolConfig<StatefulConnection<String, String>> config = new GenericObjectPoolConfig<>();
        config.setMaxIdle(pool.maxIdle);
        config.setMinIdle(pool.minIdle);
        config.setMaxTotal(pool.maxTotal);
        config.setMaxWaitMillis(pool.maxWaitMillis);
        return config;
    }

    /**
     * 选择redis作为默认缓存工具
     *
     * @param factory redis连接工厂
     * @return redis缓存管理
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(factory);
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig();
        return new RedisCacheManager(redisCacheWriter, redisCacheConfiguration);
    }

}
