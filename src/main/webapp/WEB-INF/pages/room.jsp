<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<% String user =(String)session.getAttribute("sender"); %>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>聊天室</title>
<script type="text/javascript" src="http://code.jquery.com/jquery-1.11.0.min.js"></script>
<script type="text/javascript">
    $(function(){
        (function getMessages(){
            $.ajax({
                dataType: "json",
                url: 'getMessages',
                cache: false,
                success: function(data){
                    var v = $('#text').val();
                    v += '\r\n' + data.date + ' ' + data.sender + '：' + data.content;
                    $('#text').val(v);
                }
            }).always(function(){
                getMessages();
            });
        })();
        $('#send').click(function(event){
            event.preventDefault();
            var values = $(this).val();
            console.info("value:"+values);
            $.post('setMessage', values, function(data){
                $('#form>[name=content]').val('');
            }, 'json');
        });
        $('#logout').click(function(){
            $.ajax({
                dataType: "json",
                url: 'logout',
                cache: false,
                success: function(data){
                    window.location.href = 'index.jsp';
                }
            });
        });
    });
</script>
</head>
<body>
欢迎：<%=user %><br/>
<textarea id="text" rows="20" style="width: 500;"></textarea>
<form id="form" method="post">
<input type="text" name="content" />
<input id="send" value="发送" type="submit"/>
<input id="logout" value="离开" type="button"/>
</form>


</body>
</html>