<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
  <head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<title>和声云客服平台--座席登陆</title>
	<link rel="icon" href="<c:url value='/resources/images/hesong_logo.jpg'/>" type="image/jpg" />

    <!-- Bootstrap core CSS -->
    <link href="<c:url value='/resources/js/bootstrap-3.2.0-dist/css/bootstrap.min.css'/>" rel="stylesheet">
    <link rel="stylesheet" href="<c:url value='/resources/css/signin.css'/>" />
    <link rel="stylesheet" href="<c:url value='/resources/css/sticky-footer-navbar.css'/>" />

  </head>

  <body>

    <div class="container">

      <div class="form-signin">
        <input id="email" type="email" class="form-control" placeholder="账户" required autofocus>
        <input id="password" type="password" class="form-control" placeholder="密码" required>
        <div id="warning" class="alert alert-danger" role="alert" style="display:none">注意：账户或密码错误！</div>
        <button class="btn btn-lg btn-primary btn-block" onclick="login()">登陆</button>
      </div>

    </div> <!-- /container -->

    <div class="footer" style="background:#eee;">
      <div class="container">
        <p class="text-muted" style="color:#333;text-align:center">CopyRight  2013-2014  和声云 版权所有    All Rights Reserved . 京ICP备14034791号</p>
      </div>
    </div>

    <!-- Bootstrap core JavaScript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="<c:url value='/resources/js/jquery-1.11.0.min.js'/>"></script>    
    <script src="<c:url value='/resources/js/bootstrap-3.2.0-dist/js/bootstrap.min.js'/>"></script>
    
    <script>
        function login() {
        	$("#warning").css("display","none");
        	var email = $("#email").val();
        	var password = $("#password").val();
        	console.log(email + "  " + password);
        	$.ajax({
                url : "/wx/webchat/loginRequest",
                type: 'POST',
                data : '{"account":"'+email+'","password":"'+password+'"}',
                dataType : "json",
                success : function(data) {
                    if(data.success) {
                    	//window.location.href = "/wx/webchat";
                    	$.post("/wx/webchat", {staff_uuid : data.person.id}, function() { window.location.href = "/wx/webchat"; });
                    } else {
                    	$("#warning").css("display","block");
                    }
                },
                error : function(erorr) {
                    console.log("error:" + error);
                    $("#warning").css("display","block");
                }
            });
        }
    
    </script>  
  </body>
</html>