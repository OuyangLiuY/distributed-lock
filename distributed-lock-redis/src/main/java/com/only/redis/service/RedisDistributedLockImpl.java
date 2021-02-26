package com.only.redis.service;


import com.only.base.DistributedLock;
import com.only.redis.util.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Slf4j
@Service
public class RedisDistributedLockImpl implements RedisDistributeLock {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private final ThreadLocal<String> threadLocal = new ThreadLocal<>();

    private final ThreadLocal<Integer> threadLocalInteger = new ThreadLocal<>();

    /**
     * redis实现锁的简单方式，思想：setValue成功获取锁，（否则循环等待尝试set），释放锁，直接删除当前key，--非重入方式
     * @param key
     * @param timeout
     * @param unit
     * @return
     */
    @Override
    public Boolean tryLock(String key, long timeout, TimeUnit unit) {
        Boolean isLocked;
        //log.info("尝试获取 redis lock 锁");
        long futureTime = timeout + System.currentTimeMillis();
        String uuid = null;
        for (; ; ) {
            if (threadLocal.get() == null) {
                uuid = UUID.randomUUID().toString();
                threadLocal.set(uuid);
            }
            assert uuid != null;
            isLocked = stringRedisTemplate.opsForValue().setIfAbsent(key, uuid, timeout, unit);
            if (isLocked != null && isLocked) {
                log.info("获取锁成功 = " + uuid);
                return true;
            }
            long now = System.currentTimeMillis();
            /*if(futureTime - now < 0){
                break;
            }*/
        }
        //throw  new RuntimeException("获取锁超时...");
    }

    @Override
    public void releaseLock(String key) {
        // 判断当前线程所对应的uuid是否与Redis对应的uuid相同，再执行删除锁操作
        log.info("释放当前锁 = " + threadLocal.get());
        if (threadLocal.get().equals(stringRedisTemplate.opsForValue().get(key))) {
            stringRedisTemplate.delete(key);
        }
    }

    @Override
    public boolean tryReLock(String key, long timeout, TimeUnit unit) {
        long futureTime = timeout + System.currentTimeMillis();
        String uuid = threadLocal.get();
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
            threadLocal.set(uuid);
        }
        for (; ; ) {
            log.info("uuid = " + uuid);
            Long res = stringRedisTemplate.opsForHash().increment(key, uuid, 1L);
            if (res > 0) {
                log.info("获取锁成功 = " + uuid);
                return true;
            }
            long now = System.currentTimeMillis();
            /*if(futureTime - now < 0){
                break;
            }*/
        }
    }

    @Override
    public void releaseReLock(String key) {
        String uuid = threadLocal.get();
        log.info("释放当前锁 = " + uuid);
        long res  = Long.parseLong(String.valueOf(stringRedisTemplate.opsForHash().get(key,uuid )));
        System.out.println("res = " + res);
        if(res > 1){
            stringRedisTemplate.opsForHash().increment(key, threadLocal.get(),-1L);
        }else {
            stringRedisTemplate.opsForHash().delete(key,uuid);
        }
    }
}
