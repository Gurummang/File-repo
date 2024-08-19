package com.GASB.file.service.rabbitmq;

import com.GASB.file.service.history.FileGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RabbitMQListener {

    private final FileGroupService fileGroupService;

    @Autowired
    public RabbitMQListener(FileGroupService fileGroupService){
        this.fileGroupService =fileGroupService;
    }


    @RabbitListener(queues = "#{@rabbitMQProperties.groupingQueue}")
    public void onVtReportRequestReceived(long eventId) {
        log.info("received : {}", eventId);
        fileGroupService.groupFilesAndSave(eventId);
    }
}
