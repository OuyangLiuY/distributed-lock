package com.only;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan(value = {"com.only.dao"})
public class MySqlLockApplication {
    public static void main(String[] args) {
        SpringApplication.run(MySqlLockApplication.class, args);
    }
}
