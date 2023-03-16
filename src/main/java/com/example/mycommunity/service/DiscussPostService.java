package com.example.mycommunity.service;


import com.example.mycommunity.dao.DiscussPostMapper;
import com.example.mycommunity.entity.DiscussPost;
import com.example.mycommunity.util.SensitiveFilter;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    @Autowired
    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private SensitiveFilter sensitiveFilter;
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;//最大缓存数

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;//过期时间

    //Caffeine核心接口：Cache,LoadingCache,AsyncLoadingCache

    //贴子列表缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;//

    //帖子总数缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    @PostConstruct
    public void init(){
        //帖子列表缓存
        postListCache= Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {//当缓存中没有数据时,自动从数据库中加载数据，然后放入缓存
                    @Override
                    public List<DiscussPost> load(String key) throws Exception {
                       if (key==null||key.length()==0){//key的意思是offset+limit
                            System.out.println("参数错误");
                           throw new IllegalArgumentException("参数错误");
                       }
                        String[] params = key.split(":");

                        if (params==null||params.length!=2){//如果参数不对，抛出异常，因为是offset+limit，所以参数应该是2个
                            System.out.println("参数错误");
                            throw new IllegalArgumentException("参数错误");
                        }

                        int offset=Integer.valueOf(params[0]);
                        int limit=Integer.valueOf(params[1]);

                        //也可以设置二级缓存：redis->mysql

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit,1);//查询热门帖子
                    }
                });

        //帖子总数缓存
        postRowsCache=Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds,TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public Integer load(Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }



    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit,int orderMode){
        if(userId==0&&orderMode==1){//如果是查询热门帖子,且用户id为1,则从缓存中取
            return postListCache.get(offset+":"+limit);
        }
        logger.debug("load post list from DB.");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit,orderMode);
    }

    public int findDiscussPostRows(int userId) {
        if (userId == 0) {//如果是查询自己的帖子,则从缓存中取
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows from DB.");
        return discussPostMapper.selectDiscussPostRows(userId);

    }

    public int addDiscussPost(DiscussPost post){
        if(post==null){
            throw new IllegalArgumentException("参数不能空");
        }
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id){//根据id查询帖子
        return  discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id,int commentCount){//更新评论数量,评论数量+1
        return discussPostMapper.updateCommentCount(id,commentCount);
    }

    //修改帖子类型
    public int updateType(int id,int type){
        return discussPostMapper.updateType(id,type);
    }

    //修改帖子状态
    public int updateStatus(int id,int status){
        return discussPostMapper.updateStatus(id,status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id,score);
    }
}
