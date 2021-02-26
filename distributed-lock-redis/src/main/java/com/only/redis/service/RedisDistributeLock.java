package com.only.redis.service;

import com.only.base.DistributedLock;

import java.util.concurrent.TimeUnit;

public interface RedisDistributeLock extends DistributedLock {

    boolean tryReLock(String key, long timeout , TimeUnit unit);

    void releaseReLock(String key);

}
