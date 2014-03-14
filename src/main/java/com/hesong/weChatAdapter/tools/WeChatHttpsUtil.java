package com.hesong.weChatAdapter.tools;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.hesong.weChatAdapter.model.AccessToken;

public class WeChatHttpsUtil {
    private static Logger log = Logger.getLogger(WeChatHttpsUtil.class);

    public final static String access_token_url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";

    public static JSONObject httpsRequest(String requestUrl,
            String requestMethod, String outputStr) {
        JSONObject jsonObject = null;
        StringBuffer buffer = new StringBuffer();

        try {
            TrustManager[] tm = { new HttpsTrustManager() };
            SSLContext sslCtxt = SSLContext.getInstance("SSLv3");
            sslCtxt.init(null, tm, new java.security.SecureRandom());

            SSLSocketFactory ssf = sslCtxt.getSocketFactory();

            URL url = new URL(requestUrl);
            HttpsURLConnection httpsUrlConct = (HttpsURLConnection) url
                    .openConnection();
            httpsUrlConct.setSSLSocketFactory(ssf);

            httpsUrlConct.setDoInput(true);
            httpsUrlConct.setDoOutput(true);
            httpsUrlConct.setUseCaches(false);

            httpsUrlConct.setRequestMethod(requestMethod);

            if ("GET".equalsIgnoreCase(requestMethod)) {
                httpsUrlConct.connect();
                // TODO: use sleep to avoid ssl exception just like:
                // Received fatal alert: bad_record_mac
                // maybe find some other solutions
                Thread.sleep(1000);
            }

            if (outputStr != null) {
                OutputStream out = httpsUrlConct.getOutputStream();
                out.write(outputStr.getBytes("UTF-8"));
                out.close();
            }

            InputStream in = httpsUrlConct.getInputStream();
            InputStreamReader inputReader = new InputStreamReader(in, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            inputReader.close();
            in.close();
            in = null;
            httpsUrlConct.disconnect();
            jsonObject = JSONObject.fromObject(buffer.toString());
        } catch (ConnectException ce) {
            log.error("WeChat server connection time out.");
            return getErrorMsg(9003, "WeChat server connection time out: "+ce.toString());
        } catch (Exception e) {
            log.error("HTTPS request error: " + e.toString());
            return getErrorMsg(9002, "HTTPS request error: " + e.toString());
        }

        return jsonObject;
    }

    public static JSONObject httpPostRequest(String requestUrl, String outputStr, int timeout) {
        StringBuffer buffer = new StringBuffer();
        InputStream in = null;
        try {
            log.info("Post content: "+outputStr);
            URL url = new URL(requestUrl);
            URLConnection httpUrlConct = url.openConnection();

            httpUrlConct.setDoInput(true);
            httpUrlConct.setDoOutput(true);
            httpUrlConct.setUseCaches(false);
            httpUrlConct.setReadTimeout(timeout); // 0 to infini

            if (outputStr != null) {
                OutputStream out = httpUrlConct.getOutputStream();
                out.write(outputStr.getBytes("UTF-8"));
                out.close();
            }

            in = httpUrlConct.getInputStream();
            InputStreamReader inputReader = new InputStreamReader(in, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(inputReader);

            String str = null;
            while ((str = bufferedReader.readLine()) != null) {
                buffer.append(str);
            }
            bufferedReader.close();
            inputReader.close();
            
            return JSONObject.fromObject(buffer.toString());
            //jsonObject = JSONObject.fromObject(buffer.toString());
//            if(buffer.toString().equalsIgnoreCase("success")) {
//                return getErrorMsg(0, "OK");
//            } else {
//                return getErrorMsg(8001, "Request refused.");
//            }
        } catch(SocketTimeoutException se){
            log.info("Connection time out.");
            return getErrorMsg(9001, "HTTP connection timeout.");
        }catch (Exception e) {
            e.printStackTrace();
            log.error("Http request error: " + e.toString());
            return getErrorMsg(9009, "Http request error: " + e.toString());
        }
        
    }

    public static AccessToken getAccessToken(String appid, String appSecret) {
        AccessToken token = null;

        String requestUrl = access_token_url.replace("APPID", appid).replace(
                "APPSECRET", appSecret);
        JSONObject jo = httpsRequest(requestUrl, "GET", null);

        if (jo != null && jo.getString("access_token") != null) {
            try {
                token = new AccessToken(appid, appSecret,
                        jo.getString("access_token"), jo.getInt("expires_in"));
                log.info(token.toString());
            } catch (Exception e) {
                log.error("Get token failed, errorcode:{"
                        + jo.getInt("errcode") + "} errormsg:{"
                        + jo.getString("errmsg") + "}");
                return null;
            }
        }

        return token;
    }
    
    public static JSONObject getErrorMsg(int errcode, String errmsg) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("errcode", errcode);
        jsonObject.put("errmsg", errmsg);
        return jsonObject;
    }
}
