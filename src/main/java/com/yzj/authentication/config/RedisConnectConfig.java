package com.yzj.authentication.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConnectConfig {

    @Autowired
    private RedisClient defaultClient;

    @Bean
    public StatefulRedisConnection<String, String> redisConnection() {
        return defaultClient.connect();
    }
}
