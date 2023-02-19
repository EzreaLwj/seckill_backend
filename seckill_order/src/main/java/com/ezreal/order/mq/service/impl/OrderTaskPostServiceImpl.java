package com.ezreal.order.mq.service.impl;

import com.alibaba.fastjson.JSON;
import com.ezreal.order.mq.config.MqConfig;
import com.ezreal.order.mq.service.OrderTaskPostService;
import com.ezreal.common.model.mq.task.PlaceOrderTask;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@ConditionalOnProperty(name = "place_order_type", havingValue = "queued")
public class OrderTaskPostServiceImpl implements OrderTaskPostService {
    private Logger logger = LoggerFactory.getLogger(OrderTaskPostServiceImpl.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public boolean post(PlaceOrderTask placeOrderTask) {
        logger.info("投递下单任务|{}", JSON.toJSONString(placeOrderTask));
        if (placeOrderTask == null) {
            logger.info("下单任务参数为空");
            return false;
        }
        String placeOrderTaskString = JSON.toJSONString(placeOrderTask);

        try {
            rabbitTemplate.convertAndSend(MqConfig.SECKILL_EXCHANGE_NAME, MqConfig.SECKILL_ROUTING_KEY, placeOrderTaskString);
            logger.info("OrderTaskPostServiceImpl|任务投递成功|{}", placeOrderTaskString);
            return true;
        } catch (AmqpException e) {
            logger.info("OrderTaskPostServiceImpl|任务投递失败|{}", placeOrderTaskString);
            return false;
        }
    }
}
