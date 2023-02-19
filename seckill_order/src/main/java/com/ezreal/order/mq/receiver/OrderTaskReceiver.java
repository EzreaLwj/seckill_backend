package com.ezreal.order.mq.receiver;

import com.alibaba.fastjson.JSON;
import com.ezreal.common.model.mq.task.PlaceOrderTask;
import com.ezreal.order.mq.config.MqConfig;
import com.ezreal.order.service.impl.queue.QueuedPlaceOrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

@Component
@Slf4j
@ConditionalOnProperty(name = "place_order_type", havingValue = "queued")
public class OrderTaskReceiver {

    private static final Logger logger = LoggerFactory.getLogger(OrderTaskReceiver.class);

    @Autowired
    private QueuedPlaceOrderServiceImpl queuedPlaceOrderService;

    @RabbitListener(queues = MqConfig.SECKILL_QUEUE_NAME)
    public void receiveOrderTask(Message message) {
        logger.info("OrderTaskReceiver|接收下单消息");
        if (message == null) {
            logger.info("OrderTaskReceiver|接收下单任务消息为空");
            return;
        }

        String messageString = new String(message.getBody(), Charset.defaultCharset());
        PlaceOrderTask placeOrderTask = JSON.parseObject(messageString, PlaceOrderTask.class);
        try {
            queuedPlaceOrderService.handlePlaceOrderTask(placeOrderTask);
            logger.info("OrderTaskReceiver|下单任务成功{}|", messageString);
        } catch (Exception e) {
            logger.info("OrderTaskReceiver|下单任务失败|{}", messageString);
        }
    }
}
