package com.example.mycommunity.util;

public class RedisKeyUtil {// 生成redis的key,方便管理
    private static final String SPLIT = ":";// 分隔符
    private static final String PREFIX_ENTITY_LIKE = "like:entity";// 点赞

    private static final String PREFIX_USER_LIKE = "like:user";// 用户点赞

    private static  final String PREFIX_FOLLOWEE = "followee";// 关注的实体

    private static  final String PREFIX_FOLLOWER = "follower";// 粉丝

    private static final String PREFIX_KAPTCHA = "kaptcha";// 验证码

    private static final String PREFIX_TICKET = "ticket";// 登录凭证

    private static final String PREFIX_USER="user";//用户

    private static final String PREFIX_UV="uv";//独立访客

    private static final String PREFIX_DAU="dau";//日活跃用户

    private static final String PREFIX_POST="post";//帖子分数

    // 某个实体的赞 例如:like:entity:entityType:entityId -> set(userId),存储点赞的用户,可以用来判断用户是否点赞,以及点赞的数量,可以用来排序
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    // 某个用户收到的赞 例如:like:user:userId -> int,存储用户收到的赞的数量
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    // 某个用户关注的实体
    // 例如:followee:userId:entityType -> zset(entityId,now),
    // 存储用户关注的实体,可以用来判断用户是否关注,以及关注的时间,可以用来排序
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    // 某个实体拥有的粉丝
    // 例如:follower:entityType:entityId -> zset(userId,now),now是关注的时间,可以用来排序
    // 存储实体拥有的粉丝,可以用来判断用户是否关注,以及关注的时间,可以用来排序
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    //登录验证码
    public static String getKaptchaKey(String owener){
        return PREFIX_KAPTCHA + SPLIT + owener;
    }

    //登录凭证
    public static String getTicketKey(String ticket){
        return PREFIX_TICKET + SPLIT + ticket;
    }

    //用户
    public static String getUserKey(int userId){
        return PREFIX_USER + SPLIT + userId;
    }

    //单日独立访客
    public static String getUVKey(String date){
        return PREFIX_UV + SPLIT + date;
    }

    //区间独立访客
    public static String getUVKey(String startDate,String endDate){
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    //单日活跃用户
    public static String getDAUKey(String date){
        return PREFIX_DAU + SPLIT + date;
    }

    //区间活跃用户
    public static String getDAUKey(String startDate,String endDate){
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    //帖子分数
    public static String getPostScoreKey(){
        return PREFIX_POST + SPLIT + "score";
    }

}
