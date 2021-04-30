package com.only.mysql.lock;

import com.only.base.AbstractLock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * fu(for update)锁，可重入基于行锁，不支持行锁的无效或锁表，支持阻塞和非阻塞
 * create table fu_distribute_lock(
 * id int unsigned auto_increment primary key,
 * lock_name varchar(100) not null,
 * unique(lock_name)
 * ) engine=innodb;
 */
@Slf4j
public class MysqlDistributeLockFU extends AbstractLock {
    private static final String SELECT_SQL = "select * from method_lock where method_name = ? for update";
    private static final String INSERT_SQL = "insert into method_lock ( method_name ) values(?)";
    private final ExecutorService threadPoolExecutor = Executors.newSingleThreadExecutor();
    private final DataSource dataSource;
    private Connection connection;
    private final String lockName;

    public MysqlDistributeLockFU(DataSource dataSource, String lockName) {
        this.dataSource = dataSource;
        this.lockName = lockName;
    }

    @Override
    public void lock() {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            for (; ; ) {
                this.connection = dataSource.getConnection();
                connection.setAutoCommit(false);
                statement = connection.prepareStatement(SELECT_SQL);
                statement.setString(1, lockName);
                resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    return;
                }
                Utils.gracefulClose(resultSet, statement, connection);
                log.info("锁记录不存在，正在创建");
                PreparedStatement insert = null;
                try {
                    Connection connection = dataSource.getConnection();
                    insert = connection.prepareStatement(INSERT_SQL);
                    insert.setString(1, lockName);
                    if (insert.executeUpdate() == 1) {
                        log.info("创建锁记录成功");
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } finally {
                    Utils.gracefulClose(insert, connection);
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            Utils.gracefulClose(resultSet, connection);
        }

    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        final Future<?> future = threadPoolExecutor.submit(() -> {
            try {
                lock();
                return 1;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        try {
            final Object o = future.get(time, unit);
            if (o == null) {
                future.cancel(true);
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @SneakyThrows
    @Override
    public void unlock() {
        connection.commit();
        connection.close();
    }

    //  username: root
    //        password: Meeting#12345
    //        url: jdbc:mysql://118.89.165.94:3306/repair_server?characterEncoding=UTF-8&useSSL=false&allowMultiQueries=true&serverTimezone=Asia/Shanghai&useAffectedRows=true
    //
}
