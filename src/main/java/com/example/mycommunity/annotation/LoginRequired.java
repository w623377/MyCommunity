package com.example.mycommunity.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//注解作用的目标，这里是方法
@Retention(RetentionPolicy.RUNTIME)//注解的生命周期，这里是运行时
public @interface LoginRequired {
}
