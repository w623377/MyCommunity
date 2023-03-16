package com.example.mycommunity.dao;

import com.example.mycommunity.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

//评论
@Mapper
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(int entityType,//评论的类型
                                         int entityId, //评论的id
                                         int offset, //分页的起始位置
                                         int limit);//每页的数量

    int selectCountByEntity(int entityType, int entityId);//查询该评论的数量

    int insertComment(Comment comment);

    Comment selectCommentById(int id);//根据id查询评论

}
