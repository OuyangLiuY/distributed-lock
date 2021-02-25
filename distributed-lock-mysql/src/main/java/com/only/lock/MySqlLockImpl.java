package com.only.lock;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.only.dao.entity.MethodLock;
import com.only.dao.mapper.MethodLockMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MySqlLockImpl implements MySqlDistributedLock {

    @Autowired
    MethodLockMapper methodLockMapper;
    @Autowired
    DataSourceTransactionManager dataSourceTransactionManager;
    @Autowired
    TransactionDefinition transactionDefinition;

    private final ThreadLocal<TransactionStatus> transaction = new ThreadLocal<>();

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
    public boolean simpleLock(String methodName, long timeout, TimeUnit unit) {
        return tryLock(methodName,timeout,unit);
    }

    @Override
    public boolean optimisticLock(String methodName, long timeout, TimeUnit unit) throws Exception {
        LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
        methodLock.eq(MethodLock::getMethodName,methodName);
        methodLock.eq(MethodLock::getState,false);
      //  isContains(methodName);
        // 前提：当前数据必须存在且数据的状态state必须为false
        for(;;){
            MethodLock one = methodLockMapper.selectOne(methodLock);
            if(one != null){
                LambdaQueryWrapper<MethodLock> update = new LambdaQueryWrapper<>();
                update.eq(MethodLock::getMethodName,methodName);
                update.eq(MethodLock::getVersion,one.getVersion());
                update.eq(MethodLock::getState,false);
                one.setState(true);
                one.setVersion(one.getVersion() + 1);
                int res;
                try {
                    res = methodLockMapper.update(one, update);
                }catch (Exception e){
                    //更新失败，继续尝试获取锁
                    e.printStackTrace();
                    log.error("获取锁失败,休息20毫秒继续获取");
                    continue;
                }
                //更新成功获取锁
               if( res > 0){
                   log.info("获取锁成功");
                   return true;
               }
            }
        }
    }

    @Override
    public void releaseOptimisticLock(String methodName) {
        LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
        methodLock.eq(MethodLock::getMethodName,methodName);
        MethodLock one = methodLockMapper.selectOne(methodLock);
        if(one != null){
            if(one.getState()){
                one.setState(false);
                one.setVersion(one.getVersion() - 1);
                methodLock.ge(MethodLock::getVersion,0);
                methodLockMapper.updateById(one);
            }
        }
    }

    @Override
    public boolean pessimisticLock(String methodName, long timeout, TimeUnit unit) {
        String sql = "SELECT id from method_lock where method_name = ? for UPDATE";
        //isContains(methodName);
        transaction.set(dataSourceTransactionManager.getTransaction(transactionDefinition));
        long waitTime = 500;
        long futureTime = System.currentTimeMillis() + timeout;
        long reTime = timeout;
        try {
            for (;;){
                try {
                    MethodLock methodLock = methodLockMapper.selectByMethodName(methodName);
                    return methodLock != null;
                }catch (Exception e){
                    e.printStackTrace();
                }
                reTime = futureTime - System.currentTimeMillis();
                if (reTime <= 0)
                    break;
                if (reTime < waitTime) {
                    waitTime = reTime;
                }
            }
            return false;
        }catch (Exception e){
            dataSourceTransactionManager.rollback(transaction.get());
            return false;
        }
    }

    private boolean isContains(String methodName) throws Exception {
        if(getLockDateByName(methodName) != null){
            return true;
        }
        methodLockMapper.insert(getEntity(methodName));
        return false;
    }

    @Override
    public void releasePessimisticLock(String methodName) {
        dataSourceTransactionManager.commit(transaction.get());
    }

    @Override
    public List<MethodLock> getLockData() {
        LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
        return methodLockMapper.selectList(methodLock);
    }

    private MethodLock getLockDateByName(String methodName ){
        LambdaQueryWrapper<MethodLock> methodLock = new LambdaQueryWrapper<>();
        methodLock.eq(MethodLock::getMethodName,methodName);
        return methodLockMapper.selectOne(methodLock);
    }
}
