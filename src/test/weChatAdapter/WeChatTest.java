package weChatAdapter;

import static org.junit.Assert.*;

import java.util.Date;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.junit.Test;

import com.hesong.jsonrpc.JsonrpcHandler;
import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.weChatAdapter.message.request.TextMessage;
import com.hesong.weChatAdapter.tools.API;
import com.hesong.weChatAdapter.tools.SignatureChecker;
import com.hesong.weChatAdapter.tools.WeChatHttpsUtil;
import com.hesong.weChatAdapter.tools.WeChatXMLParser;

public class WeChatTest {

    @Test
    public void sha1test() {
        System.out.println(SignatureChecker.SHA1("woshishei"));
        assertTrue(SignatureChecker.SHA1("woshishei").equals(
                "5c01324982957013fa166fa246fef976a9102279"));
    }

    @Test
    public void getTokenTest() {
        System.out.println(WeChatHttpsUtil.getAccessToken(API.APPID,
                API.APP_SECRET));

    }

    @Test
    public void xmlTest() {
        TextMessage msg = new TextMessage();
        msg.setContent("Test");
        msg.setCreateTime(new Date().getTime());
        msg.setFromUserName("asfas");
        msg.setMsgId((long) 112233);
        msg.setMsgType("text");
        msg.setToUserName("vvvvv");

        WeChatXMLParser.xstream.alias("xml", msg.getClass());
        System.out.println(WeChatXMLParser.xstream.toXML(msg));
    }

    @Test
    public void sendMsgTest() {
        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
        String msg = "{\"jsonrpc\": \"2.0\", \"method\": \"SendImMessage\", \"params\": [null, \"sender01\",\"oJr3Ht02_3HFoKDl0WwkdBlLvc3o\", \"room1\", { \"msgtype\":\"text\", \"text\": { \"content\":\"Hello'\\\" *&^%$#你\\n好啊[]{}World\" } } ], \"id\": 3}";
        System.out.println(handler.handle(msg));
    }

    @Test
    public void manageMenuTest() {
        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
        String get = "{\"jsonrpc\": \"2.0\", \"method\": \"ManageMenu\", \"params\": [\"account\",\"get\", null], \"id\": 2}";
        String create = "{\"jsonrpc\": \"2.0\", \"method\": \"ManageMenu\", \"params\": [\"account\",\"create\",{ \"button\":[ { \"type\":\"click\", \"name\":\"今日歌曲\", \"key\":\"V1001_TODAY_MUSIC\" }, { \"type\":\"click\", \"name\":\"歌手简介\", \"key\":\"V1001_TODAY_SINGER\" }, { \"name\":\"菜单\", \"sub_button\":[ { \"type\":\"view\", \"name\":\"搜索\", \"url\":\"http://www.soso.com/\" }, { \"type\":\"view\", \"name\":\"视频\", \"url\":\"http://v.qq.com/\" }, { \"type\":\"click\", \"name\":\"赞一下我们\", \"key\":\"V1001_GOOD\" }] }] } ], \"id\": 2}";
        String delete = "{\"jsonrpc\": \"2.0\", \"method\": \"ManageMenu\", \"params\": [\"account\",\"delete\", null], \"id\": 2}";
        System.out.println(handler.handle(create));
        System.out.println(handler.handle(get));
    }
    
    @Test
    public void getClientInfoTest(){    
        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
        String jsonrpc = "{\"jsonrpc\": \"2.0\", \"method\": \"GetClient\", \"params\": [\"abcdef\",[\"oJr3Ht02_3HFoKDl0WwkdBlLvc3o\", \"oJr3Ht294GRv1J0fJYvGlF6kfNSo\",\"123asd\"] ], \"id\": 3}";
        handler.handle(jsonrpc);
    }
    
    @Test
    public void test(){
        JSONObject jo = new JSONObject();
        jo.put("nihao", "nihao");
        String jsonrpc = "{\"jsonrpc\": \"2.0\", \"method\": \"Invited\", \"params\": [\"abcdef\",\"fromuser123\",\"room_id123\", \"to_user123\",\"comeon in\",null,120,null], \"id\": 3}";
        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
        String j = handler.handle(jsonrpc);
        System.out.println(j);
    }
    
    @Test
    public void reqTest(){
        JSONObject jo = new JSONObject();
        jo.put("content", "wexin test");
        WeChatHttpsUtil.httpPostRequest("http://localhost:8080/weChatAdapter/chat/sendMessageQuest", jo.toString(), 2000);
    }

//    @Test
//    public void getFollowersTest() {
//        MessageManager.getFollowersList(API.ACCESS_TOKEN);
//    }
//
//    @Test
//    public void getFollowersFromTest() {
//        MessageManager.getFollowersFrom(API.ACCESS_TOKEN,
//                "ogfGduNuSPi5TyIcYOyMzvlnRF9c");
//    }
}
