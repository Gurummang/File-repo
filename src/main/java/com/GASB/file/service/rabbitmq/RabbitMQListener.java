package com.GASB.file.service.rabbitmq;

import com.GASB.file.service.history.FileGroupService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RabbitMQListener {

    @Autowired
    private FileGroupService fileGroupService;

    @RabbitListener(queues = "#{@rabbitMQProperties.groupingQueue}")
    public void onVtReportRequestReceived(long actId) {
        System.out.println("received : " + actId);
        fileGroupService.groupFilesAndSave(actId);
    }
}
