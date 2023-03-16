package com.example.mycommunity.controller.interceptor;

import com.example.mycommunity.annotation.LoginRequired;
import com.example.mycommunity.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    // 在请求处理之前进行调用（Controller方法调用之前）
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod) {// handle是拦截的目标对象，如果是HandlerMethod类型的对象，说明是一个Controller方法
            HandlerMethod handlerMethod = (HandlerMethod) handler;// 将handler强制转换为HandlerMethod类型
            Method method = handlerMethod.getMethod();// 获取HandlerMethod对象中的Method对象
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if (loginRequired != null && hostHolder.getUser() == null) {// 如果方法上有LoginRequired注解，且当前用户未登录
                response.sendRedirect(request.getContextPath() + "/login");// 重定向到登录页面，返回false表示拦截，不再执行Controller方法
                return false;
            }
        }
        return true;
    }
}
