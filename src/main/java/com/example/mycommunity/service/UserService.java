package com.example.mycommunity.service;

import com.example.mycommunity.dao.LoginTicketMapper;
import com.example.mycommunity.dao.UserMapper;
import com.example.mycommunity.entity.LoginTicket;
import com.example.mycommunity.entity.User;
import com.example.mycommunity.util.CommunityConstant;
import com.example.mycommunity.util.CommunityUtil;
import com.example.mycommunity.util.MailClient;
import com.example.mycommunity.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${community.path.domain}")
    private String domain;//域名

    @Value("/community")
    private String contextPath;//项目名


//    @Autowired
//    private LoginTicketMapper loginTicketMapper;//登录凭证

    public User findUserById(int id) {
//        return userMapper.selectById(id);
        User user = getCache(id);//先从缓存中取
        if (user == null) {//如果缓存中没有，就从数据库中取
            user = initCache(id);//从数据库中取出来后，放入缓存中
        }
        return user;
    }

    //注册
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (user == null) {
            throw new IllegalArgumentException("参数不能为空!");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }

        // 验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }

        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));//随机生成5位字符串
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));//密码加密
        user.setType(0);//普通用户
        user.setStatus(0);//未激活
        user.setActivationCode(CommunityUtil.generateUUID());//激活码
        user.setHeaderUrl(String.format("http://rr6zp6ole.hn-bkt.clouddn.com/2023030816064%d.png", new Random().nextInt(19)));//随机头像
        user.setCreateTime(new Date());//注册时间
        userMapper.insertUser(user);//插入数据库

        // 激活邮件
        Context context = new Context();//thymeleaf模板引擎,用于发送邮件
        context.setVariable("email", user.getEmail());//设置变量,用于模板引擎,在模板中可以使用
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();//激活链接,激活码+用户id,用于激活,激活码用于验证
        context.setVariable("url", url);//
        String content = templateEngine.process("mail/activation", context);//模板引擎,生成html内容
        mailClient.sendMail(user.getEmail(), "激活账号", content);//发送邮件
        return map;
    }

    //激活
    public int activation(int userId, String code) {//用户id,激活码
        System.out.println("userId:" + userId + " code:" + code);
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1) {
            return ACTIVATION_REPEAT;//重复激活
        } else if (user.getActivationCode().equals(code)) {//激活码正确
            userMapper.updateStatus(userId, 1);//更新状态
            clearCache(userId);//清除缓存,因为状态改变了
            return ACTIVATION_SUCCESS;//激活成功
        } else {
            return ACTIVATION_FAILURE;//激活失败
        }
    }
    //登录，返回map，map中包含ticket，用于登录凭证，过期时间，用户信息
    public Map<String, Object> login(String username, String password, int expiredSeconds) {//账号,密码,过期时间
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证账号
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }

        // 验证状态
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }

        // 验证密码
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }

        //登录成功,生成登录凭证，用于验证登录状态，存入redis
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));//过期时间,当前时间+过期时间,单位是毫秒
        //loginTicketMapper.insertLoginTicket(loginTicket);//插入数据库，生成登录凭证，用于验证登录状态

        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());//生成redis的key
        redisTemplate.opsForValue().set(redisKey, loginTicket);//存入redis,是序列化的对象，不是字符串，所以要自定义序列化器

        map.put("ticket", loginTicket.getTicket());//返回登录凭证
        return map;//返回map
    }

    //退出
    public void logout(String ticket) {

       // oginTicketMapper.updateStatus(ticket, 1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);//生成redis的key
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(redisKey);//从redis中取出登录凭证
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey, loginTicket);//更新redis中的登录凭证
    }

    //查询凭证，用于验证登录状态，返回LoginTicket对象
    public LoginTicket findLoginTicket(String ticket) {
        String redisKey = RedisKeyUtil.getTicketKey(ticket);//生成redis的key
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);//从redis中取出登录凭证
    }

    //更新头像，返回受影响的行数，用于判断是否更新成功
    public int updateHeader(int userId, String headerUrl) {
//        return userMapper.updateHeader(userId, headerUrl);
        int rows = userMapper.updateHeader(userId, headerUrl);//更新数据库
        clearCache(userId);//清除缓存,因为头像改变了
        return rows;
    }

    // 重置密码
    public Map<String, Object> resetPassword(String email, String password) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(email)) {
            map.put("emailMsg", "邮箱不能为空!");
            return map;
        }
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }

        // 验证邮箱
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            map.put("emailMsg", "该邮箱尚未注册!");
            return map;
        }

        // 重置密码
        password = CommunityUtil.md5(password + user.getSalt());
        userMapper.updatePassword(user.getId(), password);

        map.put("user", user);
        return map;
    }

    // 修改密码
    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassword) {
        Map<String, Object> map = new HashMap<>();

        // 空值处理
        if (StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "原密码不能为空!");
            return map;
        }
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码不能为空!");
            return map;
        }

        // 验证原始密码
        User user = userMapper.selectById(userId);
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)) {
            map.put("oldPasswordMsg", "原密码输入有误!");
            return map;
        }

        // 更新密码
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        userMapper.updatePassword(userId, newPassword);

        return map;
    }


    // 根据用户名查找用户
    public User findUserByName(String Name) {
        return userMapper.selectByName(Name);
    }

    //1.优先从缓存中取值
    private User getCache(int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }
    //2.取不到时初始化缓存数据
    private User initCache(int userId){
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }
    //3.数据变更时清除缓存数据
    private void clearCache  (int userId){
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
    //查询某个用户的权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);
        List<GrantedAuthority> list = new ArrayList<>();
        list.add((GrantedAuthority) () -> {
            switch (user.getType()) {
                case 1:
                    return AUTHORITY_ADMIN;
                case 2:
                    return AUTHORITY_MODERATOR;
                default:
                    return AUTHORITY_USER;
            }
        });
        return list;
    }
}
