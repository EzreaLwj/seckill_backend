package com.ezreal.activity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.ezreal.common", "com.ezreal.activity"})
public class SeckillActivityApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillActivityApplication.class, args);
    }
}
