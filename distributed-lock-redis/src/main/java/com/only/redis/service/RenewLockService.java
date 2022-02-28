package com.only.redis.service;

/**
 * @author yueyi2019
 */
public interface RenewLockService {

    /**
     * 续约
     * @param key
     * @param value
     * @param time
     */
    public void renewLock(String key , String value , int time);
}
