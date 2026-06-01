package be.ucll.it.courses.backend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration for NoSQL caching.
 *
 * We use Redis as a key-value store to cache frequently accessed data like
 * the latest telemetry readings and robot status. This avoids hitting
 * PostgreSQL on every dashboard refresh.
 *
 * Why Redis over other NoSQL options:
 * - Our cache data is simple key-value pairs (device ID → latest telemetry)
 * - Redis is in-memory, giving sub-millisecond reads vs ~5ms for PostgreSQL
 * - Spring Boot has built-in Redis support with minimal configuration
 * - Redis TTL (time-to-live) automatically expires stale cache entries
 *
 * This config only activates when app.redis.enabled=true.
 * In tests, Redis is replaced by an in-memory mock.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "app.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
}
