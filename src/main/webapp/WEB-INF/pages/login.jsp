<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <meta name="author" content="">
    <link rel="icon" href="../../favicon.ico">

    <title>座席登陆</title>

    <!-- Bootstrap core CSS -->
    <link href="<c:url value='/resources/js/bootstrap-3.2.0-dist/css/bootstrap.min.css'/>" rel="stylesheet">
    <link rel="stylesheet" href="<c:url value='/resources/css/signin.css'/>" />




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
        	$("#warning").css("display","block");
        }
    
    </script>  
  </body>
</html>