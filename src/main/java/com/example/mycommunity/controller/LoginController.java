package com.example.mycommunity.controller;

import com.example.mycommunity.entity.User;
import com.example.mycommunity.service.UserService;
import com.example.mycommunity.util.CommunityConstant;
import com.example.mycommunity.util.CommunityUtil;
import com.example.mycommunity.util.RedisKeyUtil;
import com.google.code.kaptcha.Producer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;// 用于生成验证码

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage() {
        return "site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "site/login";
    }

    @RequestMapping(path = "/forget", method = RequestMethod.GET)
    public String getForgetPage() {
        return "site/forget";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user) {// 传入的是一个User对象, 里面包含了用户填写的表单信息
        Map<String, Object> map = userService.register(user);// 调用service层的方法, 返回一个map
        if (map == null || map.isEmpty()) {// 如果map为空, 说明注册成功
            model.addAttribute("msg", "注册成功,我们已经向您的邮箱发送了一封激活邮件,请尽快激活!");
            model.addAttribute("target", "/index");
            return "site/operate-result";
        } else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "site/register";
        }
    }



    // http://localhost:8080/community/activation/101/code
    // 101是userId, code是激活码
    /*
    * 激活账号
     */
    @RequestMapping(path = "/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activation(userId, code);
        System.out.println(result);
        if (result == ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "激活成功,您的账号已经可以正常使用了!");
            model.addAttribute("target", "/login");
        } else if (result == ACTIVATION_REPEAT) {
            model.addAttribute("msg", "无效操作,该账号已经激活过了!");
            model.addAttribute("target", "/index");
        } else {
            model.addAttribute("msg", "激活失败,您提供的激活码不正确!");
            model.addAttribute("target", "/index");
        }
        return "site/operate-result";
    }

    //生成验证码，返回给浏览器，浏览器再将验证码显示出来
    @RequestMapping(path = "/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {//验证码要存到服务器中，所以需要session，同时要返回给浏览器，所以需要response
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        // 将验证码存入session
        // session.setAttribute("kaptcha", text);

        // 将验证码的归属
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);//cookie的名字是kaptchaOwner，值是kaptchaOwner
        cookie.setMaxAge(60);//60秒，验证码只能使用一次
        cookie.setPath(contextPath);//设置cookie的作用范围
        response.addCookie(cookie);

        // 将验证码存入redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);

        // 将突图片输出给浏览器
        response.setContentType("image/png");//设置响应头，告诉浏览器返回的是图片
        try {
            OutputStream os = response.getOutputStream();//获取输出流
            ImageIO.write(image, "png", os);//将图片写入到输出流中
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());//打印错误信息
        }
    }

    @RequestMapping(path = "/login", method = RequestMethod.POST)
    public String login(String username,
                        String password,
                        String code,//验证码
                        boolean rememberme,//rememberme是记住我功能
                        Model model, //用于向前端传递数据
                        @CookieValue("kaptchaOwner") String kaptchaOwner,//从cookie中获取验证码的归属
//                        HttpSession session,//用于存储验证码
                        HttpServletResponse response) {//用于存储登录凭证
        // 检查验证码
        //String kaptcha = (String) session.getAttribute("kaptcha");//从session中获取验证码
        String kaptcha =null;//从redis中获取验证码,先设置为null

        if(StringUtils.isNoneBlank(kaptchaOwner)){
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);//从redis中获取验证码的key
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);//从redis中根据key获取验证码
        }

        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {//如果验证码为空或者用户输入的验证码为空或者用户输入的验证码不正确
            model.addAttribute("codeMsg", "验证码不正确!");//向前端传递数据
            return "site/login";
        }

        // 检查账号,密码
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);//调用userService的login方法
        if (map.containsKey("ticket")) {//如果map中包含ticket，说明登录成功
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());//创建一个cookie，名字为ticket，值为ticket
            cookie.setPath(contextPath);//设置cookie的路径，这里设置为项目的根路径，这样所有的页面都可以访问到这个cookie，从而实现登录凭证的共享，
                                        // 这样就不用每次都去数据库中查询了，提高了效率，同时也保证了安全性，因为cookie是加密的，只有服务器才能解密
            cookie.setMaxAge(expiredSeconds);//设置cookie的生存时间
            response.addCookie(cookie);//将cookie添加到response中
            return "redirect:index";
        } else {//如果map中不包含ticket，说明登录失败
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket) {//从cookie中获取ticket
        userService.logout(ticket);//退出登录，删除ticket
        SecurityContextHolder.clearContext();//清除安全认证上下文
        return "redirect:login";
    }

}
