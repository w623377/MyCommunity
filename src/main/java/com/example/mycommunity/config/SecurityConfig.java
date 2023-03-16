package com.example.mycommunity.config;

import com.example.mycommunity.util.CommunityConstant;
import com.example.mycommunity.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant{

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    //
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(AUTHORITY_USER,//用户
                        AUTHORITY_ADMIN,//管理员
                        AUTHORITY_MODERATOR//版主
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        AUTHORITY_MODERATOR//版主
                )
                .antMatchers("/discuss/delete","/data/**","/actuator/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_ADMIN
                )
                .anyRequest().permitAll()//其他任何请求都可以访问
                .and().csrf().disable();//关闭csrf

            //权限不够的时候处理
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {//没有登录
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
                        String requestHeader = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(requestHeader)) {//如果是异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            response.getWriter().write(CommunityUtil.getJSONString(403, "你还没有登录哦！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() {//权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
                        String requestHeader = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(requestHeader)) {//如果是异步请求
                            response.setContentType("application/plain;charset=utf-8");
                            response.getWriter().write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限！"));
                        } else {
                            response.sendRedirect(request.getContextPath() + "/denied");
                        }
                    }
                });
        //Security底层默认会拦截/logout请求，进行退出处理
        //覆盖它默认的逻辑，才能执行我们自己的退出代码
        http.logout().logoutUrl("/securitylogout");
    }
}
