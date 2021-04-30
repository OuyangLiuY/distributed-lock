package com.only.redis.lock;

import com.only.base.AbstractLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class RedisLock extends AbstractLock {

    private final RedissonClient client;
    private final String key;

    public RedisLock(RedissonClient client, String key) {
        this.client = client;
        this.key = key;
    }

    @Override
    public void lock() {
        client.getLock(key).lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        client.getLock(key).lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return client.getLock(key).tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return client.getLock(key).tryLock(time,unit);
    }

    @Override
    public void unlock() {
       client.getLock(key).unlock();
    }

    @Override
    public Condition newCondition() {
        return client.getLock(key).newCondition();
    }
}
