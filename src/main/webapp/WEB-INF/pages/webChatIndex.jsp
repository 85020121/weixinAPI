<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Weixin IM</title>

<script src="<c:url value='/resources/js/jquery-1.11.0.min.js'/>"></script>    
<script src="<c:url value='/resources/js/jquery.cookie.js'/>"></script>    
<script src="<c:url value='/resources/js/layer/layer.min.js'/>"></script>    

<script>
var global_layer;

function weixin_checkin(staff_uuid) {
    var url = "/weixinAPI/webchat/"+staff_uuid+"/checkin";
    var ret = false;
    $.ajax({
        url : url,
        cache : false, 
        async : false,
        type : "GET",
        dataType : 'json',
        success : function (result){
            if(result.errcode == 0) {
                ret = true;
            }
        }
    });
    return ret;
}

function weixin_checkout(tenantUn, staff_uuid) {
    var url = "/weixinAPI/webchat/"+tenantUn+"/checkout/"+staff_uuid;
    var ret = false;
    $.ajax({
        url : url,
        cache : false, 
        async : false,
        type : "GET",
        dataType : 'json',
        success : function (result){
            if(result.errcode ==0) {
                ret = true;
            }
        }
    });
    return ret;
}

// 进入聊天室
function enterChatRoom(tenantUn, staff_uuid) {
	console.log("enterChatRoom");
	var url = "/weixinAPI/webchat/"+tenantUn+"/enterChatRoom/"+staff_uuid;
	global_layer = $.layer({
        type: 2,
        maxmin: true,
        shadeClose: false,
        title: '微信 IM',
        shade: [0.1,'#fff'],
        offset: ['0px','0px'],
        area: ['620px', '410px'],
        iframe: {src: url},
        closeBtn: [0, false]
    }); 
}
</script>

</head>
<body>

</body>
</html>