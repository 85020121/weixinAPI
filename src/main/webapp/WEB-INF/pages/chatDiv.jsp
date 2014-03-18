<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<link rel="stylesheet" href="<c:url value='/resources/css/chatTab.css'/>" />

    <link rel="stylesheet" href="<c:url value='/resources/js/libs/dojo/1.9.1/dijit/themes/claro/claro.css'/>" />
    <script>dojoConfig = {parseOnLoad: false}</script>
<script src="//ajax.googleapis.com/ajax/libs/dojo/1.9.1/dojo/dojo.js"></script>
     
    <script>
require(["dojo/parser", "dijit/layout/TabContainer", "dijit/layout/ContentPane", "dojo/dom-construct", "dojo/dom", "dojo/on", "dijit/registry", "dojox/layout/ScrollPane", "dojo/domReady!"],
        function(parser, TabContainer, ContentPane, domConstruct, dom, on, registry){

    parser.parse();
    
    var chatContent = "<div id='chatboard' style='height:200px; border:1px solid black; overflow:auto; margin-bottom'>";
    chatContent += "<ol id='roomxlist'><li>text</li><li>text</li></ol></div>";
    chatContent += "<div id='chat_editor' class='chatOperator lightBorder'>";
    chatContent += "<div class='inputArea'><textarea type='text' id='room0messageInput' class='chatInput lightBorder'></textarea>";
    chatContent += "<a href='javascript:;' class='chatSend' onclick='postMessage('room0')' id='room0sendMessage'><b>发送</b></a></div></div>"

    var closablePane = new ContentPane({
        title:"Close Me",
        closable: true,
        content: chatContent,
        onClose: function(){
           // confirm() returns true or false, so return that.
           return confirm("Do you really want to Close this?");
        }
    });
    

    onCloseEx.addChild(closablePane);
    
    on(dom.byId("placeBA"), "click", function(){
    	console.log("clicked")
        var row = domConstruct.toDom("<li>AAAAAAAAAAAAAAAAAAAAAAAA</li>");
        domConstruct.place(row, "roomxlist");
    	//var content = dojo.byId("roomxlist").innerHTML;
    	dojo.byId("chatboard").scrollTop = dojo.byId("chatboard").scrollHeight;
    	//content += "<li>AAAAAAAAAAAAAAAAAAAAAAAA</li>";
    	//dojo.byId("roomxlist").innerHTML = content;
    	console.log("height:"+document.getElementById("roomxlist").scrollHeight);

      });
    
});
    </script>
</head>
<body class="claro">
    <div style="height: 100px;">
    <div id="chatboardTab" data-dojo-id="onCloseEx" data-dojo-type="dijit/layout/TabContainer" style="width: 600px;height: 100px;" doLayout="false">
        <div data-dojo-type="dijit/layout/ContentPane" title="Room0" data-dojo-props="selected:true">
            <div class="chatMainPanel" id="chatMainPanel" style="width: 500px;">

        <textarea id="room0chatboard" rows="10" style="width: 494px;"></textarea>

        <div id="chat_editor" class="chatOperator lightBorder">
            <div class="inputArea">
                <textarea type="text" id="room0messageInput"
                    class="chatInput lightBorder"></textarea>
                <a href="javascript:;" class="chatSend" onclick='postMessage("room0")' id="room0sendMessage"><b>发送</b></a>
            </div>
        </div>
    </div>
    
        </div>
        <div data-dojo-type="dijit/layout/ContentPane" title="Other Closable" data-dojo-props="closable:true, onClose:function(){return confirm('really?');}">
            ... I have an in-line onClose
        </div>
    </div>
</div>

<button id="placeBA" style="padding-left:700px">Place node</button>
</body>
</html>