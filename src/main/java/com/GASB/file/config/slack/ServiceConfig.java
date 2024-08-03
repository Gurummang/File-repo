package com.GASB.file.config.slack;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public ExtractData reqeustToJson() {
        return new ExtractData();
    }
}
