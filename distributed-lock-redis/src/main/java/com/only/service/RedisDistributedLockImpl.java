package com.only.service;


import com.only.base.DistributedLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class RedisDistributedLockImpl implements DistributedLock {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private ThreadLocal<String> threadLocal = new ThreadLocal<String>();

    private ThreadLocal<Integer> threadLocalInteger = new ThreadLocal<Integer>();
    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        Boolean isLocked = false;
        if (threadLocal.get() == null) {
            String uuid = UUID.randomUUID().toString();
            threadLocal.set(uuid);
            //isLocked = stringRedisTemplate.opsForValue().setIfAbsent(key, uuid, timeout, unit);
        } else {
            isLocked = true;
        }

        // 重入次数加1
        if (isLocked) {
            Integer count = threadLocalInteger.get() == null ? 0 : threadLocalInteger.get();
            threadLocalInteger.set(count++);
        }

        return isLocked;
    }

    @Override
    public void releaseLock(String key) {
        // 判断当前线程所对应的uuid是否与Redis对应的uuid相同，再执行删除锁操作
       /* if (threadLocal.get().equals(stringRedisTemplate.opsForValue().get(key))) {
            Integer count = threadLocalInteger.get();
            // 计数器减为0时才能释放锁
            if (count == null || --count <= 0) {
                stringRedisTemplate.delete(key);
            }
        }*/
    }
}
