package com.only.lock;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.only.base.DistributedLock;
import com.only.dao.entity.MethodLock;
import com.only.dao.mapper.MethodLockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MySqlLockImpl implements MySqlDistributedLock {

    @Autowired
    MethodLockMapper methodLockMapper;
    @Autowired
    DataSourceTransactionManager manager;
    @Autowired
    TransactionDefinition transaction;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        //尝试获取锁
        int res;
        long begin = System.currentTimeMillis();
        try {
            res = methodLockMapper.insert(getEntity(key));
            long end = System.currentTimeMillis();
            res = (end - begin) <=  unit.toMillis(timeout) && res > 0 ? 1 : -1;
        }catch (Exception e){
            e.printStackTrace();
            log.error("获取锁失败");
            res = -1;
        }
        return res > 0;
    }

    @Override
    public void releaseLock(String key) {
        try {
            LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
            methodLock.eq(MethodLock::getMethodName,key);
            int res = methodLockMapper.delete(methodLock);
            log.info("解锁成功，res = {}" , res);
        }catch (Exception e){
            e.printStackTrace();
            log.error("获取锁失败");
        }
    }

    private MethodLock getEntity(String methodName) throws UnknownHostException {
        MethodLock methodLock =new MethodLock();
        methodLock.setMethodName(methodName);
        methodLock.setState(true);
        methodLock.setVersion(1);
        methodLock.setThreadId(String.valueOf(Thread.currentThread().getId()));
        InetAddress addr = InetAddress.getLocalHost();
        methodLock.setIp(addr.getHostAddress());
        return methodLock;
    }

    @Override
    public boolean getLock(String methodName,long timeout, TimeUnit unit) {
        return tryLock(methodName,timeout,unit);
    }

    @Override
    public List<MethodLock> getLockData() {
        LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
        return methodLockMapper.selectList(methodLock);
    }
}
