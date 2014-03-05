<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />


<script>
	dojoConfig = {
		parseOnLoad : true
	}
</script>
<link rel="stylesheet"
	href="<c:url value='/resources/js/libs/dojo/1.9.1/dijit/themes/claro/claro.css'/>" />

<script src="//ajax.googleapis.com/ajax/libs/dojo/1.9.1/dojo/dojo.js">
	
</script>
<script>
	dojo.require("dijit/form/ValidationTextBox");
	dojo.require("dojo.on");
	dojo.require("dojo.io-query");

	function sendText() {
		var sendText = dojo.byId("sendText");
		dojo.on(sendText, "click", function() {

			var request = dojo.byId("request").value;
			var unitId = dojo.byId("unitId").value;
			var clientId = dojo.byId("clientId").value;
			console.log("request: " + request + " unitId: "
					+ unitId + " clientId" + clientId);

			var queryStr;
			require([ "dojo/io-query" ], function(ioQuery) {
				var query = {
					smartbus : dojo.toJson({
						"request" : request,
						"unitId" : unitId,
						"clientId" : clientId
					})
				};
				// Assemble the new uri with its query string attached.
				queryStr = ioQuery.objectToQuery(query);

			});
			console.log("queryStr: " + queryStr);
			var callback = dojo.byId("callback");
			var xhrArgs = {
				url : "/weChatAdapter/smartbus/send/do",
				postData : queryStr,
				handleAs : "text",
				load : function(data) {
					console.log("res:" + data);
					callback.innerHTML = "Smartbus send text: " + data;
				},
				error : function(eror) {
					console.log("error:" + error);
					callback.innerHTML = "Smartbus send text: " + error;
				}
			}
			var deferred = dojo.xhrPost(xhrArgs);
		});
	}

	dojo.ready(function() {
		sendText();

	});
</script>
</head>
<body class="claro">

	<script type="dojo/on" data-dojo-event="reset">
        return confirm('Press OK to reset widget values');
    </script>

	<script type="dojo/on" data-dojo-event="submit">
        if(this.validate()){
            return confirm('Form is valid, press OK to submit');
        }else{
            alert('Form contains invalid data.  Please correct first');
            return false;
        }
        return true;
    </script>
	<h4>Smartbus setting</h4>
	<table style="border: 1px solid #9f9f9f;">
		<tr>
			<td><label>Dest Client unit ID:</label></td>
			<td><input type="text" id="unitId" name="unitId" required="true"
				data-dojo-type="dijit/form/ValidationTextBox" value="${unitId}" /></td>
		</tr>

		<tr>
			<td><label>Dest Client ID:</label></td>
			<td><input type="text" id="clientId" name="clientId"
				required="true" data-dojo-type="dijit/form/ValidationTextBox"
				value="${clientId}" /></td>
		</tr>
		<tr>
			<td><label>Request:</label></td>
			<td><input type="text" id="request" name="request"
				required="true" data-dojo-type="dijit/form/ValidationTextBox"
				value="${port}" /></td>
		</tr>
	</table>

	<button id="sendText" name="sendText">Send</button>
	<h4 id="callback"></h4>
</body>
</html>