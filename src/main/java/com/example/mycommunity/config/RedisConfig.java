package com.example.mycommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;


@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();// 创建RedisTemplate对象 ,用于操作redis
        template.setConnectionFactory(factory);// 设置连接工厂,连接redis,否则会报错
        //设置key的序列化器
        template.setKeySerializer(RedisSerializer.string());
        //设置value的序列化器
        template.setValueSerializer(RedisSerializer.json());
        //设置hash的key的序列化器
        template.setHashKeySerializer(RedisSerializer.string());
        //设置hash的value的序列化器
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();// 初始化RedisTemplate对象,否则会报错

        return template;// 返回RedisTemplate对象
    }

}
