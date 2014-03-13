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
<script src="//ajax.googleapis.com/ajax/libs/dojo/1.9.1/dojo/dojo.js"></script>
<script src="<c:url value='/resources/js/libs/eventsource.js'/>"></script>
<script type="text/javascript"
	src="http://code.jquery.com/jquery-1.11.0.min.js"></script>


<script>
	dojo.require("dojo.on");
	dojo.require("dojo.io-query");
	
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
	            url : "chat/logout",
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
			url : "chat/getMessages",
			handleAs : "json",
			load : function(data) {
				console.info("data: " + data.content);
				var text = dojo.byId("text").value;
				text += '\r\n' + data.date + ' ' + data.sender + '：' + data.content;
				dojo.byId("text").innerHTML = text;
				setTimeout(function() {
                  getMessage();
              }, 1000);
			},
			error : function(error) {
				console.info("error: " + error);
				getMessage();
// 		        setTimeout(function() {
// 		            getMessage();
// 		        }, 12000);
			}
		}
		dojo.xhrGet(xhrArgs);
	}

	function postMessage() {
		var send = dojo.byId("sendMessage");
		dojo.on(send, "click", function() {

			var queryStr;
			require([ "dojo/io-query" ], function(ioQuery) {
				var query = {
					message : dojo.toJson({
						"content" : dojo.byId("messageInput").value,
					})
				};
				// Assemble the new uri with its query string attached.
				queryStr = ioQuery.objectToQuery(query);

			});
			console.log("queryStr: " + queryStr);
			var xhrArgs = {
				url : "chat/sendMessageRequest",
				postData : dojo.toJson({"content" : dojo.byId("messageInput").value}),
				handleAs : "json",
				load : function(data) {
					console.log("res:" + data);
				},
				error : function(eror) {
					console.log("error:" + error);
				}
			}
			var deferred = dojo.xhrPost(xhrArgs);
		});
	}

	dojo.ready(function() {
		postMessage();
		getMessage();
		//ShowMessage(200,100);
		window.onbeforeunload = exitRoom;
	});
</script>
</head>
<body>
	<div class="chatMainPanel" id="chatMainPanel" style="width: 500px">

		<div class="chatTitle">
			<div class="chatNameWrap">
				<p class="chatName" id="messagePanelTitle">Room</p>
			</div>

		</div>
		<textarea id="text" rows="10" style="width: 494px;"></textarea>

		<div id="chat_editor" class="chatOperator lightBorder">
			<div class="inputArea">
				<textarea type="text" id="messageInput"
					class="chatInput lightBorder"></textarea>
				<a href="javascript:;" class="chatSend" id="sendMessage"><b>发送</b></a>
			</div>
		</div>
	</div>
	
	<div id="invitation">
        <a id="accept" class="closeInvation chatSend" style="float:left;" href="javascript:void(0);">接受</a>
        <a id="refuse" class="closeInvation chatSend" style="float:right;" href="javascript:void(0);">拒绝</a>
    </div>
</body>
</html>