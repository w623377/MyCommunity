package com.example.mycommunity.controller.interceptor;

import com.example.mycommunity.entity.User;
import com.example.mycommunity.service.DataService;
import com.example.mycommunity.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataInterceptor implements HandlerInterceptor {
    @Autowired
    private HostHolder  hostHolder;

    @Autowired
    private DataService  dataService;

    //在Controller之前执行,统计uv和dau,在每次请求之前都会执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //统计uv
        String ip =request.getRemoteHost();
        dataService.recordUV(ip);

        //统计DAU
        User user = hostHolder.getUser();
        if(user !=null){
            dataService.recordDAU(user.getId());
        }
        return true;
    }
}
