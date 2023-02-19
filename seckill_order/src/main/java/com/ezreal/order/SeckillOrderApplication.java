package com.ezreal.order;

import com.ezreal.clients.SeckillActivityClient;
import com.ezreal.clients.SeckillGoodClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients(clients = {SeckillGoodClient.class, SeckillActivityClient.class})
@EnableScheduling
@ComponentScan(basePackages = {"com.ezreal.common", "com.ezreal.order"})
public class SeckillOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillOrderApplication.class, args);
    }
}
