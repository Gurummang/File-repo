package com.GASB.file.service.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQListener {

    @RabbitListener(queues = "#{@rabbitMQProperties.groupingQueue}")
    public void onVtReportRequestReceived(long activitiesId) {
        System.out.println("received : " + activitiesId);
    }
}
