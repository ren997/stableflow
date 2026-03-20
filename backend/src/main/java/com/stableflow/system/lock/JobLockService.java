package com.stableflow.system.lock;

import java.time.Duration;

public interface JobLockService {

    /** Try to acquire a job lock with a bounded TTL / 尝试获取一个带过期时间的任务锁 */
    boolean tryLock(String key, String value, Duration ttl);

    /** Release the lock only when the caller still owns it / 仅在调用方仍持有锁时释放任务锁 */
    void unlock(String key, String value);
}
