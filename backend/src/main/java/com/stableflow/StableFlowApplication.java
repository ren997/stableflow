package com.stableflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.stableflow")
public class StableFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(StableFlowApplication.class, args);
    }
}
