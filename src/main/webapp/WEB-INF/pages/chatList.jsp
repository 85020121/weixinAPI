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
    var layerRemainder;
    
    function clickList(id) {
    	console.log("clickList: " + id);
        $("#my-message-iframe").css("display","none");
        $(".edit-client-info-div").css("display","none");
        $(".list-group-item").removeClass("active");
        $("#my-messages").removeClass("active");
        $("#"+id).addClass("active");
        $("#"+id).find("span.badge").remove();
        var roomid = id.replace("channel-list", "chatboard");
        $(".weixin-chat-room").css("display","none"); 
        $(".weixin-history-board").css("display","none"); 
        $("#" + roomid).css("display","block");
        //var edit_info = id.replace("channel-list", "edit-client-info-div");
        //$("#" + edit_info).css("display","block");
        var chatboard = $("#"+id.replace("channel-list", "panel-body"));
        chatboard.scrollTop(chatboard[0].scrollHeight);
    }
    
    function clickHistoryList(id) {
        console.log("clickHistoryList: " + id);
        $("#my-message-iframe").css("display","none");
        $(".edit-client-info-div").css("display","none");
        $(".list-group-item").removeClass("active");
        $("#my-messages").removeClass("active");
        $("#"+id+"-chat-history").addClass("active");
        var roomid = id.replace("channel-list", "chatboard");
        $(".weixin-chat-room").css("display","none"); 
        $(".weixin-history-board").css("display","none"); 
        $("#"+id+"-historyboard").css("display","block");
    }
    
    function sendExpressMessage(message) {
    	console.log("title="+message.title);
        var id = getActiveChartboard();
        console.log("id="+id);
        if(id == null) return;
        var inputId = id.replace("chatboard","input-message");
        console.log("input = "+ "#"+id+" #"+inputId);
        $('#'+id+' #'+inputId).val(message.title);
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
                    $("#staff-menu").append("<li id='weixin-staff-checkout'><a href='javascript:void(0)' onclick='weixin_checkout(\""+person.tenant.tenantUn+"\",\""+person.id+"\")'><span class='glyphicon glyphicon-log-out'></span><span>&nbsp;&nbsp;签出</span></a></li>");
                    $("#staff-name").html(person.name);
                    if(result.isCheckedIn) {
                    	isCheckedIn = true;
                    	checkin(person);
                    }
/*                     var skills = person.skills;
                    for(var i=0;i<skills.length;i++){
                        if(i>0){
                            $("#skills-group").append('<li class="divider"></li>');
                        }
                        $("#skills-group").append('<li><a href="#">'+skills[i].name+'</a></li>');
                    } */
                    
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
                	alert(result.errmsg);
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
    
    function getHistoryList(tenantUn, openid, work_num, page) {
    	console.log("getHistoryList");
        var url = "/wx/webchat/"+tenantUn+"/"+work_num+"/getHistoryList/"+page;
        $.ajax({
            url : url,
            cache : false, 
            async : true,
            type : "GET",
            dataType : 'json',
            success : function (result){
            	console.log("length:"+result.length);
                    for(var i=0; i<result.length; i++) {
                    	var image = result[i].client_headimgurl.substring(0, result[i].client_headimgurl.length-1) + 46;
                    	var html = "<a href='javascript:void(0)' class='list-group-item' style='padding: 5px 5px;' onclick='showHistory(\""+openid+"\",\""+result[i].client_openid+"\")'><img src='"+image+"' class='img-rounded' style='height:32px'><span style='margin-left:10px;'>"+result[i].client_name+"</span><span style='margin-left:10px;font-size:10px'>"+result[i].start_time+"</span></a>";
                        $(".hitstory-client-list").append(html);
                    }
            }
        });
    }
    
    function checkin(person) {
    	$("#staff-headerimg").attr("src", person.headImgUrl.substring(0, person.headImgUrl.length-1) + 64);
        $(".staff-working-num").html("工号："+person.number);
        $(".row-offcanvas").css("display","block");
        $("#staff-status").html("已签到<span class='caret'>");
        $("#staff-status").css("background","#428bca");
        
        var takeClient = '<li><a href="#" onclick=\'takeClientFromWeb("'+person.tenant.tenantUn+'","'+person.id+'")\'><span class="glyphicon glyphicon-user"></span><span>&nbsp;&nbsp;抢接</span></a></li>';
        $("#staff-service-menu").append(takeClient);
        
        var channels = person.channels;
        var openingChannel = "";
        var temOpenid = "";
        
        for(var i=0; i<channels.length; i++){
            var channel = channels[i];
            temOpenid = channel.openId;
            weixinMessageUrl = "http://www.clouduc.cn/crm/mobile/replymessage/messagelist.php?openid="+channel.openId;
            var id = channel.openId + "-channel-list";
            var html = '<a href="javascript:void(0)" id="'+id+'" class="list-group-item staff-channel-list" onclick=\'clickList("'+id+'")\'>客服通道'+(i+1)+'<input style="display:none" value=0></a>';
            $(".channel-list").append(html);

             var chatBoard = "<div id='"+channel.openId+"-chatboard' class='panel panel-primary weixin-chat-room' style='display:none;'>";
               chatBoard += "<div id='"+channel.openId+"-panel-heading-div' class='panel-heading'>";
               chatBoard += "<h3 id='"+channel.openId+"-panel-heading' class='panel-title'>客服通道"+(i+1)+"</h3></div>";
               chatBoard += "<div id='"+channel.openId+"-panel-body' class='panel-body' style='height:350px; overflow:auto; margin-bottom;background-color: #EFF3F7;'></div>";
               chatBoard += "<div id='"+channel.openId+"-chat-editor' class='chatOperator lightBorder' style=''><div id='"+channel.openId+"-disable-input' style='width:100%;height:102%;position:absolute;opacity:0.4;background-color:black;z-index:99;display:block'></div>";
               chatBoard += "<div class='inputArea'><div class='attach'><a href='javascript:;' class='emotion func expression' title='选择表情'></a>";
               chatBoard += "<a href='javascript:;' id='"+channel.openId+"-uploader' class='func file' style='position:relative;display:block;margin:0;' title='图片文件'></a></div>";
               chatBoard += "<div class='input-group'><input type='text' id='"+channel.openId+"-account-input' style='display:none' value='"+channel.weixinId+"'><input type='text' id='"+channel.openId+"-input-message' class='form-control weixin-input-area' value='没有会话，暂时无法输入'><span class='input-group-btn'>";    
               chatBoard += "<button id='"+channel.openId+"-sendmessage' class='btn btn-primary' onclick='postMessage(\""+channel.weixinId+"\",\""+channel.openId+"\")' type='button'>发送</button><button type='button' class='btn btn-danger' onclick='endSession(\""+channel.openId+"\")' style='margin-left:10px'>结束会话</button></span></div></div></div>";
            
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
                   
            if(checkIsInSession(channel.openId) && openingChannel==""){
            	openingChannel = channel.openId;
            }
            
           
            $('.weixin-service-count-iframe').attr("src","http://www.clouduc.cn/hsy/tenant/user/staffServiceCounts?openid="+channel.openId);
        }
        
        console.log("openingChannel="+openingChannel);
        if(openingChannel != ""){
        	clickList(openingChannel + "-channel-list");
        }
        $('.weixin-service-count-div').css("display", "block");
        getHistoryList(person.tenant.tenantUn, temOpenid, person.number, 1);
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
                	$("#staff-status").html("未签到<span class='caret'>");
                    $("#staff-status").css("background","rgb(184, 55, 55)");
                    $("#staff-service-menu").empty();
                } else {
                    console.log("msg:"+result.errmgs);
                }
                
                $('.weixin-service-count-iframe').attr("src","");
                $('.weixin-service-count-div').css("display", "none");
            },
            error : function(error) {
            	console.log("error:"+error);
            }
        });
    }
    
    function checkIsInSession(openid) {
    	var url = "/wx/webchat/"+openid+"/isInSession";
    	var ret;
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            success : function (result){
                if(result.isInSession) {
                	console.log("name: "+result.clientName);
                	$('#'+openid+"-channel-list").prepend('<span class="glyphicon glyphicon-transfer">&nbsp;</span>')
                	
                	var history = result.history;

                	for(var i=0; i<history.length; i++) {
                		var message = history[i];
                		console.log("message: " + message);
                		var row;
                		if(message.sender_type == "client") {
                			row = "<div class='senderName'>"+message.time+"  "+message.sender_name+"</div><div class='chatItem you'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
                            row += "<pre style='white-space:pre-wrap'>"+ parse_content(message.content) +"</pre></div></div></div></div>";
                		} else {
                			row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
                            row += "<pre style='white-space:pre-wrap'>"+ parse_content(message.content) +"</pre></div></div></div></div>";
                		}
                		appendNewContent(openid, row);
                	}
                	
                    var image = result.clientImage.substring(0, result.clientImage.length-1) + 64;
                    var header = '<a class="pull-left" style="margin-top:-53px;margin-left:-5px;" href="javascript:void(0)">';
                    header += '<img class="media-object img-circle" src="'+image+'" style="border:2px solid white"></a>';
                    header += '<h3 class="panel-title">'+result.clientName+'<a href="javascript:void(0)" onclick=\'editClientInfo("'+result.clientOpenid+'","'+openid+'")\' style="margin-left:75%"><span class="glyphicon glyphicon-edit"></a></h3>';
                    $('#'+openid+"-panel-heading-div").empty();
                    $('#'+openid+"-panel-heading-div").append(header);
                	
                    var chatboard = $('#'+openid+'-panel-body');
                    var link = "<div class='sysmsg'><a id='"+openid+"-chat-history' onclick='getChatHistory(\""+openid+"\",2, true)' href='javascript:void(0)'>点击查看聊天记录</a></div>";
                    chatboard.prepend(link);
                	
                	var addToBoard = "<div class='sysmsg'>系统提示：您正在和客户‘"+result.clientName+"’进行对话。</div>";
                	chatboard.append(addToBoard);

                    chatboard.scrollTop(chatboard[0].scrollHeight);
                    $('#'+openid+'-disable-input').css("display","none");
                    $('#'+openid+'-input-message').val("");
                    ret = true;
                } else {
                	ret =  false;
                }
            },
            error : function(error) {
                console.log("error:"+error);
                ret = false;
            }
        });
        return ret;
    }
    
    function getExpressMessage(tenantUn) {
    	var url = "http://www.clouduc.cn/hsy/rest/n/getquickreply?tenantUn="+tenantUn;
        $.ajax({
            url : url,
            cache : false, 
            async : true,
            type : "GET",
            success : function (result){
            	$("#staff-express-message").empty();
            	for(var i=0;i<result.length;i++){
                    if(i>0){
                        $("#staff-express-message").append('<li class="divider"></li>');
                    }
                    $("#staff-express-message").append('<li><a href="javascript:void(0)" onclick="sendExpressMessage(this)" title='+result[i].replyContent+'>'+result[i].replyName+'</a></li>');
                }
            },
            error : function(error) {
                console.log("error:"+error);
            }
        });
    }
    
    function takeClientFromWeb(tenantUn, staff_id) {
        var url = "/wx/webchat/"+tenantUn+"/takeClientFromWeb/"+staff_id;
        $.ajax({
            url : url,
            cache : false, 
            async : true,
            type : "GET",
            success : function (result){
                if(!result.success){
                	alert(result.errmsg);
                }
            },
            error : function() {
            }
        });
    }
    
    function endSession(id) {
    	if(confirm("是否要结束会话？")){
    		console.log("yes");
    	} else {
    		return;
    	}
    	var url = "/wx/webchat/"+id+"/endSession";
        $.ajax({
            url : url,
            cache : false, 
            async : true,
            type : "GET",
            success : function (){
            	var chatboard = $('#'+id+'-panel-body');
            	chatboard.append("<div class='sysmsg'>系统提示：您已向客户发出结束会话请求，请等待客户反馈。</div>");
            	chatboard.scrollTop(chatboard[0].scrollHeight);
            	 
            	$('#'+id+'-disable-input').css("display","block");
            	$('#'+id+'-input-message').val("等待用户反馈，暂时无法输入");
            },
            error : function() {
            }
        });
    }
    
    function createHistoryPane(channel, openid, name, image) {
        var image_url = image.substring(0, image.length-1) + 46;
        var id = openid+"-chat-history";
        $("#"+id).remove();
        var html = "<a href='javascript:void(0)' id='"+id+"' class='list-group-item staff-channel-list' onclick='clickHistoryList(\""+openid+"\")'><img src='"+image_url+"' class='img-rounded'><span style='margin-left:10px;height:24px'>"+name+"</span></a>";
        $(".hitstory-client-list").prepend(html);
        
        var historyBoard = "<div id='"+openid+"-historyboard' class='panel panel-primary weixin-history-board' style='height:450px;'>";
        historyBoard += "<div class='panel-heading' style='text-align:center'>";
        historyBoard += "<h3 id='"+openid+"-historyboard-heading' class='panel-title'>"+name+"</h3></div>";
        historyBoard += "<div id='"+openid+"-historyboard-body' class='panel-body' style='height:430px; overflow:auto; margin-bottom;background-color: #EFF3F7;'></div></div>";
        var content = $("#"+channel+"-panel-body").html();
        console.log("content:"+content);
        $(".jumbotron").append(historyBoard);
        $("#"+openid+"-historyboard-body").append(content);
        $("#"+openid+"-historyboard-body .sysmsg").remove();
        
        var history_board = $("#"+openid+"-historyboard-body");
        history_board.scrollTop(history_board[0].scrollHeight);
        
        $(".weixin-history-board").css("display","none");
        //clickHistoryList(openid);
    }

    function getChatHistory(id, page, hasHistory){
    	var url = "/wx/webchat/"+id+"/getChatHistory/"+page;
    	var a_tag = $('#'+id+'-chat-history');
    	var chatboard = $('#'+id+'-panel-body');
    	var scrollHeight;
    	if(hasHistory){
    		scrollHeight = chatboard[0].scrollHeight;
    		console.log("childHeight: "+ scrollHeight);
    	}
    	
    	a_tag.html("<img src='../../wx/resources/images/loading.gif' />");
        $.ajax({
            url : url,
            cache : false, 
            async : false,
            type : "GET",
            success : function (result){
            	if(result.length > 0) {
            		for(var i=result.length-1; i>=0; i--) {
            			var message = result[i];
                        console.log("message: " + message);
                        var row;
                        if(message.sender_type == "client") {
                            row = "<div class='senderName'>"+message.time+"  "+message.sender_name+"</div><div class='chatItem you'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
                            row += "<pre style='white-space:pre-wrap'>"+ parse_content(message.content) +"</pre></div></div></div></div>";
                        } else {
                            row = "<div class='chatItem me'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
                            row += "<pre style='white-space:pre-wrap'>"+ parse_content(message.content) +"</pre></div></div></div></div>";
                        }
                        chatboard.prepend(row);
            		}
                    a_tag.remove();
                    var link = "<div class='sysmsg'><a id='"+id+"-chat-history' onclick='getChatHistory(\""+id+"\", "+(page+1)+", true)' href='javascript:void(0)'>查看更早的聊天记录</a></div>";
                    chatboard.prepend(link);

            	} else {
            		console.log("No history");
            		a_tag.remove();
                    chatboard.prepend("<div class='sysmsg'>没有聊天记录</div>");
            	}
            },
            error : function(error) {
                console.log("error:"+error);
                a_tag.remove();
                chatboard.prepend("<div class='sysmsg'>没有聊天记录</div>");
            }
        });
        
        if(hasHistory){
        	scrollHeight = chatboard[0].scrollHeight - scrollHeight;
        	chatboard.scrollTop(scrollHeight);
            console.log("childHeight: "+ scrollHeight);
        }
    }
    
    function editClientInfo(clientOpenid, staffOpenid) {
    	var div = $("#"+staffOpenid+"-edit-client-info-div");
    	if(div.css("display") == "block") {
    		div.css("display","none");
    		return;
    	}
        if(div.css("display") == "none") {
            div.css("display","block");
            return;
        }
    	var url = 'http://www.clouduc.cn/hsy/mobile/tenant/prospectsinfo_register.jsp?prospectsWXId='+clientOpenid+'&openid='+ staffOpenid;
        var edit_div = '<div id="'+staffOpenid+'-edit-client-info-div" class="embed-responsive embed-responsive-16by9 edit-client-info-div" style="min-height: 450px">';
        edit_div += '<iframe class="embed-responsive-item" onload="this.height=this.contentWindow.document.documentElement.scrollHeight" style="border-radius:4px;" src="'+url+'"></iframe></div>';
        $(".edit-client-info-container").append(edit_div);
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
                console.log("JsonData="+data.data);
                for (var i in data.data) 
                	console.log("'" + i + "':" + data.data[i]); 
				var room =  data.channelId;
                var chatBoardId = room+"-chatboard";
				if(data.msgtype == "sysMessage"){
					console.log("action: "+data.action);
					if(data.action == "takeClient") {
						console.log("takeClient");
						$('#'+room+"-panel-body").empty();
                        clickList(room+"-channel-list");
                        $('#'+room+"-channel-list").prepend('<span class="glyphicon glyphicon-transfer">&nbsp;</span>')
                        $('#'+room+'-disable-input').css("display","none");
                        $('#'+room+'-input-message').val("");
                        //var addToBoard = "<div class='sysmsg'><a id='"+room+"-chat-history' onclick='getChatHistory(\""+room+"\")' href='#'>点击查看聊天记录<input value='1' style='display:none' /></a></div>";
                        getChatHistory(room, 1, false);
                        var addToBoard = "<div class='sysmsg'>"+data.content+"</div>";
                        appendNewContent(room, addToBoard);
                        var image = data.data.clientImage.substring(0, data.data.clientImage.length-1) + 64;
                        var header = '<a class="pull-left" style="margin-top:-53px;margin-left:-5px;" href="javascript:void(0)">';
                        header += '<img class="media-object img-circle" src="'+image+'" style="border:2px solid white"></a>';
                        header += '<h3 class="panel-title">'+data.data.clientName+'<a href="javascript:void(0)" onclick=\'editClientInfo("'+data.data.clientOpenid+'","'+room+'")\' style="margin-left:75%"><span class="glyphicon glyphicon-edit"></a></h3>';
                        $('#'+room+"-edit-client-info-div").remove();
                        $('#'+room+"-panel-heading-div").empty();
                        $('#'+room+"-panel-heading-div").append(header);
					} else if(data.action == "takeClientFromMobile") {
						console.log("takeClientFromMobile");
						$(".xubox_layer").remove();
						var chat_panel = $('#'+room+"-panel-body");
						chat_panel.empty();
                        clickList(room+"-channel-list");
                        $('#'+room+"-channel-list").prepend('<span class="glyphicon glyphicon-transfer">&nbsp;</span>')
                        $('#'+room+'-disable-input').css("display","none");
                        $('#'+room+'-input-message').val("");
                        getChatHistory(room, 1, false);
                        chat_panel.scrollTop(chat_panel[0].scrollHeight);
                        var image = data.data.clientImage.substring(0, data.data.clientImage.length-1) + 64;
                        var header = '<a class="pull-left" style="margin-top:-53px;margin-left:-5px;" href="javascript:void(0)">';
                        header += '<img class="media-object img-circle" src="'+image+'" style="border:2px solid white"></a>';
                        header += '<h3 class="panel-title">'+data.data.clientName+'</h3>';
                        header += '<span class="glyphicon glyphicon-edit">';
                        $('#'+room+"-edit-client-info-div").remove();
                        $('#'+room+"-panel-heading-div").empty();
                        $('#'+room+"-panel-heading-div").append(header);
                    }  else if(data.action == "endSession") {
						console.log("endSession");
						$('#'+room+"-channel-list").find("span.glyphicon").remove();
                        var addToBoard = "<div class='sysmsg'>"+data.content+"</div>";
                        //createHistoryPane(room, data.data.clientOpenid, data.data.clientName, data.data.clientImage);
                        appendNewContent(room, addToBoard);
                        $('#'+room+'-disable-input').css("display","block");
                        $('#'+room+'-input-message').val("无活动会话，暂时无法输入");
					} else if(data.action == "webCheckin") {
						mobile_checkin();
					} else if(data.action == "webCheckout") {
						isCheckedIn = false;
	                    $(".staff-channel-list").remove();
	                    $(".weixin-chat-room").remove();
	                    $(".row-offcanvas").css("display","none");
	                    $("#staff-status").html("未签到<span class='caret'>");
	                    $("#staff-status").css("background","rgb(184, 55, 55)");
                    } else if(data.action == "reactiveClient") {
                    	$('#'+room+'-disable-input').css("display","none");
                        $('#'+room+'-input-message').val("");
                        var d = new Date();
                        row = "<div class='senderName'>"+d.getHours()+":"+d.getMinutes()+"  "+data.senderName+"</div><div class='chatItem you'><div class='cloud cloudText'><div class='cloudBody'><div class='cloudContent'>";
                        row += "<pre style='white-space:pre-wrap'>"+parse_content(data.content)+"</pre></div></div></div></div>";
                        appendNewContent(room, row);
                    }
					
				} else if(data.msgtype == "staffService"){
                    console.log("staffService: "+ room);
                    if($(".xubox_layer").length>0) {
                    	console.log("staffService blocked");
                    	getMessage(stff_uuid);
                    	return;
                    }
                    $.layer({
                        shade: [0],
                        area: ['auto','auto'],
                        dialog: {
                            msg: data.content,
                            btns: 2,                    
                            type: 4,
                            btn: ['确定','忽略'],
                            yes: function(index){
                            	layerRemainder = index;
                            	$.ajax({
                                        url : "/wx/webchat/"+data.channelId+"/takeClient",
                                        cache : false, 
                                        async : true,
                                        type : "GET",
                                        dataType : 'json',
                                        success : function(data) {
                                        	if(!data.success) {
                                                console.log("take client failed: "+ data.msg);
                                        		alert(data.msg);
                                        	}
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
	      <a class="navbar-brand" href="javascript:void(0)"></a>
	    </div>
	
	    <!-- Collect the nav links, forms, and other content for toggling -->
	    <div class="collapse navbar-collapse" id="bs-example-navbar-collapse-1">
	      <ul class="nav navbar-nav">
	        <li class="dropdown">
              <a href="javascript:void(0)" id="staff-status" class="dropdown-toggle" data-toggle="dropdown" style="background: rgb(184, 55, 55);color:white" >未签到<span class="caret"></span></a>
              <ul id="staff-menu" class="dropdown-menu" style="min-width:50px" role="menu">
                <li><a id="staff-checkin-button" href="javascript:void(0)" onclick="weixin_checkin()"><span class='glyphicon glyphicon-log-in'></span><span>&nbsp;&nbsp;签到</span></a></li>
                <li class="divider"></li>
              </ul>
            </li>
	        <li><a href="javascript:void(0)" id="staff-name"></a></li>
<!-- 	        <li class="dropdown">
	          <a href="#" class="dropdown-toggle" data-toggle="dropdown">技能组<span class="caret"></span></a>
	          <ul id="skills-group" class="dropdown-menu" style="min-width:50px" role="menu">
	          </ul>
	        </li> -->
	        <li class="dropdown">
              <a href="javascript:void(0)" class="dropdown-toggle" data-toggle="dropdown">客服菜单<span class="caret"></span></a>
              <ul id="staff-service-menu" class="dropdown-menu" style="min-width:50px" role="menu">
              </ul>
            </li>
            <li class="dropdown">
              <a href="javascript:void(0)" class="dropdown-toggle" data-toggle="dropdown">常用语<span class="caret"></span></a>
              <ul id="staff-express-message" class="dropdown-menu" style="min-width:50px" role="menu">
              </ul>
            </li>
	      </ul>
	      <ul class="nav navbar-nav navbar-right">
	        <li>
                <div class="embed-responsive embed-responsive-16by9 weixin-service-count-div" style="width: 400px;height:50px;padding-bottom:0;margin-right:20px;display:none">
                  <iframe class="embed-responsive-item weixin-service-count-iframe" scrolling="no" src=""></iframe>
                </div>
            </li>
            <li><a href="javascript:void(0)" title="注销"><span class="glyphicon glyphicon-off"></span></a></li>
	      </ul>
	    </div><!-- /.navbar-collapse -->
	  </div><!-- /.container-fluid -->
	</nav>
    <div class="container" style="background-color:#6B747A;padding: 40px 0px 0;">
      <div class="row row-offcanvas row-offcanvas-right" style="display:none;">

        <div class="col-xs-12 col-sm-3 sidebar-offcanvas" id="sidebar" role="navigation">
		  
		  <div class="panel panel-primary">
			  <div class="panel-heading">
			    <a class="pull-left" style="margin-top:-53px;margin-left:-5px;" href="javascript:void(0)">
                  <img id="staff-headerimg" class="media-object img-circle" src="" style="border:2px solid white">
                </a>
			    <h3 class="panel-title staff-working-num"></h3>
			  </div>
			  <div class="panel-body">
			    <div class="list-group channel-list">
                </div>
                <div class="list-group">
                    <a href="javascript:void(0)" id="my-messages" class="list-group-item">我的留言</a>
                </div>
			  </div>
		  </div>
		  
		  <div class="panel panel-primary" style="max-height:400px">
              <div class="panel-heading">
                <h3 class="panel-title">历史会话</h3>
              </div>
              <div class="panel-body">
                <div class="list-group hitstory-client-list">
                </div>
              </div>
          </div>
		  
          
        </div><!--/span-->

        <div class="col-xs-12 col-sm-6">
          <div class="jumbotron" style="padding-top:0;padding:0;" >
          
            <div id="my-message-iframe" class="embed-responsive embed-responsive-16by9" style="display:none;min-height: 450px">
                <iframe class="embed-responsive-item weixin-message-list" onload="this.height=this.contentWindow.document.documentElement.scrollHeight" scrolling="no" style="border-radius:4px;" src=""></iframe>
            </div>
            
          </div>
        </div><!--/span-->

         <div class="col-xs-12 col-sm-3">
          <div class="jumbotron edit-client-info-container" style="padding-top:0;padding:0;" >
          
<!--             <div id="edit-client-info-div" class="embed-responsive embed-responsive-16by9" style="display:none;min-height: 450px">
                <iframe class="embed-responsive-item edit-client-info-iframe" onload="this.height=this.contentWindow.document.documentElement.scrollHeight" style="border-radius:4px;" src=""></iframe>
            </div> -->
            
          </div>
        </div> 
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
    $(".edit-client-info-div").css("display","none");
    $(".weixin-history-board").css("display","none"); 
    $("#my-message-iframe").css("display","block");
    $(".list-group-item").removeClass("active");
    $("#my-messages").addClass("active");
});

</script>
</body>
</html>