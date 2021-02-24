package com.only.lock;


import com.alibaba.druid.pool.ha.PropertiesUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.only.base.DistributedLock;
import com.only.dao.entity.MethodLock;
import com.only.dao.mapper.MethodLockMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

@Service
public class MySqlLockImpl implements DistributedLock {

    @Autowired
    MethodLockMapper methodLockMapper;

    @Autowired
    DataSourceTransactionManager manager;
    @Autowired
    TransactionDefinition transaction;
    @Autowired
    PropertiesBeanDefinitionReader sd;

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) {
        //尝试获取锁
        /*String sql = "select id from method_lock where id = ? for update";
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection();
            Statement st = connection.createStatement();
            ResultSet resultSet = st.executeQuery(sql);


        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }*/
        LambdaQueryWrapper<MethodLock> lock = new LambdaQueryWrapper<>();
        //methodLockMapper.insert()
        return false;
    }

    @Override
    public void releaseLock(String key) {

    }

    private MethodLock getEntity(String methodName){
        MethodLock methodLock =new MethodLock();
        methodLock.setMethodName(methodName);

        return methodLock;
    }
}
