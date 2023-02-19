package com.ezreal.goods;

import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.ezreal.clients.SeckillActivityClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableFeignClients(clients = {SeckillActivityClient.class})
@EnableDiscoveryClient
@ComponentScan(basePackages = {"com.ezreal.common", "com.ezreal.goods"})
public class SeckillGoodApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillGoodApplication.class, args);
    }
}
