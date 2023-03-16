package com.example.mycommunity.controller;

import com.example.mycommunity.entity.Event;
import com.example.mycommunity.event.EventProducer;
import com.example.mycommunity.util.CommunityConstant;
import com.example.mycommunity.util.CommunityUtil;
import org.apache.tomcat.util.http.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController implements CommunityConstant {
    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Autowired
    private EventProducer eventProducer;//事件生产者

    @Value("${community.path.domain}")
    private String domain;//域名

    @Value("${server.servlet.context-path}")
    private String contextPath;//项目名

    @Value("${wk.image.storage}")
    private String wkImageStorage;//wk图片存放的路径

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.url}")
    private String shareBucketUrl;

    @RequestMapping(path = "/share",method = RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl){
        //文件名
        String fileName = CommunityUtil.generateUUID();

        //异步生成长图
        Event event=new Event()
                .setTopic(TOPIC_SHARE)
                .setData("htmlUrl",htmlUrl)
                .setData("fileName",fileName)
                .setData("suffix",".png");

        eventProducer.fireEvent(event);

        //返回访问图片的路径
        Map<String,Object> map=new HashMap<>();
//        map.put("shareUrl",domain+contextPath+"/share/image/"+fileName);
        map.put("shareUrl",shareBucketUrl+"/"+fileName);
        return CommunityUtil.getJSONString(0,"访问该图片链接:",map);
    }
    //废弃，改为访问七牛云
    //访问图片
    @RequestMapping(path = "/share/image{fileName}",method = RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response){
        if (fileName==null){
            throw new IllegalArgumentException("文件名不能为空");
        }
        //服务器上的文件
        File file=new File(wkImageStorage+"/"+fileName+".png");
        try {
            OutputStream os =response.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer=new byte[1024];
            int b=0;
            while ((b=fis.read(buffer))!=-1){//
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("获取长图失败:"+e.getMessage());
        }
    }

    //每隔4分钟清理一次wk图片
    @Scheduled(cron = "0 0/4 * * * *")//每隔4分钟
    public void shareImageClear(){
        File file=new File(wkImageStorage);
        if (file.exists()&&file.listFiles()!=null){
            File[] files=file.listFiles();//获取所有文件
            for (File f:files){//遍历所有文件
                if (System.currentTimeMillis()-f.lastModified()>1000*6){//如果文件的最后修改时间距离现在超过一天
                    f.delete();
                    logger.info("清理wk图片:"+f.getName());
                }
            }
        }
    }
}
