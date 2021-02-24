package com.only.base;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {
    /**
     * 尝试枷锁
     * @param key
     * @param timeout
     * @param unit
     * @return
     */
    boolean tryLock(String key, long timeout , TimeUnit unit);

    /**
     * 解锁
     * @param key
     */
    void releaseLock(String key);
}
