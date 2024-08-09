package com.GASB.file.service.rabbitmq;

import com.GASB.file.service.history.FileGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

//import static org.hibernate.internal.CoreLogging.logger;

@Service
public class RabbitMQListener {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQListener.class);

    @Autowired
    private FileGroupService fileGroupService;

    @RabbitListener(queues = "#{@rabbitMQProperties.groupingQueue}")
    public void onVtReportRequestReceived(long eventId) {
//        logger.info("received : {}", actId);
        System.out.println("received :" + eventId);
        fileGroupService.groupFilesAndSave(eventId);
    }
}
