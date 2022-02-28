package com.only.redis.service.impl;

import com.only.redis.service.RenewLockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
public class RenewLockServiceImpl implements RenewLockService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 简单redis锁，手动续约
     * @param key
     * @param value
     * @param time
     */
    @Override
    @Async
    public void renewLock(String key, String value, int time) {
        String v = redisTemplate.opsForValue().get(key);
        if (value.equals(v)){
            int sleepTime = time / 3;
            try {
                Thread.sleep(sleepTime * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            redisTemplate.expire(key,time,TimeUnit.SECONDS);
            renewLock(key,value,time);
        }
    }
}