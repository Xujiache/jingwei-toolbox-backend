package com.xujiache.jingweitoolbox.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {
    @Bean
    ObjectMapper platformObjectMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }
}
