package com.example.mycommunity.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//@Component
//@Aspect
public class AlphaAspect {
    @Pointcut("execution(* com.example.mycommunity.service.*.*(..))")// 该方法用于定义切点，即哪些方法需要被拦截
    public void pointcut() {// 该方法不需要实现，只是为了让@Pointcut注解能够生效
    }

    @Before("pointcut()")// 在切点之前执行
    public void before() {
        System.out.println("before");
    }

    @After("pointcut()")// 在切点之后执行
    public void after() {
        System.out.println("after");
    }
    @AfterReturning("pointcut()")// 在切点之后执行，如果切点抛出异常，则不执行
    public void afterReturning() {
        System.out.println("afterReturning");
    }

    @AfterThrowing("pointcut()")// 在切点之后执行，如果切点抛出异常，则执行
    public void afterThrowing() {
        System.out.println("afterThrowing");
    }

    @Around("pointcut()")// 在切点之前和之后执行
    public Object around( ProceedingJoinPoint joinPoint) throws Throwable {// 该方法的参数必须是ProceedingJoinPoint,
        System.out.println("around before");                                     // 否则会报错,该参数用于调用切点方法,即调用业务方法
        Object obj =joinPoint.proceed();// 调用切点方法,即调用业务方法,如果业务方法抛出异常,则不会执行下面的代码
        System.out.println("around after");
        return obj;
    }
}
