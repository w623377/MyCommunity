package com.example.mycommunity.event;

import com.alibaba.fastjson.JSONObject;
import com.example.mycommunity.entity.DiscussPost;
import com.example.mycommunity.entity.Event;
import com.example.mycommunity.entity.Message;
import com.example.mycommunity.service.DiscussPostService;
import com.example.mycommunity.service.ElasticsearchService;
import com.example.mycommunity.service.MessageService;
import com.example.mycommunity.util.CommunityConstant;
import com.example.mycommunity.util.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

//消费者,处理事件,将传来成的事件进行json解析存入数据库
@Component
public class EventConsumer implements CommunityConstant {//消费者,处理事件,将传来成的事件进行json解析存入数据库
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;//线程池

    @Value("${wk.image.command}")
    private String wkImageCommand;//wk图片命令

    @Value("${wk.image.storage}")
    private String wkImageStorage;//wk图片存放的路径

    //处理评论事件,点赞事件,关注事件
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
            if(record==null||record.value()==null)
            {
                logger.error("消息内容为空");
                return;
            }

            Event event = JSONObject.parseObject(record.value().toString(), Event.class);//将json字符串转换为event对象

            if (event==null){
                logger.error("消息格式错误");
                return;
            }

            //发送站内通知
            Message message = new Message();//创建消息对象

            message.setFromId(SYSTEM_USER_ID);//系统用户
            message.setToId(event.getEntityUserId());//接收者
            message.setConversationId(event.getTopic());//主题
            message.setCreateTime(new Date());
            //没有设置status,因为默认为0

            Map<String, Object> content = new HashMap<>();//存储消息内容
            content.put("userId", event.getUserId());//触发者
            content.put("entityType", event.getEntityType());//触发的实体类型
            content.put("entityId", event.getEntityId());//触发的实体id
            if(!event.getData().isEmpty()){//触发的实体附带的数据
                for(Map.Entry<String,Object> entry:event.getData().entrySet()){//遍历map
                    content.put(entry.getKey(),entry.getValue());//将map中的数据存入content
                }
            }
            message.setContent(JSONObject.toJSONString(content));//将content转换为json字符串,存入message表的content中,
                                        // 会有转义字符，因为json字符串中有双引号，后面取的时候要注意，用jsonObject.getString()或者
                                        //jsonObjiect.parseObject()转换为map
            messageService.addMessage(message);
    }

    //消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublicMessage(ConsumerRecord record){//参数record是kafka传来的消息
        if(record==null||record.value()==null)
        {
            logger.error("消息内容为空");
            return;
        }
        //将发来的消息转换为event对象，利用fastjson将json字符串转换为event对象，因为kafka传来的消息是json字符串
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event==null){
            logger.error("消息格式错误");
            return;
        }
        //根据id从数据库查到帖子然后存入es服务器
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        elasticsearchService.saveDiscussPost(post);

    }

    //消费删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){//参数record是kafka传来的消息
        if(record==null||record.value()==null)
        {
            logger.error("消息内容为空");
            return;
        }
        //将发来的消息转换为event对象，利用fastjson将json字符串转换为event对象，因为kafka传来的消息是json字符串
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event==null){
            logger.error("消息格式错误");
            return;
        }
        //根据id从es服务器删除帖子
        elasticsearchService.deleteDiscussPost(event.getEntityId());

    }

    //消费分享事件
    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record) {//参数record是kafka传来的消息
        if (record == null || record.value() == null) {
            logger.error("消息内容为空");
            return;
        }
        //将发来的消息转换为event对象，利用fastjson将json字符串转换为event对象，因为kafka传来的消息是json字符串
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);

        if (event == null) {
            logger.error("消息格式错误");
            return;
        }
        //生成分享图片
        String htmlUrl = event.getData().get("htmlUrl").toString();//获取分享的url
        String fileName = event.getData().get("fileName").toString();//生成随机文件名
        String suffix = event.getData().get("suffix").toString();//生成的图片格式
        File file = new File(wkImageStorage+"/" + fileName + suffix);//生成图片的路径
        //执行命令
        String cmd = wkImageCommand + " --quality 75 " + htmlUrl + " " + file.getAbsolutePath();
        try {
            Runtime.getRuntime().exec(cmd);//执行命令
            logger.info("生成长图成功:" + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败:" + e.getMessage());
        }

        //启动定时器，检查图片是否生成成功，如果生成成功就将图片上传到七牛云服务器
        UploadTask task = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);//每隔5秒执行一次
        task.setFuture(future);//将返回值存入task中，用于取消任务
    }

    //上传图片任务
    class UploadTask implements Runnable {

        //文件名
        private String fileName;
        //文件后缀
        private String suffix;
        //启动任务的返回值，用于取消任务
        private Future future;

        //开始时间
        private long startTime;
        //上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        //上传图片
        @Override
        public void run() {
            //如果上传时间超过1分钟，就取消任务
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.error("执行时间过长，终止任务");
                future.cancel(true);
                return;
            }
            // 上传失败
            if (uploadTimes >= 3) {
                logger.error("上传次数过多,终止任务:" + fileName);
                future.cancel(true);
                return;
            }
            //上传图片
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                // 生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                //上传的机房
                UploadManager uploadManager = new UploadManager(new Configuration(Zone.zone2()));
                try {
                    // 开始上传图片
                    Response response = uploadManager.put(
                            path, fileName, uploadToken, null, "image/" + suffix.substring(suffix.lastIndexOf(".")+1), false);
                    // 处理响应结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    } else {
                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else {
                logger.info("等待图片生成[" + fileName + "].");
            }
        }
    }

}
