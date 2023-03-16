package com.example.mycommunity.quartz;

import com.example.mycommunity.entity.DiscussPost;
import com.example.mycommunity.service.DiscussPostService;
import com.example.mycommunity.service.ElasticsearchService;
import com.example.mycommunity.service.LikeService;
import com.example.mycommunity.util.CommunityConstant;
import com.example.mycommunity.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/***
 * @Description: 刷新帖子分数任务，
 *  根据帖子的分数计算权重，要用到likeService, discussPostService, redisTemplate, elasticsearchService
 */

@Component
public class PostScoreRefreshJob implements Job, CommunityConstant {
    //实例化logger
    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private static final Date epoch;//牛客纪元

    //静态代码块，初始化牛客纪元
    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2014-08-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化牛客纪元失败!", e);
        }
    }

   //执行任务
    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey= RedisKeyUtil.getPostScoreKey();
        //绑定redisKey, 用于操作redis中的set, 用于存储帖子id, 用于计算帖子分数, 用于排序
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if(operations.size()==0){
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        logger.info("[任务开始] 正在刷新帖子分数: "+"数量:"+operations.size());
        while (operations.size()>0){
            //取出一定数量的帖子id
            this.refresh((Integer) operations.pop());
        }
        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    //计算帖子分数
    private void refresh(int id) {
        DiscussPost post = discussPostService.findDiscussPostById(id);
        if(post==null){
            logger.error("该帖子不存在:id="+id);
            return;
        }
        //是否精华
        boolean wonderful = post.getStatus()==1;
        //评论数量
        int commentCount = post.getCommentCount();
        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, id);

        //计算权重
        double w = (wonderful?75:0)+commentCount*10+likeCount*2;
        //分数=权重+距离天数
        double score = Math.log10(Math.max(w, 1))+(post.getCreateTime().getTime()-epoch.getTime())/(1000*3600*24);

        //更新帖子分数
        discussPostService.updateScore(id, score);
        //同步搜索数据
        post.setScore(score);
        elasticsearchService.saveDiscussPost(post);
    }
}
