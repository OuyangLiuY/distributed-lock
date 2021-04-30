package com.only.etcd.lock;

import com.google.common.collect.Maps;
import com.only.base.AbstractLock;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.lock.LockResponse;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class EtcdDistributedLock extends AbstractLock {
    private Lock lock;
    private final Lease lease;
    private String lockKey;
    private String lockPath;
    private AtomicInteger lockCount;
    /** 租约有效期,防止客户端崩溃，可在租约到期后自动释放锁；另一方面，正常执行过程中，会自动进行续租,单位 ns */
    private final Long leaseTTL;
    /** 续约锁租期的定时任务，初次启动延迟，单位默认为 s,默认为1s，可根据业务定制设置*/
    private Long initDelay = 1L;
    /** 定时任务线程池类 */
    ScheduledExecutorService service = null;
    /** 保存线程与锁对象的映射，锁对象包含重入次数，重入次数的最大限制为Int的最大值 */
    private final ConcurrentMap<Thread, LockData> threadData = Maps.newConcurrentMap();

    public EtcdDistributedLock(Client client, String lockPath, Long leaseTTL, TimeUnit unit) {
        this.lease = client.getLeaseClient();
        this.lockPath = lockPath;
        this.leaseTTL = unit.toNanos(leaseTTL);
        this.service = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void lock() {
        Thread thread = Thread.currentThread();
        LockData lockData = threadData.get(thread);
        if(lockData != null && lockData.isLockSuccess()){ // 得重入了
            int res = lockData.getLockCount().incrementAndGet();
            System.out.println("重入的次数 count = " + res);
            return;
        }
        // 第一次获取锁
        long leaseId = 0;
        try {
            leaseId = lease.grant(TimeUnit.NANOSECONDS.toNanos(leaseTTL)).get().getID();
            // 续租心跳周期
            long period = leaseTTL - leaseTTL / 5;
            // 启动定时任务续约
            service.scheduleWithFixedDelay(new KeepAlive(lease,leaseId),initDelay,period,TimeUnit.NANOSECONDS);
            LockResponse lockResponse = lock.lock(ByteSequence.from(lockKey.getBytes()), leaseId).get();
            if(lockResponse != null){
                lockPath = lockResponse.getKey().toString(StandardCharsets.UTF_8);
                log.info("获取锁成功,锁路径:{},线程:{}",lockPath,thread.getName());
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        // 获取锁成功，锁对象设置
        LockData newLockData = new LockData(thread, lockKey);
        newLockData.setLeaseId(leaseId);
        newLockData.setExecutorService(service);
        threadData.put(thread, newLockData);
        newLockData.setLockSuccess(true);
    }

    @Override
    public void unlock() {
        Thread thread = Thread.currentThread();
        LockData lockData = threadData.get(thread);
        if (lockData == null){
            throw new IllegalMonitorStateException("You do not own the lock: " + lockKey);
        }
        // 减去次数
        int newLockCount = lockData.lockCount.decrementAndGet();
        if ( newLockCount > 0 ) {
            return;
        }
        if ( newLockCount < 0 ) {
            throw new IllegalMonitorStateException("Lock count has gone negative for lock: " + lockKey);
        }
        try {
            // 释放锁
            if(lockPath != null){
                lock.unlock(ByteSequence.from(lockPath.getBytes())).get();
            }
            // 关闭定时任务
            lockData.getExecutorService().shutdown();
            // 删除租约
            if (lockData.getLeaseId() != 0L) {
                lease.revoke(lockData.getLeaseId());
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("解锁失败",e);
        }finally {
            // 移除当前线程资源
            threadData.remove(thread);
        }
    }

    private static class KeepAlive implements Runnable{
        private final Lease lease;
        private final long leaseId;

        public KeepAlive(Lease lease, long leaseId) {
            this.lease = lease;
            this.leaseId = leaseId;
        }

        @Override
        public void run() {
            // 对当前ID进行一次续约
            lease.keepAliveOnce(leaseId);
        }
    }
}
