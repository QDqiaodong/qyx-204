package com.example.dormitory.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis SET NX PX 的简单互斥锁。
 *
 * 仅用于把同一台洗漱台的并发绑定/解绑请求在应用层提前串行化，
 * 数据库行锁（SELECT ... FOR UPDATE）才是最终正确性保障，
 * 因此 Redis 不可用时降级为不持锁，绝不静默放行错误的数据写入。
 */
@Component
public class RedisLock {

    private static final Logger log = LoggerFactory.getLogger(RedisLock.class);

    private static final long DEFAULT_EXPIRE_SECONDS = 30;
    private static final long DEFAULT_WAIT_MILLIS = 5000;
    private static final long RETRY_INTERVAL_MILLIS = 50;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试获取锁，获取不到时在 waitMillis 内重试。
     *
     * @return 锁令牌（释放时原样传回）；Redis 不可用时返回 null，表示未持锁、降级运行
     */
    public String tryLock(String key, long waitMillis) {
        String token = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitMillis;
        try {
            do {
                Boolean ok = redisTemplate.opsForValue()
                        .setIfAbsent(key, token, DEFAULT_EXPIRE_SECONDS, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(ok)) {
                    return token;
                }
                try {
                    Thread.sleep(RETRY_INTERVAL_MILLIS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } while (System.currentTimeMillis() < deadline);
        } catch (Exception e) {
            // Redis 故障不应阻断业务：数据库行锁仍保证正确性，这里降级为不持锁
            log.warn("获取Redis锁失败，降级为仅依赖数据库行锁，key={}", key, e);
            return null;
        }
        return null;
    }

    public String tryLock(String key) {
        return tryLock(key, DEFAULT_WAIT_MILLIS);
    }

    /**
     * 释放锁。只删除令牌匹配的键，避免误删别人的锁。token 为 null 表示当初未持锁，直接跳过。
     */
    public void unlock(String key, String token) {
        if (token == null) {
            return;
        }
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), token);
        } catch (Exception e) {
            // 锁设有过期时间，释放失败会自动过期，仅记录不抛出
            log.warn("释放Redis锁失败，等待自动过期，key={}", key, e);
        }
    }
}
