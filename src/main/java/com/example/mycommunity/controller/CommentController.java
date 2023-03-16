package com.example.mycommunity.controller;

import com.example.mycommunity.entity.Comment;
import com.example.mycommunity.entity.DiscussPost;
import com.example.mycommunity.entity.Event;
import com.example.mycommunity.event.EventProducer;
import com.example.mycommunity.service.CommentService;
import com.example.mycommunity.service.DiscussPostService;
import com.example.mycommunity.util.CommunityConstant;
import com.example.mycommunity.util.HostHolder;
import com.example.mycommunity.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {
    @Autowired
    private CommentService commentService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    // 添加评论
    @RequestMapping(path = "/add/{discussPostId}",method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        // 创建触发评论事件
        Event event= new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostHolder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setData("postId",discussPostId);
        if (comment.getEntityType()==ENTITY_TYPE_POST){//如果是对帖子的评论
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());//找到帖子
            event.setEntityUserId(target.getUserId());//设置帖子的作者
        } else if (comment.getEntityType()==ENTITY_TYPE_COMMENT) {//如果是对评论的评论
            Comment target = commentService.findCommentById(comment.getEntityId());//找到评论
            event.setEntityUserId(target.getUserId());//设置评论的作者
        }

        eventProducer.fireEvent(event);//触发事件

        if(comment.getEntityType()==ENTITY_TYPE_POST){//如果是对帖子的评论
            // 触发发帖事件
            event=new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(hostHolder.getUser().getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);//触发事件

            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }

        return "redirect:/discuss/detail/"+discussPostId;
    }
}
