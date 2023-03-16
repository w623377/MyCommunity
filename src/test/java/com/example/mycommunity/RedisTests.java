//package com.example.mycommunity;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.dao.DataAccessException;
//import org.springframework.data.redis.connection.RedisConnection;
//import org.springframework.data.redis.connection.RedisStringCommands;
//import org.springframework.data.redis.core.*;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import java.util.concurrent.TimeUnit;
//
//@RunWith(SpringRunner.class)
//@SpringBootTest
//@ContextConfiguration(classes = MyCommunityApplication.class)
//public class RedisTests {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//    @Test
//    public void testSting(){
//        String redisKey= "test:count";
//        redisTemplate.opsForValue().set(redisKey,1);//存入数据
//        System.out.println(redisTemplate.opsForValue().get(redisKey));//取出数据
//        System.out.println(redisTemplate.opsForValue().increment(redisKey));//自增
//        System.out.println(redisTemplate.opsForValue().decrement(redisKey));//自减
//    }
//
//    @Test
//    public void testHashes(){
//         String redisKey= "test:user";
//         redisTemplate.opsForHash().put(redisKey,"id",1);
//         redisTemplate.opsForHash().put(redisKey,"username","zhangsan");
//         System.out.println(redisTemplate.opsForHash().get(redisKey,"id"));
//         System.out.println(redisTemplate.opsForHash().get(redisKey,"username"));
//    }
//    @Test
//    public void testList(){
//        String redisKey= "test:ids";
//        redisTemplate.opsForList().leftPush(redisKey,101);
//        redisTemplate.opsForList().leftPush(redisKey,102);
//        redisTemplate.opsForList().leftPush(redisKey,103);
//        System.out.println(redisTemplate.opsForList().size(redisKey));
//        System.out.println(redisTemplate.opsForList().index(redisKey,0));
//        System.out.println(redisTemplate.opsForList().range(redisKey,0,2));
//        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
//        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
//        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
//    }
//    @Test
//    public void testSet(){
//        String redisKey= "test:teachers";
//        redisTemplate.opsForSet().add(redisKey,"张三","李四","王五","赵六","田七");
//        System.out.println(redisTemplate.opsForSet().size(redisKey));
//        System.out.println(redisTemplate.opsForSet().pop(redisKey));
//        System.out.println(redisTemplate.opsForSet().members(redisKey));
//    }
//    @Test
//    public void testSortedSet(){
//        String redisKey= "test:students";
//        redisTemplate.opsForZSet().add(redisKey,"张三",80);
//        redisTemplate.opsForZSet().add(redisKey,"李四",90);
//        redisTemplate.opsForZSet().add(redisKey,"王五",100);
//        redisTemplate.opsForZSet().add(redisKey,"赵六",70);
//        redisTemplate.opsForZSet().add(redisKey,"田七",60);
//        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
//        System.out.println(redisTemplate.opsForZSet().score(redisKey,"张三"));
//        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey,"张三"));//倒序
//        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey,0,2));//倒序
//    }
//
//    @Test
//    public void testKeys(){
//        redisTemplate.delete("test:user");
//        System.out.println(redisTemplate.hasKey("test:user"));
//        redisTemplate.expire("test:students",10, TimeUnit.SECONDS);
//    }
//
//    //多次访问同一个key
//    @Test
//    public void testBoundOperations(){
//        String redisKey="test:count";
//        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);//绑定,不用每次都写key
//        operations.increment();
//        operations.increment();
//        operations.increment();
//        operations.increment();
//        operations.increment();
//        System.out.println(operations.get());
//    }
//
//    //编程式事务
//    @Test
//    public void testTransactional(){
//        Object obj = redisTemplate.execute(new SessionCallback() {
//            @Override
//            public  Object execute(RedisOperations operations) throws DataAccessException {
//                String redisKey="test:tx";
//                operations.multi();//开启事务
//                operations.opsForSet().add(redisKey,"张三");
//                operations.opsForSet().add(redisKey,"李四");
//                operations.opsForSet().add(redisKey,"王五");
//                System.out.println(operations.opsForSet().members(redisKey));//事务未提交前，查询不到数据
//                return operations.exec();
//            }
//        });
//        System.out.println(obj);
//    }
//
//    //统计20万个重复数据的独立总数
//    @Test
//    public void testHyperLogLog(){
//        String redisKey="test:hll:01";
//
//        for (int i = 1; i <100000 ; i++) {
//                redisTemplate.opsForHyperLogLog().add(redisKey,i);
//            }
//            for (int i = 1; i <100000 ; i++) {
//                int r=(int)(Math.random()*100000+1);
//                redisTemplate.opsForHyperLogLog().add(redisKey,r);
//            }
//            System.out.println(redisTemplate.opsForHyperLogLog().size(redisKey));
//        }
//
//    //将3组数据合并，再统计合并后的重复数据的独立总数
//    @Test
//    public void testHyperLogLogUnion(){
//        String redisKey2="test:hll:02";
//        for (int i = 1; i <10000 ; i++) {
//            redisTemplate.opsForHyperLogLog().add(redisKey2,i);
//        }
//
//        String redisKey3="test:hll:03";
//        for (int i = 5001; i <20000 ; i++) {
//            redisTemplate.opsForHyperLogLog().add(redisKey3,i);
//        }
//
//        String redisKey4="test:hll:04";
//        for (int i = 10001; i <30000 ; i++) {
//            redisTemplate.opsForHyperLogLog().add(redisKey4,i);
//        }
//
//        String unionKey="test:hll:union";
//        redisTemplate.opsForHyperLogLog().union(unionKey,redisKey2,redisKey3,redisKey4);
//        System.out.println(redisTemplate.opsForHyperLogLog().size(unionKey));
//    }
//
//    //统计一组数据的布尔值
//    @Test
//    public void testBitMap(){
//        String redisKey="test:bm:01";
//        //记录三个用户的签到情况
//        //0表示没签到，1表示签到
//        redisTemplate.opsForValue().setBit(redisKey,1,true);
//        redisTemplate.opsForValue().setBit(redisKey,3,true);
//        redisTemplate.opsForValue().setBit(redisKey,6,true);
//
//        //统计一共签到了多少人
//        Object obj = redisTemplate.execute(new RedisCallback() {
//            @Override
//            public Object doInRedis(RedisConnection connection) throws DataAccessException {
//                //获取一个Redis的连接
//                String redisKey="test:bm:01";
//                return connection.bitCount(redisKey.getBytes());
//            }
//        });
//        System.out.println(obj);
//    }
//
//    //统计三组数据的布尔值，并对这三组数据做OR运算
//    @Test
//    public void testBitMapOperration(){
//        String redisKey2="test:bm:02";
//        //记录三个用户的签到情况
//        //0表示没签到，1表示签到
//        redisTemplate.opsForValue().setBit(redisKey2,0,true);
//        redisTemplate.opsForValue().setBit(redisKey2,1,true);
//        redisTemplate.opsForValue().setBit(redisKey2,2,true);
//
//        String redisKey3="test:bm:03";
//        //记录三个用户的签到情况
//        //0表示没签到，1表示签到
//        redisTemplate.opsForValue().setBit(redisKey3,2,true);
//        redisTemplate.opsForValue().setBit(redisKey3,3,true);
//        redisTemplate.opsForValue().setBit(redisKey3,4,true);
//
//
//        String redisKey4="test:bm:04";
//        //记录三个用户的签到情况
//        //0表示没签到，1表示签到
//        redisTemplate.opsForValue().setBit(redisKey4,4,true);
//        redisTemplate.opsForValue().setBit(redisKey4,5,true);
//        redisTemplate.opsForValue().setBit(redisKey4,6,true);
//
//
//        String redisKey5="test:bm:or";
//        Object obj = redisTemplate.execute((RedisCallback<Object>) redisConnection -> {
//            redisConnection.bitOp(RedisStringCommands.BitOperation.OR,
//                    redisKey5.getBytes(),
//                    redisKey2.getBytes(),
//                    redisKey3.getBytes(),
//                    redisKey4.getBytes());
//            return redisConnection.bitCount(redisKey5.getBytes());
//        });
//        System.out.println(obj);
//
//
//    }
//
//}
