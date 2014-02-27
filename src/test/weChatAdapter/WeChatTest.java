package weChatAdapter;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.sf.json.JSONObject;

import org.junit.Test;

import com.hesong.jsonrpc.JsonrpcHandler;
import com.hesong.jsonrpc.WeChatMethodSet;
import com.hesong.smartbus.client.WeChatCallback;
import com.hesong.smartbus.client.net.Client;
import com.hesong.smartbus.client.net.Client.ConnectError;
import com.hesong.weChatAdapter.manager.MessageManager;
import com.hesong.weChatAdapter.message.request.TextMessage;
import com.hesong.weChatAdapter.message.send.TextMessageToSend;
import com.hesong.weChatAdapter.tools.SignatureChecker;
import com.hesong.weChatAdapter.tools.WeChatXMLParser;

public class WeChatTest {

    @Test
    public void sha1test() {
        System.out.println(SignatureChecker.SHA1("woshishei"));
        assertTrue(SignatureChecker.SHA1("woshishei").equals(
                "5c01324982957013fa166fa246fef976a9102279"));
    }

    @Test
    public void smartBusTest() {
        Client.initialize(Byte.parseByte("30"));

        Client client = new Client(Byte.parseByte("23"), (long) 11,
                "192.168.1.203", (short) 8089, "WeChat smartbus client");
        client.setCallbacks(new WeChatCallback());

        System.out.println("Connect...");

        try {
            client.connect();
        } catch (ConnectError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Connect failed.");
        }
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
        msg.setTouser("ogfGduA0yfPY_aET7do8GvE5Bm4w");
        msg.setMsgtype("text");
        Map<String, String> map = new HashMap<>();
        map.put("content", "Send msg test");
        msg.setText(map);
        String jsonMsg = JSONObject.fromObject(msg).toString();
        System.out.println(jsonMsg);

        MessageManager.sendMessage(msg);
    }
    
    @Test
    public void jsonrpcTest(){
        TextMessageToSend msg = new TextMessageToSend();
        msg.setTouser("ogfGduA0yfPY_aET7do8GvE5Bm4w");
        msg.setMsgtype("text");
        Map<String, String> map = new HashMap<>();
        map.put("content", "Send msg test");
        msg.setText(map);
        String jsonrpc;
        jsonrpc = "{\"jsonrpc\": \"2.0\", \"method\": \"sendMessage\", \"params\": [\"ogfGduA0yfPY_aET7do8GvE5Bm4w\",\"text\",{\"content\":\"Send jsonrpc msg test\"}], \"id\": 1}";
        //jsonrpc = "{\"jsonrpc\": \"2.0\", \"method\": \"echo\", \"params\": [\"Hello JSON-RPC\"], \"id\": 1}";
        System.out.println(jsonrpc);

        JsonrpcHandler handler = new JsonrpcHandler(new WeChatMethodSet());
        try {
            handler.handle(jsonrpc);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
