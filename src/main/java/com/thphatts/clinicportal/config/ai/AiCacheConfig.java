package com.thphatts.clinicportal.config.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
public class AiCacheConfig {

    /**
     * Serializer riêng cho cache value: dùng Jackson để lưu object dạng JSON trong Redis
     * thay vì Java native serialization (chậm hơn, không đọc được từ bên ngoài để debug).
     */
    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        ObjectMapper redisObjectMapper = new ObjectMapper();
        redisObjectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        redisObjectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues(); // KHÔNG cache null → tránh cache "not found" mãi mãi

        return builder -> builder
                .withCacheConfiguration("doctors", defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .withCacheConfiguration("aiClinicContext", defaultConfig.entryTtl(Duration.ofMinutes(30)))
                .withCacheConfiguration("clinicKnowledgeEmbeddings", defaultConfig.entryTtl(Duration.ofHours(6)));
    }

    @Bean(name = "aiAsyncExecutor")
    public Executor aiAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AI-Async-");
        executor.initialize();
        return executor;
    }
}