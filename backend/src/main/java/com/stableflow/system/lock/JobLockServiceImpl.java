package com.stableflow.system.lock;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JobLockServiceImpl implements JobLockService {

    private static final RedisScript<Long> RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>(
        "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
        Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean tryLock(String key, String value, Duration ttl) {
        Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(locked);
    }

    @Override
    public void unlock(String key, String value) {
        stringRedisTemplate.execute(RELEASE_LOCK_SCRIPT, List.of(key), value);
    }
}
