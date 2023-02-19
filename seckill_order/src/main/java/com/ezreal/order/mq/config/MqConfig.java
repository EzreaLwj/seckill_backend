package com.ezreal.order.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    public static final String SECKILL_QUEUE_NAME = "seckill_queue";
    public static final String SECKILL_EXCHANGE_NAME = "seckill_exchange";
    public static final String SECKILL_ROUTING_KEY = "seckill_routing_key";

    // 配置交换机
    @Bean("seckill_exchange")
    public DirectExchange newSeckillDirectExchange() {
        return ExchangeBuilder
                .directExchange(SECKILL_EXCHANGE_NAME)
                .durable(true)
                .build();
    }

    // 配置队列
    @Bean("seckill_queue")
    public Queue newSeckillQueue() {
        return QueueBuilder.durable(SECKILL_QUEUE_NAME).build();
    }

    // 绑定队列和交换机
    @Bean
    public Binding bindingSeckillExchangeAndQueue(@Qualifier("seckill_queue") Queue queue,
                                           @Qualifier("seckill_exchange") DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(SECKILL_ROUTING_KEY);
    }

}
