package com.example.mycommunity.dao;

import com.example.mycommunity.entity.LoginTicket;
import org.apache.ibatis.annotations.*;

@Mapper
@Deprecated
public interface LoginTicketMapper {//登录凭证，用于记录用户登录状态，以及登录凭证的状态，如是否过期，是否被注销，是否被封禁等，
    // 登录凭证的状态是通过ticket来判断的，ticket是一个随机生成的字符串，每次用户登录时，都会生成一个ticket，ticket会被存储在数据库中，
    // 同时会被存储在cookie中，当用户访问其他页面时，会携带这个ticket，
    // 服务器会根据ticket来判断用户的登录状态，如果ticket存在，说明用户已经登录，如果ticket不存在，说明用户未登录
    // 通过ticket来判断用户的登录状态，这样就不需要每次都去数据库中查询用户的登录状态了，这样可以提高效率，也可以减少数据库的压力

    @Insert({
            "insert into login_ticket(user_id,ticket,status,expired) ",
            "values(#{userId},#{ticket},#{status},#{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);//插入登录凭证，返回值为影响的行数，如果返回值为1，说明插入成功，如果返回值为0，说明插入失败，这里使用了@Options注解，这个注解可以获取自增主键

    @Select({
            "select id,user_id,ticket,status,expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);

    @Update({
            "<script>",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1=1 ",
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status);

}
