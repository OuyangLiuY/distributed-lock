package com.only.base;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public abstract class AbstractLock implements Lock {
    @Override
    public void lock() {
        throw new RuntimeException("不支持的操作");
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new RuntimeException("不支持的操作");
    }

    @Override
    public boolean tryLock() {
        throw new RuntimeException("不支持的操作");
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new RuntimeException("不支持的操作");
    }

    @Override
    public void unlock() {
        throw new RuntimeException("不支持的操作");
    }

    @Override
    public Condition newCondition() {
        throw new RuntimeException("不支持的操作");
    }
}
