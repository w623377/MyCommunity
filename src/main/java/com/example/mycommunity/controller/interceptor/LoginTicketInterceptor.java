package com.example.mycommunity.controller.interceptor;

import com.example.mycommunity.entity.LoginTicket;
import com.example.mycommunity.entity.User;
import com.example.mycommunity.service.UserService;
import com.example.mycommunity.util.CookieUtil;
import com.example.mycommunity.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.naming.AuthenticationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {//这个类是用来拦截请求的，如果请求中有凭证，就查询凭证，如果凭证有效，就查询用户，
    // 然后把用户放到hostHolder中，这样在后面的postHandle方法中，就可以直接使用hostHolder中的user，不用再次查询数据库，提高效率
    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;//这里的hostHolder是一个线程变量，每个线程都有一个hostHolder，所以不用担心线程安全问题

    // 在请求处理之前进行调用（Controller方法调用之前）
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从cookie中获取凭证
        String ticket = CookieUtil.getValue(request, "ticket");

        if (ticket != null) {
            // 查询凭证
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            // 检查凭证是否有效
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {//凭证有效,并且没有过期,就查询用户,如果退出登录了，就会把凭证的状态改为1，所以这里要判断凭证的状态
                // 根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中持有用户
                hostHolder.setUser(user);//这里的user是一个对象，是一个引用，所以在后面的postHandle方法中，可以直接使用，不用再次查询数据库，提高效率
                                        //hostHolder是一个线程变量，每个线程都有一个hostHolder，所以不用担心线程安全问题

                //构建用户认证的结果，并存入SecurityContext，方便Security进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, //
                        user.getPassword(),
                        userService.getAuthorities(user.getId()));
                //把用户认证的结果存入SecurityContext
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }

        return true;
    }

    // 在模板引擎之前执行,所以可以在这里把user传给模板引擎,这样在模板引擎中就可以使用user了
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);//这里的user是一个对象，是一个引用，所以在后面的postHandle方法中，可以直接使用，不用再次查询数据库，提高效率
        }
    }

    // 在模板引擎之后执行,所以可以在这里把user清除,这样在模板引擎中就不可以使用user了
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        SecurityContextHolder.clearContext();
    }
}
