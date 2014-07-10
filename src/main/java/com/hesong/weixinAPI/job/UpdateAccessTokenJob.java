package com.hesong.weixinAPI.job;

import net.sf.json.JSONObject;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;

import redis.clients.jedis.Jedis;

import com.hesong.weixinAPI.context.ContextPreloader;
import com.hesong.weixinAPI.model.AccessToken;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;


@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class UpdateAccessTokenJob implements Job {

    public static final String JOB_GROUP = "UpdateAccessToken";

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        Jedis jedis = ContextPreloader.jedisPool.getResource();

        for(String key: ContextPreloader.Account_Map.keySet()){
            AccessToken ac = ContextPreloader.Account_Map.get(key);
            ac = WeChatHttpsUtil.getAccessToken(ac);
            if (ac == null) {
                ContextPreloader.ContextLog.error("Get Access_Token failed for account: "+key);
                continue;
            }
            ContextPreloader.ContextLog.info("Access token updated for account: "+ac.toString());

            ContextPreloader.Account_Map.put(key, ac);
            
            if (jedis.hexists(API.REDIS_CLIENT_ACCOUNT_INFO_KEY, key)) {
                JSONObject jo = JSONObject.fromObject(jedis.hget(API.REDIS_CLIENT_ACCOUNT_INFO_KEY, key));
                jo.put("access_token", ac.getToken());
                jedis.hset(API.REDIS_CLIENT_ACCOUNT_INFO_KEY, key, jo.toString());
                jedis.hset(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, key, ac.getToken());
                ContextPreloader.ContextLog.info("Client " + key + " access_token updated: " + jedis.hget(API.REDIS_CLIENT_ACCOUNT_INFO_KEY, key));
            }
            if (jedis.hexists(API.REDIS_STAFF_ACCOUNT_INFO_KEY, key)) {
                JSONObject jo = JSONObject.fromObject(jedis.hget(API.REDIS_STAFF_ACCOUNT_INFO_KEY, key));
                jo.put("access_token", ac.getToken());
                jedis.hset(API.REDIS_STAFF_ACCOUNT_INFO_KEY, key, jo.toString());
                jedis.hset(API.REDIS_WEIXIN_ACCESS_TOKEN_KEY, key, ac.getToken());
                ContextPreloader.ContextLog.info("Staff " + key + " access_token updated: " + jedis.hget(API.REDIS_STAFF_ACCOUNT_INFO_KEY, key));
            }
        }
        ContextPreloader.jedisPool.returnBrokenResource(jedis);
        
    }
}