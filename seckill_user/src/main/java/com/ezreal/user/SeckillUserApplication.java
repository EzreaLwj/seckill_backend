package com.ezreal.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ezreal.common", "com.ezreal.user"})
public class SeckillUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillUserApplication.class, args);
    }
}
