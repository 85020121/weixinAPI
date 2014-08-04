package com.hesong.weixinAPI.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.hesong.weixinAPI.core.MessageExecutor;
import com.hesong.weixinAPI.core.MessageRouter;
import com.hesong.weixinAPI.tools.API;
import com.hesong.weixinAPI.tools.WeChatHttpsUtil;

@Controller
@RequestMapping("/broadcast")
public class BroadcastController {
    private static Logger log = Logger.getLogger(BroadcastController.class);

    private static String SEND_MESSAGE_REQUEST_URL = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=";
    private static String UPLOAD_NEWS_URL = "https://api.weixin.qq.com/cgi-bin/media/uploadnews?access_token=";
    private static String SEND_ALL_URL = "https://api.weixin.qq.com/cgi-bin/message/mass/sendall?access_token=";

    private static String PROSPECTS_CLIENT = "SELECT a.id,b.weixinid_c,a.last_name,a.lead_id FROM sugarcrm.prospects a,sugarcrm.prospects_cstm b where a.id=b.id_c and a.lead_id is null and a.deleted=0";
    private static String LEADS_CLIENT = "SELECT a.id,a.last_name,b.weixinid_c FROM sugarcrm.leads a,sugarcrm.leads_cstm b where a.id=b.id_c and a.converted=0 and a.deleted=0";
    private static String CONTACTS_CLIENT = "SELECT a.id,a.last_name,b.weixinid_c FROM sugarcrm.contacts a ,sugarcrm.contacts_cstm b where a.id=b.id_c and a.deleted=0";


    @ResponseBody
    @RequestMapping(value = "/{account}/sendToAllUsers", method = RequestMethod.POST)
    public JSONObject sendToAllUsers(@PathVariable String account,
            HttpServletRequest request) {
        String access_token = MessageRouter.getAccessToken(account);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject message = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            log.info("Message: " + message.toString());

            String msg_type = message.getString("msgtype");
            
            if (msg_type.equals("news")) {
                JSONObject news = message.getJSONObject("news");
                JSONArray articles = news.getJSONArray("articles");
                JSONObject uploadNews = uploadNews(articles, access_token);
                log.info("uploadNews: " + uploadNews.toString());
                if (uploadNews.containsKey("media_id")) {
                    JSONObject postMpNews = new JSONObject();
                    JSONObject group_id = new JSONObject();
                    group_id.put("group_id", "0");
                    JSONObject media_id = new JSONObject();
                    group_id.put("media_id", uploadNews.getString("media_id"));
                    postMpNews.put("filter", group_id);
                    postMpNews.put("mpnews", media_id);
                    postMpNews.put("msgtype", "mpnews");
                    return WeChatHttpsUtil.httpsRequest(SEND_ALL_URL
                            + access_token, "POST", postMpNews.toString());
                }
            }

            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error(e.toString());
        }
        return null;
    }
    
    @ResponseBody
    @RequestMapping(value = "/{account}/sendToGroup", method = RequestMethod.POST)
    public String sendToGroup(@PathVariable String account,
            HttpServletRequest request) {
        String access_token = MessageRouter.getAccessToken(account);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject message = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            log.info("Message: " + message.toString());
            
           int groupId = message.getInt("touser");
            String sql = "";
            if (-1 == groupId) {
                sql = PROSPECTS_CLIENT;
            } else if (0 == groupId) {
                sql =LEADS_CLIENT;
            } else if (1 == groupId) {
                sql = CONTACTS_CLIENT;
            } else {
                log.error("Invalide client group ID: " + groupId);
                return "Invalide client group ID";
            }
            message.put("sql", sql);
            message.put("access_token", access_token);
            MessageExecutor.groupMessagesQueue.put(message);
            return "Sending to group users";
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error(e.toString());
            return e.toString();
        }
    }

    @ResponseBody
    @RequestMapping(value = "/{account}/sendToActiveUsers", method = RequestMethod.POST)
    public String sendToActiveUsers(@PathVariable String account,
            HttpServletRequest request) {
        String access_token = MessageRouter.getAccessToken(account);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject message = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            message.put("access_token", access_token);
            MessageExecutor.activeMessagesQueue.put(message);
            return "Sending to all actives users.";
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error(e.toString());
            return e.toString();
        }
    }

    @ResponseBody
    @RequestMapping(value = "/{account}/sendToWeixinGroup", method = RequestMethod.POST)
    public int sendToWeixinGroup(@PathVariable String account,
            HttpServletRequest request) {
        String access_token = MessageRouter.getAccessToken(account);
        ObjectMapper mapper = new ObjectMapper();
        try {
            JSONObject message = (JSONObject) JSONSerializer.toJSON(mapper
                    .readValue(request.getInputStream(), Map.class));
            log.info("Message: " + message.toString());
            JSONArray openIDs = message.getJSONArray("touser");
            int counter = openIDs.size();
            for (Object openID : openIDs) {
                message.put("touser", openID);
                sendMessage(message.toString(), access_token);
            }
            return counter;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log.error(e.toString());
        }
        return 0;
    }

    private JSONObject uploadNews(JSONArray articles, String token) {
        JSONArray mparticles = new JSONArray();
        for (int i = 0; i < articles.size(); i++) {
            JSONObject article = articles.getJSONObject(i);
            String pic_url = article.getString("picurl");
            InputStream input = WeChatHttpsUtil.httpGetInputStream(pic_url,
                    "image/*");
            String post_url = API.UPLOAD_IMAGE_REQUEST_URL.replace(
                    "ACCESS_TOKEN", token) + "thumb";
            JSONObject ret = WeChatHttpsUtil.httpPostFile(post_url, input, UUID
                    .randomUUID().toString() + ".jpg");
            log.info("Upload media: " + ret.toString());
            if (ret.containsKey("thumb_media_id")) {
                JSONObject mparticle = new JSONObject();
                mparticle
                        .put("thumb_media_id", ret.getString("thumb_media_id"));
                mparticle.put("tile", article.getString("title"));
                mparticle.put("content_source_url", article.getString("url"));
                mparticle.put("content", article.getString("description"));

                mparticles.add(mparticle);
            } else {
                continue;
            }
        }
        JSONObject mpnews = new JSONObject();
        mpnews.put("articles", mparticles);
        log.info("mparticles: " + mpnews.toString());
        return WeChatHttpsUtil.httpsRequest(UPLOAD_NEWS_URL + token, "POST",
                mpnews.toString());
    }
    
    private void sendMessage(String msg, String token) {
        String request = SEND_MESSAGE_REQUEST_URL + token;
        JSONObject jo = WeChatHttpsUtil.httpsRequest(request, "POST", msg);
        log.info("sendMessage ret: " + jo.toString());
    }
}
