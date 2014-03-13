<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<%
    String user = (String) session.getAttribute("sender");
%>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet"
	href="<c:url value='/resources/css/chatroom.css'/>" />
    <link rel="stylesheet" href="<c:url value='/resources/js/libs/dojo/1.9.1/dijit/themes/claro/claro.css'/>" />
    
<script src="//ajax.googleapis.com/ajax/libs/dojo/1.9.1/dojo/dojo.js"></script>
<script src="<c:url value='/resources/js/libs/eventsource.js'/>"></script>
<script type="text/javascript"
	src="http://code.jquery.com/jquery-1.11.0.min.js"></script>


<script>
	dojo.require("dojo.on");
    dojo.require("dojo.io-query");
    dojo.require("dojo.query");
    dojo.require("dojo.dom");
    dojo.require("dojo.NodeList-traverse");
    
	dojoConfig = {parseOnLoad: true};
	function ShowMessage(widht,height) {
	    var TopY=0;//初始化元素距父元素的距离
	    $("#invitation").css("width",widht+"px").css("height",height+"px");//设置消息框的大小
	    $("#invitation").slideDown(1000);//弹出
	    $(".closeInvation").click(function() {//当点击关闭按钮的时候
	         if(TopY==0){
	               $("#invitation").slideUp(1000);//这里之所以用slideUp是为了兼用Firefox浏览器
	         }
	        else
	        {
	              $("#invitation").animate({top: TopY+height}, "slow", function() { $("#invitation").hide(); });//当TopY不等于0时  ie下和Firefox效果一样
	        }
	     });
	     $(window).scroll(function() {
	         $("#invitation").css("top", $(window).scrollTop() + $(window).height() - $("#invitation").height());//当滚动条滚动的时候始终在屏幕的右下角
	         TopY=$("#invitation").offset().top;//当滚动条滚动的时候随时设置元素距父原素距离
	      });
	}
	
	function exitRoom() {
		var xhrArgs = {
	            url : "client/logout",
	            handleAs : "text",
	            load : function(data) {
	            },
	            error : function(error) {
	            }
	        }
	        dojo.xhrGet(xhrArgs);
	}

	function getMessage() {

	var xhrArgs = {
			url : "getMessages",
			handleAs : "json",
			load : function(data) {
				console.info("data: "+ data.roomId + ' ' + data.date + ' ' + data.sender + '：' + data.content);
				var room = data.roomId;
				var text = dojo.byId(room+"chatboard").value;
				text += '\r\n' + data.date + ' ' + data.sender + '：' + data.content;
				dojo.byId(room+"chatboard").innerHTML = text;
				getMessage();
// 				setTimeout(function() {
//                   getMessage();
//               }, 1000);
			},
			error : function(error) {
				console.info("error: " + error);
				getMessage();
			}
		}
		dojo.xhrGet(xhrArgs);
	}

	function postMessage(room) {

		console.log("event:"+room);
		
// 			var queryStr;
// 			require([ "dojo/io-query" ], function(ioQuery) {
// 				var query = {
// 					message : dojo.toJson({
// 						"content" : dojo.byId(room+"messageInput").value,
// 					})
// 				};
// 				// Assemble the new uri with its query string attached.
// 				queryStr = ioQuery.objectToQuery(query);

// 			});
// 			console.log("queryStr: " + queryStr);
			var xhrArgs = {
				url : "msg",
				postData : dojo.toJson({"content" : dojo.byId(room+"messageInput").value,
				                        "roomId" : room,
			                            "msgtype" : "text",
			                            "sender" : "Bowen"
					                    }),
				handleAs : "text",
				load : function(data) {
					console.log("res:" + data);
				},
				error : function(eror) {
					console.log("error:" + error);
				}
			}
			var deferred = dojo.xhrPost(xhrArgs);
		
	}

	dojo.ready(function() {
		//postMessage();
		getMessage();
		//ShowMessage(200,100);
		window.onbeforeunload = exitRoom;

	});
</script>
</head>
<body>
        
	<div class="chatMainPanel" id="chatMainPanel" style="width: 500px;">

		<div class="chatTitle">
			<div class="chatNameWrap">
				<p class="chatName" id="messagePanelTitle">Room0</p>
			</div>

		</div>
		<textarea id="room0chatboard" rows="10" style="width: 494px;"></textarea>

		<div id="chat_editor" class="chatOperator lightBorder">
			<div class="inputArea">
				<textarea type="text" id="room0messageInput"
					class="chatInput lightBorder"></textarea>
				<a href="javascript:;" class="chatSend" onclick='postMessage("room0")' id="room0sendMessage"><b>发送</b></a>
			</div>
		</div>
	</div>
	
	   <div class="chatMainPanel room1" id="room1" style="width: 500px;left:600px">

        <div class="chatTitle">
            <div class="chatNameWrap">
                <p class="chatName" id="messagePanelTitle">Room1</p>
            </div>

        </div>
        <textarea id="room1chatboard" class="chatboard" type="text" rows="10" style="width: 494px;"></textarea>
        <div id="chat_editor" class="chatOperator lightBorder">
            <div class="inputArea">
                <textarea type="text" id="room1messageInput"
                    class="chatInput lightBorder"></textarea>
                <a href="javascript:;" class="chatSend" onclick='postMessage("room1")' id="room1sendMessage"><b>发送</b></a>
            </div>
        </div>
    </div>
	
	<div style="width: 350px; height: 290px; left:600px">
    <div id="tc1-prog"></div>
</div>
	
	
	<div class="invitation">
        <a id="accept" class="closeInvation chatSend" style="float:left;" href="javascript:void(0);">接受</a>
        <a id="refuse" class="closeInvation chatSend" style="float:right;" href="javascript:void(0);">拒绝</a>
    </div>
</body>
</html>