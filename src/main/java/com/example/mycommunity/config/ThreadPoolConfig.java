package com.example.mycommunity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling//允许定时任务，不加这个注解，定时任务不会生效
@EnableAsync//允许异步任务，不加这个注解，异步任务不会生效
public class ThreadPoolConfig {
}
