<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" href="<c:url value='/resources/css/chatTab.css'/>" />
    <link rel="stylesheet" href="<c:url value='/resources/js/libs/dojo/1.9.1/dijit/themes/claro/claro.css'/>" />
<script src="<c:url value='/resources/js/vlcPlayer.js'/>"></script>    
<script src="//ajax.googleapis.com/ajax/libs/dojo/1.9.1/dojo/dojo.js"></script>
<script type="text/javascript"
	src="http://code.jquery.com/jquery-1.11.0.min.js"></script>


<script>
	dojo.require("dojo.on");
    dojo.require("dojo.io-query");
    dojo.require("dojo.query");
    dojo.require("dojo.aspect");
    dojo.require("dojo.dom");
    dojo.require("dojo.cookie");
    dojo.require("dojo.parser");
    dojo.require("dijit.layout.TabContainer");
    dojo.require("dijit.layout.ContentPane");
    dojo.require("dijit.registry");
    
    
	// 弹出/收起邀请提示框
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
	
    // 尝试加入房间
    function enterRoom(room) {
        var xhrArgs = {
                url : "http://localhost:8080/weChatAdapter/client/"+dojo.cookie("MOCK_CLIENT_ID")+"/enter_room",
                postData : dojo.toJson({"roomId" : room}),
                handleAs : "text",
                load : function(data) {
                },
                error : function(error) {
                }
            }
            dojo.xhrPost(xhrArgs);
    }
	
	// 尝试退出房间
	function exitRoom(room) {
		var xhrArgs = {
	            url : "http://localhost:8080/weChatAdapter/client/"+dojo.cookie("MOCK_CLIENT_ID")+"/exit_room",
	            postData : dojo.toJson({"roomId" : room}),
	            handleAs : "text",
	            load : function(data) {
	            },
	            error : function(error) {
	            }
	        }
	        dojo.xhrPost(xhrArgs);
	}
	
	// 邀请反馈
	function ackInvitation(isAccepted){
		console.log(isAccepted);
		var xhrArgs = {
                url : "http://localhost:8080/weChatAdapter/client/"+dojo.cookie("MOCK_CLIENT_ID")+"/ackInvitation",
                handleAs : "text",
                postData : dojo.toJson({"roomId" : dojo.byId("invitationRoomId").innerHTML,
                    "agreed":isAccepted
                    }),
                load : function(data) {
                	console.log("invitation data: "+data);
                },
                error : function(error) {
                    console.log("invitation error: "+error);
                }
            }
            dojo.xhrPost(xhrArgs);
	}

	// 接收消息，并根据消息类型修改客户端聊天窗口
	function getMessage() {
	var xhrArgs = {
			url : "http://localhost:8080/weChatAdapter/client/getMessages",
			handleAs : "json",
			load : function(data) {
				if(data.msgtype == "invitation"){
					dojo.byId("invitationSender").innerHTML = data.sender + " 邀请你加入房间：";
                    dojo.byId("invitationRoomId").innerHTML = data.roomId;
					ShowMessage(180,100);
				} else if(data.msgtype == "enteredRoom"){
					console.log("Entered in to room: "+data.roomId);
					createRoomTab(data.roomId);
				} else if(data.msgtype == "exitedRoom"){
                    console.log("Exited from room: "+data.roomId);
                    var roomContentPane = dijit.byId(data.roomId+"contentPaneId");
                    chatboardTab.removeChild(roomContentPane);
                    roomContentPane.destroy();
                } else {
                	var messageContentDiv;
                	if(data.msgtype == "image") {
                		messageContentDiv = "<img width='240' src='ftp://bowen:waiwai@127.0.0.1"+data.content+"' />";
                	} else if(data.msgtype == "voice"){
                		
                	} else {
                		messageContentDiv = data.content;
                	}
                	
					console.info("data: "+ data.roomId + ' ' + data.date + ' ' + data.sender + '：' + messageContentDiv);
					var room = data.roomId;
					var chatBoardId = room+"chatboard";
					var row;
					if(dojo.cookie("MOCK_CLIENT_ID") == data.sender){
						row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
			            row += "<pre style='white-space:pre-wrap'>"+messageContentDiv+"</pre></div></div></div></div>";
					} else {
						var d = new Date();
						row = "<div class='senderName'>"+d.getHours()+":"+d.getMinutes()+"  "+data.sender+"</div><div class='chatItem you'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
						row += "<pre style='white-space:pre-wrap'>"+messageContentDiv+"</pre></div></div></div></div>";
					}
					require(["dojo/dom-construct"], function(domConstruct){
						  domConstruct.place(row, chatBoardId);
						});
					dojo.byId(room+"chatboard").scrollTop = dojo.byId(room+"chatboard").scrollHeight;
					//text += '\r\n' + data.date + ' ' + data.sender + '：' + data.content;
					//dojo.byId(room+"chatboard").innerHTML = text;
					var receiverTabId = room+"contentPaneId";
					if(chatboardTab.selectedChildWidget.id != receiverTabId){
						newMessageRemaind(receiverTabId);
					}
				}
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

	// 发送消息
	function postMessage(room) {

		console.log("event:"+room);
			var xhrArgs = {
				url : "msg",
				postData : dojo.toJson({"content" : dojo.byId(room+"messageInput").value,
				                        "roomId" : room,
			                            "msgtype" : "text",
			                            "sender" : dojo.cookie("MOCK_CLIENT_ID")
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
	
	// 创建聊天窗口，设置房间名
	function createRoomTab(roomId){
		  var d = new Date();
		  var chatContent = "<div id='"+roomId+"chatboard' style='height:250px; border:1px solid black; overflow:auto; margin-bottom;background-color: #EFF3F7;'>";
		  chatContent += "<div class='time'> <span class='timeBg left'></span> "+d.getHours()+":"+d.getMinutes()+" <span class='timeBg right'></span></div></div>";
		    chatContent += "<div id='chat_editor' class='chatOperator lightBorder'>";
		    chatContent += "<div class='inputArea'><textarea type='text' id='"+roomId+"messageInput' class='chatInput lightBorder'></textarea>";
		    chatContent += "<a href='javascript:;' class='chatSend' onclick='postMessage(\""+roomId+"\")' id='"+roomId+"sendMessage'><b>发送</b></a></div></div>"

		    var closablePane = new dijit.layout.ContentPane({
		            title : roomId,
		            closable : true,
		            content : chatContent,
		            id : roomId+"contentPaneId",
		            onClose : function() {
		            // confirm() returns true or false, so return that.
		            var close =  confirm("Do you really want to Close this?");
		            if(close){
		            	exitRoom(roomId);
		            	return false;
		            } else {
		            	console.log("Not close");
		            }
		            }
		        });
		    closablePane.domNode.setAttribute("widgetId", roomId);
		    chatboardTab.addChild(closablePane);
	}
	
	function newMessageRemaind(tabId){
        var change = true;
        var intervalRet = 
	        setInterval(function(){
	            if(change){
	                dijit.byId(tabId).controlButton.attr("style", "color: #B2CF73;");
	                change = false;
	            } else {
	                dijit.byId(tabId).controlButton.attr("style", "color: white;");
	                change = true;
	            }
	        },500);
        setTimeout(function(){
        	clearInterval(intervalRet);
        	dijit.byId(tabId).controlButton.attr("style", "color: #B2CF73;");
        }, 3000);

	}

	dojo.ready(function() {
		//postMessage();
		getMessage();
		//ShowMessage(200,100);
		window.onbeforeunload = exitRoom;
		
		dojo.parser.parse();
        createRoomTab("room0");
        createRoomTab("room1");
        //console.log(dijit.byId("room0").controlButton.domNode);
        //dojo.style(dijit.byId("room0contentPaneId").controlButton.domNode,{background-color:"red"});
        //dijit.byId("room0contentPaneId").controlButton.attr("style", "color: #9B7725;");
        //console.log("list: "+chatboardTab.tablist.pane2button["room0contentPaneId"]);
        //newMessageRemaind("room0contentPaneId");
        //chatboardTab.removeChild(dijit.byId("room0contentPaneId"));
        dojo.aspect.after(chatboardTab, "selectChild", function (event) {
            console.log("You selected ", chatboardTab.selectedChildWidget.id);
            dijit.byId(chatboardTab.selectedChildWidget.id).controlButton.attr("style", "color: black;");
       });
    });
</script>
</head>
<body class="claro">
    <div style="height: 200px;">
        <div data-dojo-id="chatboardTab" data-dojo-type="dijit/layout/TabContainer" style="width: 600px;height: 100px;" doLayout="false">
        
	   </div>
    </div>
	
</div>
	
	
	<div id="invitation">
	    <div><span id="invitationSender"></span><span id="invitationRoomId"></span></div>
        <a id="refuse" class="closeInvation chatSend" onclick='ackInvitation("false")' href="javascript:void(0);">拒绝</a>
        <a id="accept" class="closeInvation chatSend" onclick='ackInvitation("true")' href="javascript:void(0);">接受</a>
    </div>
</body>
</html>