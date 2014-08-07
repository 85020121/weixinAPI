<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<link rel="stylesheet" href="<c:url value='/resources/css/weiWeb.css'/>" />
<script type="text/javascript"
	src="http://ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js"></script>
</head>
<body>
	<form action="#" method="post">

		<div>
			<label for="name">Text Input</label> <input type="text" name="name"
				id="name" value="" tabindex="1" />
		</div>

		<div>
			<label for="textarea">Textarea</label>
			<textarea cols="40" rows="8" name="textarea" id="textarea"></textarea>
		</div>

	</form>

</body>
</html>