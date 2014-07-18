<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" href="<c:url value='/resources/css/chatTab.css'/>" />
<link rel="stylesheet" href="<c:url value='/resources/css/emoji184f03.css'/>" />
<link rel="stylesheet" href="<c:url value='/resources/css/qqemoji.css'/>" />
    <link rel="stylesheet" href="<c:url value='/resources/js/libs/dojo/1.9.1/dijit/themes/claro/claro.css'/>" />

<script src="<c:url value='/resources/js/libs/dojo/1.9.1/dojo/dojo.js'/>"></script>    
<script src="<c:url value='/resources/js/jquery-1.11.0.min.js'/>"></script>    
<script src="<c:url value='/resources/js/vlcPlayer.js'/>"></script>    
<script src="<c:url value='/resources/js/qqemoji.js'/>"></script>  
<script src="<c:url value='/resources/js/upclick-min.js'/>"></script>    
<script src="<c:url value='/resources/js/guid.js'/>"></script>    
<script src="<c:url value='/resources/js/layer/layer.min.js'/>"></script>    

<script>
    weixin_long_poll_flag = true;

	dojo.require("dojo.on");
    dojo.require("dojo.io-query");
    dojo.require("dojo.query");
    dojo.require("dojo.aspect");
    dojo.require("dojo.dom");
    dojo.require("dojo.cookie");
    dojo.require("dojo.parser");
    dojo.require("dijit.layout.TabContainer");
    dojo.require("dijit.layout.ContentPane");
    dojo.require("dijit.Dialog");
    dojo.require("dijit.registry");
    dojo.require("dijit.form.Textarea");
    dojo.require("dojo.Deferred");
    
    
    // 进入聊天室
    function enterChatRoom() {
        var xhrArgs = {
                url : "/weixinAPI/webchat/"+dojo.cookie("WX_STF_UID")+"/channelList",
                handleAs : "json",
                load : function(data) {
                	console.log("ChannelList:"+data);
                	if(data!=null){
                		   for(var i=0; i<data.length;i++){
                			   var channel = data[i];
                			   createChannelTab(i+1, channel.account, channel.openid);
                		   }
                		   getMessage();
                	}
                },
                error : function(error) {
                }
            }
            dojo.xhrGet(xhrArgs);
    }
	
	// 尝试退出房间
	function exitRoom(room) {
		var xhrArgs = {
	            url : "/weixinAPI/webchat/"+dojo.cookie("WX_STF_UID")+"/exit_room",
	            postData : dojo.toJson({"roomId" : room}),
	            handleAs : "text",
	            load : function(data) {
	            },
	            error : function(error) {
	            }
	        }
	        dojo.xhrPost(xhrArgs);
	}
	

	// 接收消息，并根据消息类型修改客户端聊天窗口
	function getMessage() {
	var xhrArgs = {
			url : "/weixinAPI/webchat/getMessages",
			handleAs : "json",
			load : function(data) {
				console.log("Msttype: "+data.msgtype);
				var room =  data.channelId;
				var escaped_room = escape(room);
                var chatBoardId = escaped_room+"chatboard";
				if(data.msgtype == "invitation"){
					dojo.byId("invitationSender").innerHTML = data.sender + " 邀请你加入房间：";
                    dojo.byId("invitationRoomId").innerHTML = room;
					ShowMessage(180,100);
					//ackInvitation(true);
				} else if(data.msgtype == "sysMessage"){
					console.log("Entered in to room: "+room);
					if(dojo.cookie("WX_STF_UID") == data.sender) {
						   createRoomTab(room);
					} else {
						var addToBoard = "<div class='sysmsg'>"+data.content+"</div>";
		                  require(["dojo/dom-construct"], function(domConstruct){
	                          domConstruct.place(addToBoard, chatBoardId);
	                        });
	                    dojo.byId(chatBoardId).scrollTop = dojo.byId(chatBoardId).scrollHeight;
	                    //text += '\r\n' + data.date + ' ' + data.sender + '：' + data.content;
	                    //dojo.byId(room+"chatboard").innerHTML = text;
	                    var receiverTabId = escaped_room+"contentPaneId";
	                    if(chatboardTab.selectedChildWidget.id != receiverTabId){
	                        newMessageRemaind(receiverTabId);
	                    }
					}
				} else if(data.msgtype == "exitedRoom"){
                    console.log("Exited from room: "+room);
                    if(dojo.cookie("WX_STF_UID") == data.sender) {
                        var roomContentPane = dijit.byId(escaped_room+"contentPaneId");
                        chatboardTab.removeChild(roomContentPane);
                        roomContentPane.destroy();
                    } else {
                        var addToBoard = "<div class='sysmsg'>系统消息: "+data.sender+" 离开房间</div>";
                        require(["dojo/dom-construct"], function(domConstruct){
                            domConstruct.place(addToBoard, chatBoardId);
                          });
                      dojo.byId(chatBoardId).scrollTop = dojo.byId(chatBoardId).scrollHeight;
                      var receiverTabId = escaped_room+"contentPaneId";
                      if(chatboardTab.selectedChildWidget.id != receiverTabId){
                          newMessageRemaind(receiverTabId);
                      }
                    }
                } else if(data.msgtype == "staffService"){
                    console.log("staffService: "+ room);
                    $.layer({
                        shade: [0],
                        area: ['auto','auto'],
                        dialog: {
                            msg: data.content,
                            btns: 2,                    
                            type: 4,
                            btn: ['确定','忽略'],
                            yes: function(index){
                            	var xhrArgs = {
                                        url : "/weixinAPI/webchat/"+data.channelId+"/takeClient",
                                        handleAs : "text",
                                        load : function(data) {
                                        },
                                        error : function(error) {
                                        }
                                    }
                                dojo.xhrGet(xhrArgs);
                            	layer.close(index);
                            	// $(".xubox_title").html()
                            }, no: function(){
                            }
                        }
                    });
                } else {
                	var messageContentDiv;
                	if(data.msgtype == "image") {
                		messageContentDiv = "<img width='240' src='ftp://root:3*)(%40faso@183.61.81.71/../"+data.content.replace("/weixin","")+"' />";
                	} else if(data.msgtype == "voice"){
                		var d = new Date();
                		var uid = d.getMinutes()+d.getSeconds();
                		messageContentDiv = "<input type='button' value='播放语音' onclick='return play(\"ftp://root:3*)(%40faso@183.61.81.71/../"+data.content+"\");'/>";
                	} else {
                		messageContentDiv = parse_content(data.content);
                	}
					console.log("data: "+ data.channelId + ' ' + data.date + ' ' + data.senderName + '：' + messageContentDiv);
					
					var row;
					if(dojo.cookie("WX_STF_UID") == data.senderName){
						row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
			            row += "<pre style='white-space:pre-wrap'>"+messageContentDiv+"</pre></div></div></div></div>";
					} else {
						var d = new Date();
						row = "<div class='senderName'>"+d.getHours()+":"+d.getMinutes()+"  "+data.senderName+"</div><div class='chatItem you'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
						row += "<pre style='white-space:pre-wrap'>"+messageContentDiv+"</pre></div></div></div></div>";
					}
					require(["dojo/dom-construct"], function(domConstruct){
						  domConstruct.place(row, chatBoardId);
						});
					dojo.byId(chatBoardId).scrollTop = dojo.byId(chatBoardId).scrollHeight;
					//text += '\r\n' + data.date + ' ' + data.sender + '：' + data.content;
					//dojo.byId(room+"chatboard").innerHTML = text;
					var receiverTabId = escaped_room+"contentPaneId";
					if(chatboardTab.selectedChildWidget.id != receiverTabId){
						newMessageRemaind(receiverTabId);
					}
				}
				if(weixin_long_poll_flag){
				    getMessage();
				}
// 				setTimeout(function() {
//                   getMessage();
//               }, 1000);
			},
			error : function(error) {
				console.info("error: " + error);
                setTimeout(function() {
                	if(weixin_long_poll_flag){
                        getMessage();
                    }
                }, 1000);
			}
		}
		dojo.xhrGet(xhrArgs);
	}

	// 发送消息
	function postMessage(account, openid) {
		var input = dojo.byId(escape(openid+"messageInput"));
	    if(input.value==""){
	    	return;
	    }
			var xhrArgs = {
				url : "/weixinAPI/webchat/"+dojo.cookie("WX_STF_UID")+"/sendWeixinMsg",
				postData : dojo.toJson({"account" : account,
				                        "content" : input.value,
				                        "channelId" : openid,
			                            "msgtype" : "text",
			                            "senderName" : dojo.cookie("WX_STF_UID")
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
			input.value = "";
	}
	
	// 创建聊天窗口，设置房间名
	function createChannelTab(num, account, openid){
		  var d = new Date();
		  var escaped_roomId = escape(openid);
		  
/*           var script = "<script type='text/javascript'>";
          script += "var uploader = document.getElementById('uploader');console('uploder:'+uploader)";
          script += "upclick({element: uploader,action: '/path_to/you_server_script.php', onstart:function(filename){alert('Start upload: '+filename);},";
          script += "oncomplete:function(response_data){alert(response_data);}});";
          script += "<\/script>"; */
		  
		  var chatContent = "<div id='"+escaped_roomId+"chatboard' style='height:250px; border:1px solid black; overflow:auto; margin-bottom;background-color: #EFF3F7;'>";
		  chatContent += "<div class='time'> <span class='timeBg left'></span> "+d.getHours()+":"+d.getMinutes()+" <span class='timeBg right'></span></div></div>";
		    chatContent += "<div id='chat_editor' class='chatOperator lightBorder'>";
		    chatContent += "<div class='inputArea'><div class='attach'><a href='javascript:;' class='emotion func expression' title='选择表情'></a>"
		    chatContent += "<a href='javascript:;' id='"+escaped_roomId+"uploader' class='func file' style='position:relative;display:block;margin:0;' title='图片文件'></a>";
		    chatContent += "</div><input type='text' id='"+escaped_roomId+"messageInput' class='chatInput lightBorder'></input>";
		    chatContent += "<a href='javascript:;' class='chatSend' onclick='postMessage(\""+account+"\",\""+openid+"\")' id='"+escaped_roomId+"sendMessage'><b>发送</b></a></div></div>"

		    
		    var closablePane = new dijit.layout.ContentPane({
		            title : "客服通道"+num,
		            closable : false,
		            content : chatContent,
		            id : escaped_roomId+"contentPaneId",
		            onClose : function() {
		            // confirm() returns true or false, so return that.
		              var close =  confirm("Do you really want to Close this?");
		              if(close){
		            	exitRoom(openid);
		            	//chatboardTab.removeChild(this);
		            	//this.destroy();
		            	return true;
		              } else {
		            	console.log("Not close");
		            	return false;
		              }
		            }
		        });
		    closablePane.domNode.setAttribute("widgetId", escaped_roomId);
		    chatboardTab.addChild(closablePane);
	          $('.emotion').qqFace();
	          uploadImage(escaped_roomId+'uploader');
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
	
	function parseEmojiToChName(i){
	    var emoji = qqemoji_ch[i];
	    if(emoji==null)return;
	    var inputAreaId = chatboardTab.selectedChildWidget.id.replace("contentPaneId","messageInput");
	    //var text = $(inputAreaId).val(); // 
	    var text = dojo.byId(inputAreaId).value;
	    dojo.byId(inputAreaId).value = text+emoji;
	}
	
	  function uploadImage(uploaderId){
	   console.log('uploaderId:'+uploaderId);
	   var uploader = document.getElementById(uploaderId);
	   var chatboardId = uploaderId.replace("uploader","chatboard");
	   var account = dojo.cookie("WX_STF_UID");
	   var guid;
	   console.log("uploader:"+uploader);
	   upclick(
	     {
	      element: uploader,
	      action: '/weixinAPI/webchat/'+account+'/upload', 
	      action_params : {"roomId":unescape(uploaderId).replace("uploader","")},
	      onstart:
	        function(filename)
	        {
	          guid = get_guid();
	          console.log('Start upload: '+filename);
	          var row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
              row += "<pre id='"+guid+"'style='white-space:pre-wrap'><img src='../../resources/images/loading.gif' /></pre></div></div></div></div>";
              require(["dojo/dom-construct"], function(domConstruct){
                  domConstruct.place(row, chatboardId);
                });
            dojo.byId(chatboardId).scrollTop = dojo.byId(chatboardId).scrollHeight;
	        },
	      oncomplete:
	        function(response_data) 
	        {
	    	  console.log("response_data"+response_data);
	          if(response_data=="Failed"){
	        	  dojo.byId(guid).innerHTML = "<span style='color:red'>发送图片失败</span>";
	          } else {
	        	  dojo.byId(guid).innerHTML = "<img width='240' src='ftp://root:3*)(%40faso@183.61.81.71/../"+response_data.replace("/weixin","")+"' />";
	          }
	        }
	     });
	  }

	dojo.ready(function() {
		//getMessage();
		//window.onbeforeunload = exitRoom;
		enterChatRoom();
		//weixin_checkin("ff808081471526a0014719da8c450007");
		dojo.parser.parse();
        dojo.aspect.after(chatboardTab, "selectChild", function (event) {
            console.log("You selected ", chatboardTab.selectedChildWidget.id);
            dijit.byId(chatboardTab.selectedChildWidget.id).controlButton.attr("style", "color: black;");
       });
    });
	

</script> 

</head>
<body class="claro">
    <div data-dojo-id="chatboardTab" data-dojo-type="dijit/layout/TabContainer" style="width: 580px;height: 400px;" doLayout="false">
    </div>
    
</body>
</html>