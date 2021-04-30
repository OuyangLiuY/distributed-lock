package com.only.redis.lock;

import com.only.base.AbstractLock;
import org.redisson.RedissonRedLock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class RedisRedLock extends AbstractLock {
    private final RedissonRedLock redLock;

    public RedisRedLock(RedissonRedLock redLock) {
        this.redLock = redLock;
    }

    @Override
    public void lock() {
        redLock.lock();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        redLock.lockInterruptibly();
    }

    @Override
    public boolean tryLock() {
        return redLock.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return redLock.tryLock(time,unit);
    }

    @Override
    public void unlock() {
        redLock.unlock();
    }

    @Override
    public Condition newCondition() {
        return redLock.newCondition();
    }
}
