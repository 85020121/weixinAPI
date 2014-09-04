package com.hesong.weixinAPI.tools;

import redis.clients.jedis.Jedis;

import com.hesong.weixinAPI.context.ContextPreloader;

public class RedisOperations {
    
    public static int getWaitingListCount(String field) {
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        if (jedis.hexists(API.REDIS_WAITING_LIST_COUNT, field)) {
            int count = Integer.parseInt(jedis.hget(API.REDIS_WAITING_LIST_COUNT, field));
            ContextPreloader.jedisPool.returnResource(jedis);
            return count;
        } else {
            ContextPreloader.jedisPool.returnResource(jedis);
            return 0;
        }
    }
    
    public static void incrWaitingListCount(String field) {
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        jedis.hincrBy(API.REDIS_WAITING_LIST_COUNT, field, 1);
        ContextPreloader.jedisPool.returnResource(jedis);
    }
    
    public static void decrWaitingListCount(String field) {
        Jedis jedis = ContextPreloader.jedisPool.getResource();
        jedis.hincrBy(API.REDIS_WAITING_LIST_COUNT, field, -1);
        ContextPreloader.jedisPool.returnResource(jedis);
    }
}
