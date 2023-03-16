package com.example.mycommunity.controller;
import com.example.mycommunity.annotation.LoginRequired;
import com.example.mycommunity.entity.User;
import com.example.mycommunity.service.FollowService;
import com.example.mycommunity.service.LikeService;
import com.example.mycommunity.service.UserService;
import com.example.mycommunity.util.CommunityConstant;
import com.example.mycommunity.util.CommunityUtil;
import com.example.mycommunity.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")//
    private String uploadPath;// 上传文件的路径

    @Value("${community.path.domain}")
    private String domain;//    项目的域名

    @Value("${server.servlet.context-path}")
    private String contextPath;// 项目的路径

    @Autowired
    private UserService userService;// 注入service层

    @Autowired
    private HostHolder hostHolder;// 注入当前用户

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;



    @LoginRequired// 自定义注解配合拦截器使用，判断用户是否登录，未登录则跳转到登录页面
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model) {
        //上传文件名称
        String fileName=CommunityUtil.generateUUID();//生成随机的文件名
        //设置响应信息
        StringMap policy=new StringMap();
        policy.put("returnBody",CommunityUtil.getJSONString(0));
        //生成上传的凭证
        Auth auth = Auth.create(accessKey, secretKey);
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName",fileName);

        return "site/setting";
    }

    //更新头像路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }

        String url = headerBucketUrl + "/" + fileName;// 七牛云的路径
        userService.updateHeader(hostHolder.getUser().getId(), url);
        return CommunityUtil.getJSONString(0);
    }

    //废弃
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {// 上传头像
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片!");
            return "site/setting";
        }

        String fileName = headerImage.getOriginalFilename();// 获取文件名
        String suffix = fileName.substring(fileName.lastIndexOf("."));// 获取文件后缀
        if (StringUtils.isBlank(suffix)) {// 判断文件后缀是否为空
            model.addAttribute("error", "文件的格式不正确!");
            return "site/setting";
        }

        // 生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        // 确定文件存放的路径
        File dest = new File(uploadPath + "/" + fileName);
        try {
            // 存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败: " + e.getMessage());
            throw new RuntimeException("上传文件失败,服务器发生异常!", e);
        }

        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);

        return "redirect:index";
    }

    //废弃
    // 获取头像，因为是响应图片，前端不需要处理，直接显示，所以返回值是空
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);// 设置响应的类型，固定
        try (
                FileInputStream fis = new FileInputStream(fileName);// 读取文件，会自动关闭输入流，不用手动关闭，jdk1.7新特性
                OutputStream os = response.getOutputStream();// 响应文件，用response的输出流来给浏览器响应文件
        ) {
            byte[] buffer = new byte[1024];// 缓冲区，每次读取1kb
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {// 读取文件，
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }


    // 修改密码
    @RequestMapping(path = "/updatePassword", method = RequestMethod.POST)
    public String updatePassword(String oldPassword, String newPassword, Model model) {
        User user = hostHolder.getUser();
        Map<String, Object> map = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if (map == null || map.isEmpty()) {
            return "redirect:logout";
        } else {
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            return "site/setting";
        }
    }

    //个人主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");// 抛出异常，交给异常处理器处理
        }
        // 用户
        model.addAttribute("user", user);

        //点赞数量
        int likeCount = likeService.findUserLikeCoount(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        //是否已关注
        boolean hasFollowed = false;//默认没有关注
        if(hostHolder.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);


        return "site/profile";
    }

    @RequestMapping(path = "/profile/mypost/{userId}", method = RequestMethod.GET)
    public String getPostPage(@PathVariable("userId") int userId, Model model) {
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new RuntimeException("该用户不存在!");// 抛出异常，交给异常处理器处理
        }
        // 用户
        model.addAttribute("user", user);

        //点赞数量
        int likeCount = likeService.findUserLikeCoount(userId);
        model.addAttribute("likeCount", likeCount);

        //关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);

        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);

        //是否已关注
        boolean hasFollowed = false;//默认没有关注
        if(hostHolder.getUser()!=null){
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);


        return "site/my-post";
    }

}
