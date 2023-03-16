package com.example.mycommunity.service;

import com.example.mycommunity.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat df =new SimpleDateFormat("yyyy-MM-dd");

    // 将指定的IP计入UV
    public void recordUV(String ip) {
        String redisKey = RedisKeyUtil.getUVKey(df.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey, ip);//key是登录的日期，value是登录的ip，相同的ip只会计算一次
    }

    //统计日期范围内的uv
    public long calculateUV(Date start,Date end){
        if(start==null||end==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if(start.after(end)){
            throw new IllegalArgumentException("开始时间不能大于结束时间");
        }
        //整理范围内的key
        List<String> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while (!calendar.getTime().after(end)) {
            String key = RedisKeyUtil.getUVKey(df.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE, 1);
        }

        //合并这些数据,
        String redisKey= RedisKeyUtil.getUVKey(df.format(start),df.format(end));//获取合并后的key
        redisTemplate.opsForHyperLogLog().union(redisKey,keyList.toArray());//将keyList中的key合并到redisKey中
        //返回统计的结果
        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    // 将指定用户计入DAU
    public void recordDAU(int userId) {
        String redisKey = RedisKeyUtil.getDAUKey(df.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey, userId, true);//key是登录的日期，value是登录的用户id的位信息多对一,相同的用户只会计算一次
    }

    //统计日期范围内的DAU
    public long calculateDAU(Date start,Date end){
        if(start==null||end==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if(start.after(end)){
            throw new IllegalArgumentException("开始时间不能大于结束时间");
        }
        //整理范围内的key
        List<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);//设置为起始时间
        while(!calendar.getTime().after(end)){
            String key = RedisKeyUtil.getDAUKey(df.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(Calendar.DATE,1);//日期加一天
        }

        // 进行OR运算
        return (long) redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey = RedisKeyUtil.getDAUKey(df.format(start), df.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(), keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });
    }

}
