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
<link href="<c:url value='/resources/js/bootstrap-3.2.0-dist/css/bootstrap.min.css'/>" rel="stylesheet">
<link href="<c:url value='/resources/js/offcanvas/offcanvas.css'/>" rel="stylesheet">

<script src="<c:url value='/resources/js/libs/dojo/1.9.1/dojo/dojo.js'/>"></script>    
<script src="<c:url value='/resources/js/jquery-1.11.0.min.js'/>"></script>    
<script src="<c:url value='/resources/js/vlcPlayer.js'/>"></script>    
<script src="<c:url value='/resources/js/qqemoji.js'/>"></script>  
<script src="<c:url value='/resources/js/upclick-min.js'/>"></script>    
<script src="<c:url value='/resources/js/guid.js'/>"></script>    
<script src="<c:url value='/resources/js/layer/layer.min.js'/>"></script>  
<script src="<c:url value='/resources/js/bootstrap-3.2.0-dist/js/bootstrap.min.js'/>"></script>  
<script src="<c:url value='/resources/js/offcanvas/offcanvas.js'/>"></script>  


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
    
    function clickList(id) {
        console.log("id: " + id);
        $(".list-group-item").removeClass("active");
        $("#"+id).addClass("active");
        var roomid = id.replace("channel-list", "chatboard");
        $(".weixin-chat-room").css("display","none"); 
        $("#" + roomid).css("display","block");
        console.log("id: " + roomid);
    }
    
    function weixin_checkin(staff_uuid) {
        var url = "/weixinAPI/webchat/"+staff_uuid+"/checkin";
        var ret;
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            dataType : 'json',
            success : function (result){
                if(result.errcode == 0) {
                    ret = true;
                } else {
                	ret = result.errmsg;
                }
            }
        });
        return ret;
    }

    function weixin_checkout(tenantUn, staff_uuid) {
        var url = "/weixinAPI/webchat/"+tenantUn+"/checkout/"+staff_uuid;
        var ret;
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            dataType : 'json',
            success : function (result){
                if(result.errcode ==0) {
                    ret = true;
                } else {
                    ret = result.errmsg;
                }
            }
        });
        return ret;
    }

    
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
                			   console.log("channel: " + channel);
                			   var id = channel.openid + "-channel-list";
                			   var html = '<a href="#" id="'+id+'" class="list-group-item" onclick=\'clickList("'+id+'")\'>客服通道'+(i+1)+'</a>';
                			   $(".list-group").append(html);
                			   
                		        var chatBoard = "<div id='"+channel.openid+"-chatboard' class='panel panel-primary weixin-chat-room' style='display:none;'>";
                		          chatBoard += "<div class='panel-heading' style='text-align:center'>";
                		          chatBoard += "<h3 id='"+channel.openid+"-panel-heading' class='panel-title'>客服通道"+(i+1)+"</h3></div>";
                		          chatBoard += "<div id='"+channel.openid+"-panel-body' class='panel-body' style='height:350px; overflow:auto; margin-bottom;background-color: #EFF3F7;'></div>";
                		          chatBoard += "<div id='chat-editor' class='chatOperator lightBorder'>";
                		          chatBoard += "<div class='inputArea'><div class='attach'><a href='javascript:;' class='emotion func expression' title='选择表情'></a>";
                		          chatBoard += "<a href='javascript:;' id='"+channel.openid+"-uploader' class='func file' style='position:relative;display:block;margin:0;' title='图片文件'></a></div>";
                		          chatBoard += "<div class='input-group'><input type='text' id='"+channel.openid+"-account-input' style='display:none' value='"+channel.account+"'><input type='text' id='"+channel.openid+"-input-message' class='form-control weixin-input-area'><span class='input-group-btn'>";    
                		          chatBoard += "<button id='"+channel.openid+"-sendmessage' class='btn btn-primary' onclick='postMessage(\""+channel.account+"\",\""+channel.openid+"\")' type='button'>发送</button></span></div></div></div>";
                			   
                		            $(".jumbotron").append(chatBoard);
                		            $('.emotion').qqFace();
                		              uploadImage(channel.openid);
                		              
                		              $(".weixin-input-area").keyup(function(event){
                		                  if(event.keyCode == "13")    
                		                  {
                                              var id = this.id.replace("-input-message","");
                                              var account = $("#"+id+"-account-input").val();
                                              postMessage(account,id);
                		                  }
                		              });
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
				console.log("Msttype:"+data.msgtype + " data.channelId:"+data.channelId);
				var room =  data.channelId;
                var chatBoardId = room+"-chatboard";
				if(data.msgtype == "sysMessage"){
						var addToBoard = "<div class='sysmsg'>"+data.content+"</div>";
						appendNewContent(room, addToBoard);
				} else if(data.msgtype == "exitedRoom"){
                    console.log("Exited from room: "+room);
                    if(dojo.cookie("WX_STF_UID") == data.sender) {
                        var roomContentPane = dijit.byId(escaped_room+"contentPaneId");
                        chatboardTab.removeChild(roomContentPane);
                        roomContentPane.destroy();
                    } else {
                        var addToBoard = "<div class='sysmsg'>系统消息: "+data.sender+" 离开房间</div>";
                        appendNewContent(room, addToBoard);
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
                		messageContentDiv = "<img width='240' src='"+data.content+"' />";
                		//messageContentDiv = "[收到一条图片消息，请在手机上查看]";
                	} else if(data.msgtype == "voice"){
                		var d = new Date();
                		var uid = d.getMinutes()+d.getSeconds();
                		//messageContentDiv = "<input type='button' value='播放语音' onclick='return play(\"ftp://root:3*)(%40faso@183.61.81.71/../"+data.content+"\");'/>";
                		messageContentDiv = "[收到一条语音消息，请在手机上查看]";
                	} else {
                		messageContentDiv = parse_content(data.content);
                	}
					console.log("data: "+ data.channelId + ' ' + data.date + ' ' + data.senderName + ' ' + messageContentDiv);
					
					var row;
					if(dojo.cookie("WX_STF_UID") == data.senderName){
						row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
			            row += "<pre style='white-space:pre-wrap'>"+messageContentDiv+"</pre></div></div></div></div>";
					} else {
						var d = new Date();
						row = "<div class='senderName'>"+d.getHours()+":"+d.getMinutes()+"  "+data.senderName+"</div><div class='chatItem you'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
						row += "<pre style='white-space:pre-wrap'>"+messageContentDiv+"</pre></div></div></div></div>";
					}
					appendNewContent(room, row);
				}
				if(weixin_long_poll_flag){
				    getMessage();
				}
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
		var input = $("#"+openid+"-input-message");
		var message = input.val();
		console.log("Message to send: "+message);
	    if(message==""){
	    	return;
	    }
			var xhrArgs = {
				url : "/weixinAPI/webchat/"+dojo.cookie("WX_STF_UID")+"/sendWeixinMsg",
				postData : dojo.toJson({"account" : account,
				                        "content" : message,
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
			input.val("");
	}
	
	// 创建聊天窗口，设置房间名
	function createChannelTab(num, account, openid){
		  var d = new Date();
		  
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
	          uploadImage(openid+'-uploader');
	}
	
	function appendNewContent(id, content) {
		var panel_body = $("#" +id + "-panel-body");
        panel_body.append(content);
        panel_body.scrollTop(panel_body[0].scrollHeight);
	}
	
	function parseEmojiToChName(i){
	    var emoji = qqemoji_ch[i];
	    if(emoji==null)return;
	    var openid = getActiveChartboard();
	    
	    if(openid == null)return;
	    openid = openid.replace("chatboard","");
	    console.log("openid: " + openid);
	    var input = $("#"+openid+"input-message");
	    console.log("input:" + input);
	    //var text = $(inputAreaId).val(); // 
	    var text = input.val();
	    console.log("text:" + text + "  " + emoji);
	    input.val(text+emoji);
	}
	
	  function uploadImage(uploaderId){
	   console.log('uploaderId:'+uploaderId);
	   var uploader = document.getElementById(uploaderId+'-uploader');
	   var account = dojo.cookie("WX_STF_UID");
	   var guid;
	   console.log("uploader:"+uploader);
	   upclick(
	     {
	      element: uploader,
	      action: '/weixinAPI/webchat/'+account+'/upload', 
	      action_params : {"roomId":uploaderId},
	      onstart:
	        function(filename)
	        {
	          guid = get_guid();
	          console.log('Start upload: '+filename);
	          var row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
              row += "<pre id='"+guid+"'style='white-space:pre-wrap'><img src='../../resources/images/loading.gif' /></pre></div></div></div></div>";
              appendNewContent(uploaderId, row);
	        },
	      oncomplete:
	        function(response_data) 
	        {
	    	  console.log("response_data"+response_data);
	          if(response_data=="Failed"){
	        	  dojo.byId(guid).innerHTML = "<span style='color:red'>系统提示：发送图片失败！</span>";
	          } else {
	        	  dojo.byId(guid).innerHTML = "<img width='240' src='"+response_data+"' />";
	          }
	        }
	     });
	  }
	  
	  function getActiveChartboard() {
		  var id;
		  $("div.weixin-chat-room").each(function(){
			  if(this.style.display == 'block') {
				  id = this.id;
			  }
		  });
		  return id;
	  }

	dojo.ready(function() {
		//getMessage();
		//window.onbeforeunload = exitRoom;
		enterChatRoom();
		//weixin_checkin("ff808081471526a0014719da8c450007");
		dojo.parser.parse();
		
    });
	

</script> 

</head>
<body style="background:#6B747A;padding-top:0px">
	<nav class="navbar navbar-inverse" role="navigation" style="margin-bottom:70px;border-radius:0px;border:0px">
	  <div class="container-fluid">
	    <!-- Brand and toggle get grouped for better mobile display -->
	    <div class="navbar-header">
	      <a class="navbar-brand" href="#">Brand</a>
	    </div>
	
	    <!-- Collect the nav links, forms, and other content for toggling -->
	    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
	      <ul class="nav navbar-nav">
	        <li class="active"><a href="#">Link</a></li>
	        <li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown">技能组 <span class="caret"></span></a>
	          <ul id="skills-group" class="dropdown-menu" role="menu">
	            <li><a href="#">售前</a></li>
	            <li><a href="#">售后</a></li>
	            <li class="divider"></li>
	            <li><a href="#">接客</a></li>
	          </ul>
	        </li>
	      </ul>
	      <ul class="nav navbar-nav navbar-right">
	        <li><a href="#">Link</a></li>
	        <li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown">Dropdown <span class="caret"></span></a>
	          <ul  class="dropdown-menu" role="menu">
	            <li><a href="#">Action</a></li>
	            <li><a href="#">Another action</a></li>
	            <li><a href="#">Something else here</a></li>
	            <li class="divider"></li>
	            <li><a href="#">Separated link</a></li>
	          </ul>
	        </li>
	      </ul>
	    </div><!-- /.navbar-collapse -->
	  </div><!-- /.container-fluid -->
	</nav>

    <div class="container" style="background-color:#6B747A;">

      <div class="row row-offcanvas row-offcanvas-right">

        <div class="col-xs-6 col-sm-3 sidebar-offcanvas" id="sidebar" role="navigation">
		  
		  <div class="panel panel-primary">
			  <div class="panel-heading">
			    <a class="pull-left" style="margin-top:-48px;margin-left:-5px;" href="#">
                  <img id="staff-headerimg" class="media-object img-circle" src="http://wx.qlogo.cn/mmopen/WWxicToNQlgxvdF4V3yM5IncQQjXk7pPgkaeglBxcRg1lHwcREca2OdMxhn6biaoT8qDz2mL8ibvQxVnvZfxpicQLPIvtnagxnKA/64" alt="...">
                </a>
			    <h3 class="panel-title">Bowen</h3>
			  </div>
			  <div class="panel-body">
			    <div class="list-group">
                </div>
			  </div>
			</div>
		  
          
        </div><!--/span-->

        <div class="col-xs-12 col-sm-9">
          <div class="jumbotron" style="padding-top:0;padding:0" >

            
            
<!--              <div class="panel panel-primary">
			  <div class="panel-heading" style="text-align:center">
			    <h3 class="panel-title">Panel title</h3>
			  </div>
			  <div class="panel-body" id="escaped_roomId" style='height:350px; overflow:auto; margin-bottom;background-color: #EFF3F7;'>
                    asdasda<br>
                    asdasda<br>
               </div>
                
                <div id='chat_editor' class='chatOperator lightBorder'>
                <div class='inputArea'><div class='attach'><a href='javascript:;' class='emotion func expression' title='选择表情'></a>
                    <a href='javascript:;' id="escaped_roomIduploader" class='func file' style='position:relative;display:block;margin:0;' title='图片文件'></a>
                    </div>
                    <div class="input-group">
				      <input type="text" class="form-control">
				      <span class="input-group-btn">
				        <button class="btn btn-primary" onclick='postMessage("account","openid")' type="button">发送</button>
				      </span>
				    </div>
                </div>
			  </div>
			</div>  -->
			
          </div>
        </div><!--/span-->


      </div><!--/row-->

    </div>

</body>
</html>