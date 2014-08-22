<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>

<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>和声云客服平台</title>
<link rel="icon" href="<c:url value='/resources/images/hesong_logo.jpg'/>" type="image/jpg" />

<link rel="stylesheet" href="<c:url value='/resources/css/chatTab.css'/>" />
<link rel="stylesheet" href="<c:url value='/resources/css/emoji184f03.css'/>" />
<link rel="stylesheet" href="<c:url value='/resources/css/qqemoji.css'/>" />
<link href="<c:url value='/resources/js/bootstrap-3.2.0-dist/css/bootstrap.min.css'/>" rel="stylesheet">
<link href="<c:url value='/resources/js/offcanvas/offcanvas.css'/>" rel="stylesheet">
<link rel="stylesheet" href="<c:url value='/resources/css/sticky-footer-navbar.css'/>" />

<script src="<c:url value='/resources/js/jquery-1.11.0.min.js'/>"></script>    
<script src="<c:url value='/resources/js/vlcPlayer.js'/>"></script>    
<script src="<c:url value='/resources/js/qqemoji.js'/>"></script>  
<script src="<c:url value='/resources/js/upclick-min.js'/>"></script>    
<script src="<c:url value='/resources/js/guid.js'/>"></script>    
<script src="<c:url value='/resources/js/layer/layer.min.js'/>"></script>  
<script src="<c:url value='/resources/js/bootstrap-3.2.0-dist/js/bootstrap.min.js'/>"></script>  
<script src="<c:url value='/resources/js/offcanvas/offcanvas.js'/>"></script>  
<script src="<c:url value='/resources/js/jquery.cookie.js'/>"></script>    

<script>

    var isWindowOnFocus = true;
    var titleMessageRemaind;
    var weixinMessageUrl = "";
    var isCheckedIn = false;
    
    function clickList(id) {
    	console.log("clickList: " + id);
        $("#my-message-iframe").css("display","none");
        $(".list-group-item").removeClass("active");
        $("#my-messages").removeClass("active");
        $("#"+id).addClass("active");
        $("#"+id).find("span.badge").remove();
        var roomid = id.replace("channel-list", "chatboard");
        $(".weixin-chat-room").css("display","none"); 
        $("#" + roomid).css("display","block");
    }
    
    function getStaffInfo() {
    	$(".staff-channel-list").remove();
        $(".weixin-chat-room").remove();
        $(".row-offcanvas").css("display","none");
    	var url = "/wx/webchat/"+$.cookie('WX_STF_UID')+"/getStaffInfo";
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            success : function (result){
                if(result.success) {
                    var person = result.person;
                    $(".navbar-brand").html(person.tenant.tenantName);
                    $("#weixin-staff-checkout").remove();
                    $("#staff-menu").append("<li id='weixin-staff-checkout'><a href='#' onclick='weixin_checkout(\""+person.tenant.tenantUn+"\",\""+person.id+"\")'><span class='glyphicon glyphicon-log-out'></span><span>&nbsp;&nbsp;签出</span></a></li>");
                    $("#staff-name").html(person.name);
                    if(result.isCheckedIn) {
                    	isCheckedIn = true;
                    	checkin(person);
                    }
                    var skills = person.skills;
                    for(var i=0;i<skills.length;i++){
                        if(i>0){
                            $("#skills-group").append('<li class="divider"></li>');
                        }
                        $("#skills-group").append('<li><a href="#">'+skills[i].name+'</a></li>');
                    }
                    
                    getExpressMessage(person.tenant.tenantUn);
                    
                    getMessage(person.id);
                } else {
                    console.log("msg:"+result.msg);
                    window.location.href = "/wx/warning";
                }
            },
            error : function(error) {
                console.log("error:"+error);
                window.location.href = "/wx/warning";
            }
        });
    }
    
    function weixin_checkin() {
    	if(isCheckedIn) {
    	    alert("您已经签到了!");
    	    return;
    	}
    	
    	console.log("weixin_checkin");
    	console.log("checkin: " + $.cookie('WX_STF_UID'));
        var url = "/wx/webchat/"+$.cookie('WX_STF_UID')+"/checkin";
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            dataType : 'json',
            success : function (result){
                if(result.success) {
                	isCheckedIn = true;
                	var person = result.person;
                    checkin(person);
                } else {
                	$(".row-offcanvas").css("display","none");
                }
            }
        });
    }
    
    function mobile_checkin() {
        console.log("mobile_checkin");
        console.log("checkin: " + $.cookie('WX_STF_UID'));
        var url = "/wx/webchat/"+$.cookie('WX_STF_UID')+"/checkin";
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            dataType : 'json',
            success : function (result){
                if(result.success) {
                    isCheckedIn = true;
                    var person = result.person;
                    checkin(person);
                } else {
                    $(".row-offcanvas").css("display","none");
                }
            }
        });
    }
    
    function checkin(person) {
    	$("#staff-headerimg").attr("src", person.headImgUrl.substring(0, person.headImgUrl.length-1) + 64);
        $(".staff-working-num").html("工号："+person.number);
        $(".row-offcanvas").css("display","block");
        $("#staff-status").html("已签到");
        $("#staff-status").css("background","#428bca");
        
        var channels = person.channels;
        for(var i=0; i<channels.length;i++){
            var channel = channels[i];
            weixinMessageUrl = "http://www.clouduc.cn/crm/mobile/replymessage/messagelist.php?openid="+channel.openId;
            var id = channel.openId + "-channel-list";
            var html = '<a href="#" id="'+id+'" class="list-group-item staff-channel-list" onclick=\'clickList("'+id+'")\'>客服通道'+(i+1)+'<input style="display:none" value=0></a>';
            $(".channel-list").append(html);
            
             var chatBoard = "<div id='"+channel.openId+"-chatboard' class='panel panel-primary weixin-chat-room' style='display:none;'>";
               chatBoard += "<div class='panel-heading' style='text-align:center'>";
               chatBoard += "<h3 id='"+channel.openId+"-panel-heading' class='panel-title'>客服通道"+(i+1)+"</h3></div>";
               chatBoard += "<div id='"+channel.openId+"-panel-body' class='panel-body' style='height:350px; overflow:auto; margin-bottom;background-color: #EFF3F7;'></div>";
               chatBoard += "<div id='"+channel.openId+"-chat-editor' class='chatOperator lightBorder' style='display:none'>";
               chatBoard += "<div class='inputArea'><div class='attach'><a href='javascript:;' class='emotion func expression' title='选择表情'></a>";
               chatBoard += "<a href='javascript:;' id='"+channel.openId+"-uploader' class='func file' style='position:relative;display:block;margin:0;' title='图片文件'></a></div>";
               chatBoard += "<div class='input-group'><input type='text' id='"+channel.openId+"-account-input' style='display:none' value='"+channel.weixinId+"'><input type='text' id='"+channel.openId+"-input-message' class='form-control weixin-input-area'><span class='input-group-btn'>";    
               chatBoard += "<button id='"+channel.openId+"-sendmessage' class='btn btn-primary' onclick='postMessage(\""+channel.weixinId+"\",\""+channel.openId+"\")' type='button'>发送</button></span></div></div></div>";
            
                $(".jumbotron").append(chatBoard);
                $('.emotion').qqFace();
                uploadImage(channel.openId);
                   
                $('.weixin-message-list').attr("src", weixinMessageUrl);
                
                   $(".weixin-input-area").keyup(function(event){
                       if(event.keyCode == "13")    
                       {
                           var id = this.id.replace("-input-message","");
                           var account = $("#"+id+"-account-input").val();
                           postMessage(account,id);
                       }
                   });
        }
    }

    function weixin_checkout(tenantUn, staff_uuid) {
        var url = "/wx/webchat/"+tenantUn+"/checkout/"+staff_uuid;
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            success : function (result){
                if(result.errcode ==0) {
                	isCheckedIn = false;
                    $(".staff-channel-list").remove();
                    $(".weixin-chat-room").remove();
                	$(".row-offcanvas").css("display","none");
                	$("#staff-status").html("未签到");
                    $("#staff-status").css("background","rgb(184, 55, 55)");
                } else {
                    console.log("msg:"+result.errmgs);
                }
            },
            error : function(error) {
            	console.log("error:"+error);
            }
        });
    }
    
    function getExpressMessage(tenantUn) {
    	var url = "http://www.clouduc.cn/sua/rest/n/getquickreply?tenantUn="+tenantUn;
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            success : function (result){
            	for(var i=0;i<result.length;i++){
                    if(i>0){
                        $("#staff-express-message").append('<li class="divider"></li>');
                    }
                    $("#staff-express-message").append('<li><a href="#" title='+result[i].replyContent+'>'+result[i].replyName+'</a></li>');
                }
            },
            error : function(error) {
                console.log("error:"+error);
            }
        });
    }

	
	// 接收消息，并根据消息类型修改客户端聊天窗口
	function getMessage(stff_uuid) {
		$.ajax({
			url : "/wx/webchat/"+stff_uuid+"/getMessages",
			cache : false, 
            async : true,
            type : "GET",
            dataType : 'json',
            success : function (data) {
				console.log("Msttype:"+data.msgtype + " data.channelId:"+data.channelId+"  action: "+data.action);
				var room =  data.channelId;
                var chatBoardId = room+"-chatboard";
				if(data.msgtype == "sysMessage"){
					console.log("action: "+data.action);
					if(data.action == "takeClient") {
						console.log("takeClient");
                        clickList(room+"-channel-list");
                        $('#'+room+"-channel-list").prepend('<span class="glyphicon glyphicon-transfer">&nbsp;</span>')
                        $('#'+room+"-chat-editor").css("display","block");
                        var addToBoard = "<div class='sysmsg'>"+data.content+"</div>";
                        appendNewContent(room, addToBoard);
					} else if(data.action == "endSession") {
						console.log("endSession");
						$('#'+room+"-channel-list").find("span.glyphicon").remove();
                        $('#'+room+"-chat-editor").css("display","none");
                        var addToBoard = "<div class='sysmsg'>"+data.content+"</div>";
                        appendNewContent(room, addToBoard);
					} else if(data.action == "webCheckin") {
						alert("您已经从手机端签入！");
						mobile_checkin();
					} else if(data.action == "webCheckout") {
						alert("您已经从手机端签出！");
						isCheckedIn = false;
	                    $(".staff-channel-list").remove();
	                    $(".weixin-chat-room").remove();
	                    $(".row-offcanvas").css("display","none");
	                    $("#staff-status").html("未签到");
	                    $("#staff-status").css("background","rgb(184, 55, 55)");
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
                            	$.ajax({
                                        url : "/wx/webchat/"+data.channelId+"/takeClient",
                                        cache : false, 
                                        async : true,
                                        type : "GET",
                                        dataType : 'text',
                                        success : function(data) {
                                        	//$('#'+chatBoardId).css("display","block");
                                        	//$('#'+room+"-channel-list").addClass("active");
                                        	//clickList(room);
                                        	//$('#'+room+"-channel-list").prepend('<span class="glyphicon glyphicon-transfer">&nbsp;</span>')
                                        	//$('#'+room+"-chat-editor").css("display","block");
                                        },
                                        error : function(error) {
                                        }
                                    });
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
					if($.cookie("WX_STF_UID") == data.senderName){
						row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
			            row += "<pre style='white-space:pre-wrap'>"+messageContentDiv+"</pre></div></div></div></div>";
					} else {
						var d = new Date();
						row = "<div class='senderName'>"+d.getHours()+":"+d.getMinutes()+"  "+data.senderName+"</div><div class='chatItem you'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
						row += "<pre style='white-space:pre-wrap'>"+messageContentDiv+"</pre></div></div></div></div>";
					}
					appendNewContent(room, row);
				}
				
				console.log("isWindowOnFocus"+isWindowOnFocus);
				if(!isWindowOnFocus){
					console.log("remaind");
					titleRemaind();
				}
				newMessageRemaind(room);
				getMessage(stff_uuid);
			},
			error : function(error) {
                getMessage(stff_uuid);
			}
		});
	}

	// 发送消息
	function postMessage(account, openid) {
		var input = $("#"+openid+"-input-message");
		var message = input.val();
		console.log("Message to send: "+message);
	    if(message==""){
	    	return;
	    }
	    
	    var json = '{"account":"'+account+'","content":"'+message+'","channelId":"'+openid+'","msgtype":"text","senderName":"'+$.cookie("WX_STF_UID")+'"}';
	    console.log("json: "+json);
	    $.ajax({
				url : "/wx/webchat/"+$.cookie("WX_STF_UID")+"/sendWeixinMsg",
				type: 'POST',
				cache : false, 
                async : false,
				data : json,
				success : function(data) {
					console.log("res:" + data);
					var messageContent = parse_content(message);
					var row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
                    row += "<pre style='white-space:pre-wrap'>"+messageContent+"</pre></div></div></div></div>";
                    appendNewContent(openid, row);
				},
				error : function(erorr) {
					console.log("error:" + error);
				}
		});
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
	   var uploader = uploaderId+'-uploader';
	   var account = $.cookie('WX_STF_UID');
	   var guid;
	   upclick(
	     {
	      element: uploader,
	      action: '/wx/webchat/'+account+'/upload', 
	      action_params : {"roomId":uploaderId},
	      onstart:
	        function(filename)
	        {
	          guid = get_guid();
	          console.log('Start upload: '+filename);
	          var row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
              row += "<pre id='"+guid+"'style='white-space:pre-wrap'><img src='../../wx/resources/images/loading.gif' /></pre></div></div></div></div>";
              appendNewContent(uploaderId, row);
	        },
	      oncomplete:
	        function(response_data) 
	        {
	    	  console.log("response_data"+response_data);
	          if(response_data=="Failed"){
	        	  $('#'+guid).html("<span style='color:red'>系统提示：发送图片失败！</span>");
	          } else {
	        	  $('#'+guid).html("<img width='240' src='"+response_data+"' />");
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
	  
	 function titleRemaind() {
         var inter = false;
         titleMessageRemaind = setInterval(function() {
             if (inter) {
                 document.title = '和声云客服平台';
                 inter = false;
             } else {
                 document.title = '新消息';
                 inter = true;
             }
         }, 2000);
	 }
	 
	 function newMessageRemaind(id) {
		 var channel = $("#"+id+"-channel-list");
		 if(!isWindowOnFocus || !channel.hasClass("active")) {
			 if(channel.find("span").hasClass("badge")) {
				 var i = parseInt(channel.find("span.badge").html());
				 channel.find("span.badge").html(i+1);
			 } else {
				 channel.append('<span class="badge">1</span>');
			 }
		 } 
	 }

	$(document).ready(function() {
		
		window.onblur = function() {
			isWindowOnFocus = false;
		};
		window.onfocus = function() {
			clearInterval(titleMessageRemaind);
			isWindowOnFocus = true;
			document.title = '和声云客服平台';
		};
		
/* 		window.onbeforeunload = function(e) {
			console.log("refresh");
			
             $.ajax({
                  url : "/wx/webchat/refresh",
                  cache : false, 
                  async : false,
                  type : "GET",
                  success : function (result){
                  },
                  error : function(error) {
                  }
              });
		} */
		
 		$(window).bind('beforeunload',function(){
		    return "注意：离开页面时数据会丢失！";
		}); 
		
		//weixin_checkin();
		getStaffInfo();
		
	});
</script> 

</head>
<body style="background:#6B747A;padding-top:0px;min-height:400px;font-family: 微软雅黑;">
	<nav class="navbar navbar-inverse" role="navigation" style="border-radius:0px;border:0px">
	  <div class="container-fluid">
	    <!-- Brand and toggle get grouped for better mobile display -->
	    <div class="navbar-header">
	      <a class="navbar-brand" href="#"></a>
	    </div>
	
	    <!-- Collect the nav links, forms, and other content for toggling -->
	    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
	      <ul class="nav navbar-nav">
	        <li class="active"><a href="#"><span class="glyphicon glyphicon-cloud"></span></a></li>
	        <li><a href="#" id="staff-name"></a></li>
	        <li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown">技能组<span class="caret"></span></a>
	          <ul id="skills-group" class="dropdown-menu" style="min-width:50px" role="menu">
	          </ul>
	        </li>
	        <li class="dropdown">
              <a href="#" class="dropdown-toggle" data-toggle="dropdown">客服菜单<span class="caret"></span></a>
              <ul id="staff-menu" class="dropdown-menu" style="min-width:50px" role="menu">
                <li><a id="staff-checkin-button" "href="#" onclick="weixin_checkin()"><span class='glyphicon glyphicon-log-in'></span><span>&nbsp;&nbsp;签到</span></a></li>
                <li class="divider"></li>
              </ul>
            </li>
            <li class="dropdown">
              <a href="#" class="dropdown-toggle" data-toggle="dropdown">常用语<span class="caret"></span></a>
              <ul id="staff-express-message" class="dropdown-menu" style="min-width:50px" role="menu">
              </ul>
            </li>
	      </ul>
	      <ul class="nav navbar-nav navbar-right">
	        <li class="active"><a id="staff-status" href="#" style="background: rgb(184, 55, 55);" >未签到</a></li>
            <li><a href="#"><span class="glyphicon glyphicon-off"></span></a></li>
	      </ul>
	    </div><!-- /.navbar-collapse -->
	  </div><!-- /.container-fluid -->
	</nav>
    <div class="container" style="background-color:#6B747A;padding: 40px 15px 0;">
      <div class="row row-offcanvas row-offcanvas-right" style="display:none;">

        <div class="col-xs-6 col-sm-3 sidebar-offcanvas" id="sidebar" role="navigation">
		  
		  <div class="panel panel-primary">
			  <div class="panel-heading">
			    <a class="pull-left" style="margin-top:-53px;margin-left:-5px;" href="#">
                  <img id="staff-headerimg" class="media-object img-circle" src="" style="border:2px solid white">
                </a>
			    <h3 class="panel-title staff-working-num"></h3>
			  </div>
			  <div class="panel-body">
			    <div class="list-group channel-list">
                </div>
                <div class="list-group">
                    <a href="#" id="my-messages" class="list-group-item">我的留言</a>
                </div>
			  </div>
			</div>
		  
          
        </div><!--/span-->

        <div class="col-xs-12 col-sm-9">
          <div class="jumbotron" style="padding-top:0;padding:0" >
          
            <div id="my-message-iframe" class="embed-responsive embed-responsive-16by9" style="display:none;">
                <iframe class="embed-responsive-item weixin-message-list" style="border-radius:4px;" src=""></iframe>
            </div>
            
          </div>
        </div><!--/span-->


      </div><!--/row-->

    </div>
    
    <div class="footer" style="background:#6B747A;">
      <div class="container">
        <p class="text-muted" style="color:rgb(182, 182, 182);text-align:center">CopyRight  2013-2014  和声云 版权所有    All Rights Reserved . 京ICP备14034791号</p>
      </div>
    </div>
<script>
$("#my-messages").click(function(){
    $('.weixin-message-list').attr("src", weixinMessageUrl);
	$(".weixin-chat-room").css("display","none");
    $("#my-message-iframe").css("display","block");
    $(".list-group-item").removeClass("active");
    $("#my-messages").addClass("active");
});

</script>
</body>
</html>