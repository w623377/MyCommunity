package com.example.mycommunity.service;

import com.example.mycommunity.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    @Autowired
    private RedisTemplate redisTemplate;

    // 点赞
    public void like(int userId,// 点赞的用户id
                     int entityType,// 点赞的实体类型,比如帖子,评论
                     int entityId,// 点赞的实体id，比如帖子id,评论id
                     int entityUserId){// 被点赞的实体的作者id
//        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);// 生成redis的key
//        Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);// 判断用户是否点赞
//        if (isMember) {// 如果点赞了,就取消点赞
//            redisTemplate.opsForSet().remove(entityLikeKey, userId);
//        } else {// 如果没有点赞,就点赞
//            redisTemplate.opsForSet().add(entityLikeKey, userId);
//        }
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);//贴子或评论的key
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);// 被点赞的用户的key
                boolean isMember = operations.opsForSet().isMember(entityLikeKey, userId);// 判断用户是否点赞

                operations.multi();// 开启事务

                if (isMember) {// 如果点赞了,就取消点赞
                    operations.opsForSet().remove(entityLikeKey, userId);
                    operations.opsForValue().decrement(userLikeKey);// 被点赞的用户的点赞数量减一
                } else {// 如果没有点赞,就点赞
                    operations.opsForSet().add(entityLikeKey, userId);
                    operations.opsForValue().increment(userLikeKey);// 被点赞的用户的点赞数量加一
                }
                return operations.exec();// 提交事务
            }
        });
    }

    // 查询某实体点赞的数量
    public long findEntityLikeCount(int entityType,int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);// 生成redis的key
        return redisTemplate.opsForSet().size(entityLikeKey);// 返回点赞的数量
    }

    // 查询某人对某实体的点赞状态
    public int findEntityLikeStatus(int userId,int entityType,int entityId){
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);// 生成redis的key
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;// 判断用key里面是否有userId,有就是已经点赞了
    }

    // 查询某个用户获得的赞
    public int findUserLikeCoount(int userId){

        String userLikeKey=RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer) redisTemplate.opsForValue().get(userLikeKey);
        return count==null?0:count.intValue();
    }


}
