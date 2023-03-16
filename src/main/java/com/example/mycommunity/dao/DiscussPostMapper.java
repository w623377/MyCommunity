package com.example.mycommunity.dao;

import com.example.mycommunity.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
//帖子
@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit,int orderMode);//查询帖子,userId为0则查询所有帖子,否则查询某个用户的帖子

    // @Param注解用于给参数取别名,
    // 如果只有一个参数,并且在<if>里使用,则必须加别名.
    int selectDiscussPostRows(@Param("userId") int userId);//查询帖子的数量
    int insertDiscussPost(DiscussPost discussPost);//插入帖子

    DiscussPost selectDiscussPostById(int id);//根据id查询帖子

    int updateCommentCount(int id,int commentCount);//更新评论数量

    //修改帖子类型
    int updateType(int id,int type);//修改帖子类型

    //修改帖子状态
    int updateStatus(int id,int status);//修改帖子状态

    int updateScore(int id, double score);//修改帖子分数

}
