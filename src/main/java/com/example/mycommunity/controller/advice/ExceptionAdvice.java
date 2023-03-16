package com.example.mycommunity.controller.advice;

import com.example.mycommunity.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@ControllerAdvice(annotations = Controller.class)//指定拦截哪些类,这里拦截所有的Controller，出现异常时会调用下面的方法
public class ExceptionAdvice {//异常处理类

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);
    @ExceptionHandler
        public void handleException(Exception e , HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("服务器发生异常:" + e.getMessage());
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }
        String XRequestedWith = request.getHeader("X-Requested-With");//判断是普通请求还是异步请求
        if("XMLHttpRequest".equals(XRequestedWith)) {//异步请求
            response.setContentType("application/plain;charset=utf-8");
            response.getWriter().write(CommunityUtil.getJSONString(1, "服务器异常!"));
        } else {
            response .sendRedirect(request.getContextPath() + "/error");
        }
    }
}
