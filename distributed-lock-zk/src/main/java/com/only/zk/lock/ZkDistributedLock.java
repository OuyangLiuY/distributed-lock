package com.only.zk.lock;

import com.only.base.AbstractLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.RetryNTimes;

import java.util.concurrent.TimeUnit;

/**
 * 支持可重入的排它锁
 */
@Slf4j
public class ZkDistributedLock extends AbstractLock {

    /**
     * 1.Connect to zk
     */
    private CuratorFramework client;

    private InterProcessLock lock;

    public ZkDistributedLock(String addr, String lockPath) {
        //1. connect
        client = CuratorFrameworkFactory.newClient(addr, new RetryNTimes(5, 5000));
        client.start();
        if (client.getState() == CuratorFrameworkState.STARTED) {
            log.info("zk client start successfully!");
            log.info("zkAddress:{},lockPath:{}", addr, lockPath);
        } else {
            throw new RuntimeException("客户端启动失败。。。");
        }
        this.lock = new InterProcessMutex(client, lockPath);
    }

    @Override
    public void lock() {
        try {
            lock.acquire();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("上锁失败...");
        }
    }

    @Override
    public boolean tryLock() {
        boolean res = false;
        try {
            res =  this.lock.acquire(0,TimeUnit.NANOSECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取锁失败...");
        }
        return res;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        boolean res = false;
        try {
            res =  this.lock.acquire(time,unit);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("获取锁失败...");
        }
        return res;
    }

    @Override
    public void unlock() {
        try {
            this.lock.release();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("释放锁失败...");
        }
    }
}
