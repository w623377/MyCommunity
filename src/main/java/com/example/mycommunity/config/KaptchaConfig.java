package com.example.mycommunity.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

//配置验证码，
@Configuration
public class KaptchaConfig {

    @Bean
    public Producer kaptchaProducer() {
        Properties properties = new Properties();
        properties.setProperty("kaptcha.image.width", "100");
        properties.setProperty("kaptcha.image.height", "40");
        properties.setProperty("kaptcha.textproducer.font.size", "32");//字体大小
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");//字体颜色
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYAZ");//字体内容
        properties.setProperty("kaptcha.textproducer.char.length", "4");//字体长度
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");//干扰线，这里是没有干扰线

        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }

}
