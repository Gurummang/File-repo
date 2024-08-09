package com.GASB.file.service.rabbitmq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQSender {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange = "groupingExchange"; // 교환기 이름
    private final String routingKey = "groupingRoutingKey"; // 라우팅 키

    @Autowired
    public RabbitMQSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendGroupingMessage(Long message) {
        // 특정 교환기와 라우팅 키를 사용할 경우
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        System.out.println("Sent message to grouping queue: " + message);
    }

//    public void sendMessageToQueue(Long message, String queueName) {
//        // 교환기와 라우팅 키를 직접 설정하지 않고 큐 이름만 사용하는 경우
//        rabbitTemplate.convertAndSend(queueName, message);
//        System.out.println("Sent message to queue " + queueName + ": " + message);
//    }
}
