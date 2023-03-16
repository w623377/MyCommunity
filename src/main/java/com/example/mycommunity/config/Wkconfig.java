package com.example.mycommunity.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class Wkconfig {
    //实例化logger
    private static final Logger logger = LoggerFactory.getLogger(Wkconfig.class);
    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @PostConstruct
    public void init() {
        //设置wk图片存放的路径
        File file=new File(wkImageStorage);
        if(!file.exists()){
            //如果文件夹不存在，创建文件夹
            file.mkdirs();
            logger.info("创建wk图片存放的文件夹: "+wkImageStorage);
        }
    }
}
