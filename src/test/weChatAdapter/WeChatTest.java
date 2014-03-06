package weChatAdapter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;

import com.hesong.jsonrpc.JsonrpcHandler;
import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.menu.BaseButton;
import com.hesong.weChatAdapter.menu.Button;
import com.hesong.weChatAdapter.menu.ClickSubButton;
import com.hesong.weChatAdapter.menu.Menu;
import com.hesong.weChatAdapter.menu.ViewSubButton;
import com.hesong.weChatAdapter.message.request.TextMessage;
import com.hesong.weChatAdapter.message.send.TextMessageToSend;
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
        System.out.println(WeChatHttpsUtil.getAccessToken(API.APPID, API.APP_SECRET));
        
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
        TextMessageToSend msg = new TextMessageToSend();
        msg.setTouser("oJr3Ht294GRv1J0fJYvGlF6kfNSo");
        msg.setMsgtype("text");
        Map<String, String> map = new HashMap<>();
        map.put("content", "Send msg test");
        //msg.setText(map);
        String jsonMsg = JSONObject.fromObject(msg).toString();
        System.out.println(jsonMsg);

        //MessageManager.sendMessage(msg);
    }
    
    @Test
    public void jsonrpcTest(){
        TextMessageToSend msg = new TextMessageToSend();
        msg.setTouser("ogfGduA0yfPY_aET7do8GvE5Bm4w");
        msg.setMsgtype("text");
        Map<String, String> map = new HashMap<>();
        map.put("content", "Send msg test");
        //msg.setText(map);
        String jsonrpc;
        jsonrpc = "{\"jsonrpc\": \"2.0\", \"method\": \"sendMessage\", \"params\": [\"ogfGduA0yfPY_aET7do8GvE5Bm4w\",\"text\",{\"content\":\"Send jsonrpc msg test\"}], \"id\": 1}";
        //jsonrpc = "{\"jsonrpc\": \"2.0\", \"method\": \"echo\", \"params\": [\"Hello JSON-RPC\"], \"id\": 1}";
        System.out.println(jsonrpc);

        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
            handler.handle(jsonrpc);
    }
    
    @Test
    public void createMenuTest(){
        ClickSubButton btn11 = new ClickSubButton();  
        btn11.setName("天气预报");  
        btn11.setType("click");  
        btn11.setKey("11"); 
  
        ViewSubButton btn12 = new ViewSubButton();  
        btn12.setName("历史上的今天");  
        btn12.setType("view");  
        btn12.setUrl("www.baidu.com");  
  
        ClickSubButton btn21 = new ClickSubButton();  
        btn21.setName("歌曲点播");  
        btn21.setType("click");  
        btn21.setKey("21");  
  
        ClickSubButton btn22 = new ClickSubButton();  
        btn22.setName("经典游戏");  
        btn22.setType("click");  
        btn22.setKey("22");  
  
        ClickSubButton btn31 = new ClickSubButton();  
        btn31.setName("Q友圈");  
        btn31.setType("click");  
        btn31.setKey("31");  
  
        ClickSubButton btn32 = new ClickSubButton();  
        btn32.setName("电影排行榜");  
        btn32.setType("click");  
        btn32.setKey("32");  
  
        ClickSubButton btn33 = new ClickSubButton();  
        btn33.setName("幽默笑话");  
        btn33.setType("click");  
        btn33.setKey("33");  
  
        Button mainBtn1 = new Button();  
        mainBtn1.setName("生活助手");  
        mainBtn1.setSub_button(new BaseButton[] { btn11, btn12});  
  
        Button mainBtn2 = new Button();  
        mainBtn2.setName("休闲驿站");  
        mainBtn2.setSub_button(new ClickSubButton[] { btn21, btn22});  
  
        Button mainBtn3 = new Button();  
        mainBtn3.setName("更多体验");  
        mainBtn3.setSub_button(new ClickSubButton[] { btn31, btn32, btn33 });  
  
        Menu menu = new Menu();  
        menu.setButton(new Button[] { mainBtn1, mainBtn2, mainBtn3 });  
        
        String jsonMenu = JSONObject.fromObject(menu).toString();
        MessageManager.createMenu(API.APPID, API.APP_SECRET, jsonMenu);
    }
    
    @Test
    public void getMenuTest(){
        MessageManager.getMenu(API.APPID, API.APP_SECRET);
    }
    
    @Test
    public void deleteMenuTest(){
        System.out.println(MessageManager.deleteMenu(API.APPID, API.APP_SECRET));
    }
    
    @Test
    public void getFollowersTest(){
        MessageManager.getFollowersList(API.ACCESS_TOKEN);
    }
    
    @Test
    public void getFollowersFromTest(){
        MessageManager.getFollowersFrom(API.ACCESS_TOKEN, "ogfGduNuSPi5TyIcYOyMzvlnRF9c");
    }
}
