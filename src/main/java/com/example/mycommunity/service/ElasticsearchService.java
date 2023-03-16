package com.example.mycommunity.service;

import com.alibaba.fastjson.JSONObject;
import com.example.mycommunity.dao.elasticsearch.DiscussPostRepository;
import com.example.mycommunity.entity.DiscussPost;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticsearchService {// 用于将数据存入Elasticsearch, 以便于搜索, 但是这里只是简单的存入, 没有进行分词等操作
    @Autowired
    private DiscussPostRepository discussPostRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    // 将数据存入Elasticsearch
    public void saveDiscussPost(DiscussPost discussPost){
        discussPostRepository.save(discussPost);
    }

    // 将数据从Elasticsearch中删除
    public void deleteDiscussPost(int id){
        discussPostRepository.deleteById(id);
    }

    // 从Elasticsearch中搜索数据
    public Page<DiscussPost> searchDiscussPost(String keyword,int current,int limit){
        //1.构建查询条件
        SearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(QueryBuilders.multiMatchQuery(keyword,"title","content"))//搜索的内容
                .withSort(SortBuilders.fieldSort("type").order(SortOrder.DESC))//排序
                .withSort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .withSort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .withPageable(org.springframework.data.domain.PageRequest.of(current,limit))//分页
                .withHighlightFields(//高亮
                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
                ).build();

        //7.x版本是可以用SearchTemplate的search方法返回SearchHits,但是6.x版本不行,所以要用queryForPage方法
        //然后用SearchTemplate的queryForPage方法,返回的是一个聚合的分页对象


        //2.执行搜索.返回的是一个聚合的分页对象
        Page<DiscussPost> page = elasticsearchTemplate.queryForPage(searchQuery, DiscussPost.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                SearchHits hits = searchResponse.getHits();//获取命中的结果
                if (hits.getTotalHits() <= 0){
                    return null;
                }
                //解析结果
                List<DiscussPost> list = new ArrayList<>();

                for(SearchHit hit:hits){//解析高亮的结果

                    //将json字符串转换为DiscussPost对象
                    DiscussPost post = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);

                    //处理高亮显示的结果
                    HighlightField titleField = hit.getHighlightFields().get("title");
                    if (titleField != null){
                        post.setTitle(titleField.getFragments()[0].toString());
                    }
                    HighlightField contentField = hit.getHighlightFields().get("content");
                    if (contentField != null){
                        post.setContent(contentField.getFragments()[0].toString());
                    }
                    list.add(post);
                }
                return new AggregatedPageImpl(list,pageable,hits.getTotalHits(),searchResponse.getScrollId(),hits.getMaxScore());
            }
        });
        System.out.println(page);
        return page;
    }

}
