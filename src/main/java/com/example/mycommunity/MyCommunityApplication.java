package com.example.mycommunity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class MyCommunityApplication {

    @PostConstruct
    public void init(){
        // 解决netty启动冲突问题,因为netty4.1.48.Final版本会导致es启动冲突，所以需要设置下面的系统属性
        // see Netty4Utils.setAvailableProcessors()
        //解决与elasticsearch冲突的问题
        System.setProperty("es.set.netty.runtime.available.processors", "false");
    }
    public static void main(String[] args) {
        SpringApplication.run(MyCommunityApplication.class, args);
    }

}
