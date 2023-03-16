package com.example.mycommunity.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.RequestHandledEvent;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

@Component
@Aspect
public class ServiceLogAspect {//用于记录日志
    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    @Pointcut("execution(* com.example.mycommunity.service.*.*(..))")
    public void pointcut() {}

    @Before("pointcut()")
    public void before(JoinPoint joinPoint) {//JoinPoint是一个接口，用于获取目标方法的信息
        //用户[1.2.3.4],在[xxx],访问了[com.nowcoder.community.service.xxx()].
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {//如果requestAttributes为空，直接返回,这是特殊的调用，
                    // 不是用户发起的,比如定时任务，消息队列的消费者调用，因为这些调用没有request，也就没有ip，所以不记录
            return;//这里的return是跳出方法，不是跳出if
        }
        HttpServletRequest request = requestAttributes.getRequest();//
        String ip = request.getRemoteHost();
        String now = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss").format(new Date());
        String target=joinPoint.getSignature().getDeclaringTypeName()+"."+joinPoint.getSignature().getName();//获取类名和方法名
        logger.info(String.format("用户[%s],在[%s],访问了[%s].",ip,now,target));
    }



}
