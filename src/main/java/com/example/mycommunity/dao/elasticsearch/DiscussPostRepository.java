package com.example.mycommunity.dao.elasticsearch;

import com.example.mycommunity.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
//这个接口是用来操作es的，继承ElasticsearchRepository
@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost,Integer>//第一个参数是实体类，第二个参数是主键类型
{}
