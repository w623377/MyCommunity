package com.example.mycommunity.controller;

import com.example.mycommunity.entity.DiscussPost;
import com.example.mycommunity.entity.Page;
import com.example.mycommunity.service.ElasticsearchService;
import com.example.mycommunity.service.LikeService;
import com.example.mycommunity.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.mycommunity.util.CommunityConstant.ENTITY_TYPE_POST;

@Controller
public class SearchController {
    @Autowired
    private ElasticsearchService elasticsearchService;//搜索服务

    @Autowired
    private UserService userService;//显示用户

    @Autowired
    private LikeService likeService;//显示点赞数

    //search?keyword=xxx
    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model){
        //搜索帖子
        org.springframework.data.domain.Page<DiscussPost> searchResult =
                elasticsearchService.searchDiscussPost(keyword, page.getCurrent() - 1, page.getLimit());//page.getCurrent()-1是因为page是从1开始的，而es是从0开始的
        //聚合数据
        List<Map<String,Object>> discussPosts = new ArrayList<>();
        if(searchResult != null){
            for(DiscussPost post : searchResult){
                Map<String,Object> map = new HashMap<>();
                //帖子
                map.put("post",post);
                //作者
                map.put("user",userService.findUserById(post.getUserId()));
                //点赞数
                map.put("likeCount",likeService.findEntityLikeCount(ENTITY_TYPE_POST,post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword);

        //分页信息
        page.setPath("/search?keyword="+keyword);//分页的路径
        page.setRows(searchResult == null ? 0 : (int)searchResult.getTotalElements());//总记录数

        return "site/search";
    }
}
