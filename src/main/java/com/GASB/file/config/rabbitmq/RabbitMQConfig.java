package com.GASB.file.config.rabbitmq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
    private final RabbitMQProperties properties;

    public RabbitMQConfig(RabbitMQProperties properties) {
        this.properties = properties;
    }

    // 교환기 설정
    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(properties.getExchange());
    }

    // 큐 설정
    @Bean
    public Queue fileQueue() {
        return new Queue(properties.getGroupingQueue(), true, false, false);
    }

    //바인딩 설정
    @Bean
    public Binding fileBinding(Queue fileQueue, DirectExchange exchange) {
        return BindingBuilder.bind(fileQueue).to(exchange).with(properties.getGroupingRoutingKey());
    }

    // RabbitTemplate 설정
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setExchange(properties.getExchange());
        return rabbitTemplate;
    }
}
