package be.ucll.it.courses.backend.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for the latest telemetry data per device.
 *
 * When Redis is available, data is stored in Redis (NoSQL key-value store)
 * for sub-millisecond reads. When Redis is not available (tests, local dev
 * without Redis), falls back to an in-memory ConcurrentHashMap.
 *
 * Data flow:
 * 1. ESP32 sends telemetry → backend saves to PostgreSQL AND updates cache
 * 2. Dashboard requests status → backend reads from cache first (fast)
 * 3. Cache entries expire after 30 seconds to prevent stale data
 */
@Service
public class TelemetryCacheService {

    private static final String CACHE_PREFIX = "telemetry:latest:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(30);

    @Nullable
    private final RedisTemplate<String, Object> redisTemplate;

    // In-memory fallback when Redis is not available
    private final Map<String, Map<String, Object>> memoryCache = new ConcurrentHashMap<>();

    public TelemetryCacheService(@Nullable RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Cache the latest telemetry for a device.
     */
    public void cacheLatestTelemetry(String deviceId, Map<String, Object> telemetryData) {
        if (redisTemplate != null) {
            try {
                String key = CACHE_PREFIX + deviceId;
                redisTemplate.opsForHash().putAll(key, telemetryData);
                redisTemplate.expire(key, CACHE_TTL);
                return;
            } catch (Exception e) {
                // Redis down — fall through to memory cache
            }
        }
        memoryCache.put(CACHE_PREFIX + deviceId, new ConcurrentHashMap<>(telemetryData));
    }

    /**
     * Get cached telemetry for a device.
     * Returns null if not in cache.
     */
    public Map<Object, Object> getCachedTelemetry(String deviceId) {
        if (redisTemplate != null) {
            try {
                String key = CACHE_PREFIX + deviceId;
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                if (!data.isEmpty()) return data;
            } catch (Exception e) {
                // Redis down — fall through to memory cache
            }
        }
        Map<String, Object> data = memoryCache.get(CACHE_PREFIX + deviceId);
        return data != null ? new java.util.HashMap<>(data) : null;
    }

    /**
     * Cache the robot's current status.
     */
    public void cacheRobotStatus(Map<String, Object> status) {
        if (redisTemplate != null) {
            try {
                String key = "robot:status:latest";
                redisTemplate.opsForHash().putAll(key, status);
                redisTemplate.expire(key, CACHE_TTL);
                return;
            } catch (Exception e) {
                // Redis down — fall through to memory cache
            }
        }
        memoryCache.put("robot:status:latest", new ConcurrentHashMap<>(status));
    }

    /**
     * Get cached robot status.
     * Returns null if not in cache.
     */
    public Map<Object, Object> getCachedRobotStatus() {
        if (redisTemplate != null) {
            try {
                String key = "robot:status:latest";
                Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
                if (!data.isEmpty()) return data;
            } catch (Exception e) {
                // Redis down — fall through to memory cache
            }
        }
        Map<String, Object> data = memoryCache.get("robot:status:latest");
        return data != null ? new java.util.HashMap<>(data) : null;
    }
}
