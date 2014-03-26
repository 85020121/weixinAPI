<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<script src="<c:url value='/resources/js/upclick-min.js'/>"></script>    

<script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js"></script>
</head>
<body>

 <a href="javascript:;"  id="uploader" onclick="uploadImage('uploader')">up </a>
  <script type="text/javascript">
  function uploadImage(uploaderId){
	  console.log('uploaderId:'+uploaderId);
   var uploader = document.getElementById(uploaderId);
   console.log("uploader:"+uploader);
   upclick(
     {
      element: uploader,
      action: '/path_to/you_server_script.php', 
      onstart:
        function(filename)
        {
          alert('Start upload: '+filename);
        },
      oncomplete:
        function(response_data) 
        {
          alert(response_data);
        }
     });
  }
  uploadImage('uploader');

   </script>
</body>
</html>