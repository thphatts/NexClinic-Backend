package com.thphatts.clinicportal.config.ai;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableCaching
@EnableAsync
@EnableScheduling
public class AiCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // aiClinicContext: Cache RAG context cũ (giữ compat)
        // clinicKnowledgeEmbeddings: Cache kết quả embedding vector (tránh gọi lại Gemini API)
        return new ConcurrentMapCacheManager("aiClinicContext", "doctors", "products", "clinicKnowledgeEmbeddings");
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
