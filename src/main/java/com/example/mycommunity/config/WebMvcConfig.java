package com.example.mycommunity.config;

import com.example.mycommunity.controller.interceptor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AlphaInterceptor alphaInterceptor;// 用于拦截请求，打印日志,用于测试

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;// 用于拦截请求，检查用户是否登录,并将用户信息存入ThreadLocal

//    @Autowired
//    private LoginRequiredInterceptor loginRequiredInterceptor;// 用于拦截请求，检查用户是否登录

    @Autowired
    private MessageInterceptor messageInterceptor;// 用于拦截请求，检查用户是否登录,并将用户信息存入ThreadLocal

    @Autowired
    private DataInterceptor dataInterceptor;//

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(alphaInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg")
                .addPathPatterns("/register", "/login");// 拦截所有请求，但是排除静态资源，只拦截注册和登录请求，用于测试

        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");// 拦截所有请求，但是排除静态资源，

//        registry.addInterceptor(loginRequiredInterceptor)
//                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(messageInterceptor)// 拦截所有请求，但是排除静态资源，
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");

        registry.addInterceptor(dataInterceptor)// 拦截所有请求，但是排除静态资源，
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg");
    }

}
