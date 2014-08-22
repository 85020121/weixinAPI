<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
  <head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

  </head>

  <body>


    <!-- Bootstrap core JavaScript
    ================================================== -->
    <!-- Placed at the end of the document so the pages load faster -->
    <script src="<c:url value='/resources/js/jquery-1.11.0.min.js'/>"></script>    
    <script src="<c:url value='/resources/js/jquery.cookie.js'/>"></script>    
    
    <script>
    $(document).ready(function() {
    	console.log($.cookie('WX_STF_UID'));
    	$.post("/wx/webchat", {staff_uuid : $.cookie('WX_STF_UID')}, function() { window.location.href = "/wx/webchat"; }); 
    })
    
    </script>  
  </body>
</html>