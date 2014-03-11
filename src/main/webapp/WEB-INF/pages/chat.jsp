<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html >
<head>

    <link rel="stylesheet" href="<c:url value='/resources/js/libs/dojo/1.9.1/dijit/themes/claro/claro.css'/>">
    <style type="text/css">
#messageLog {
  margin-top: 20px;
  padding: 5px;
  width: 400px;
}
    </style>
    <script>dojoConfig = {parseOnLoad: true}</script>
    <script src='//ajax.googleapis.com/ajax/libs/dojo/1.9.1/dojo/dojo.js'></script>
    
    <script>
dojo.require("dijit.form.TextBox"); // Those widgets are only included to make the example look nice
dojo.require("dijit.form.Button"); // Those widgets are only included to make the example look nice

dojo.require("dojo.io.script");
dojo.require("dojox.cometd");
dojo.require("dojox.cometd.callbackPollTransport");
dojo.ready(function(){
    dojox.cometd.init("http://localhost:8080/weChatAdapter/chat");
    dojox.cometd.subscribe("/demo", function(message){
        console.log("received", message);
        dojo.byId("messageLog").
            appendChild(document.createElement("div")).
            appendChild(document.createTextNode(message.data.from + ": " + message.data.text));
    });
    dojo.connect(dojo.byId("send"), "onclick", function(){
        if(!dojo.byId("sendName").value.length || !dojo.byId("sendText").value.length){
                            alert("Please enter some text");
                            return;
                    }
                    dojox.cometd.publish("/demo", {
             from: dojo.byId("sendName").value,
             text: dojo.byId("sendText").value
        });
    });
});
    </script>
</head>
<body class="claro">
    <div id="chatroom">
    <div style="clear: both;"><label for="sendName" style="float: left; width: 100px; padding: 3px;">Name:</label> <input id="sendName" type="text" data-dojo-type="dijit.form.TextBox"></div>
    <div style="clear: both;"><label for="sendText" style="float: left; width: 100px; padding: 3px;">Message:</label> <input id="sendText" type="text" data-dojo-type="dijit.form.TextBox"><button id="send" data-dojo-type="dijit.form.Button">Send Message</button></div>
    <div id="messageLog"><strong>Messages:</strong></div>
</div>
</body>
</html>