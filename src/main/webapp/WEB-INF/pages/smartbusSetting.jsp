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

	function connect() {
		var save = dojo.byId("connect");
		dojo.on(save, "click", function() {

			var host = dojo.byId("host").value;
			var port = dojo.byId("port").value;
            var unitId = dojo.byId("unitId").value;
            var clientId = dojo.byId("clientId").value;
			console.log("host: " + host + " port: " + port
					+ " unitId: " + unitId + " clientId"+clientId);

			var queryStr;
			require([ "dojo/io-query" ], function(ioQuery) {
				var query = {
					smartbus : dojo.toJson({
						"host" : host,
						"port" : port,
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
				url : "/weChatAdapter/smartbus/connect",
				postData : queryStr,
				handleAs : "text",
				load : function(data) {
					console.log("res:" + data);
					callback.innerHTML = "Smartbus connection: "+data;
				},
				error : function(eror) {
					console.log("error:" + error);
                    callback.innerHTML = "Smartbus connection: "+error;
				}
			}
			var deferred = dojo.xhrPost(xhrArgs);
		});
	}

	dojo.ready(function() {
		connect();

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
			<td><label>Smartbus host:</label></td>
			<td><input type="text" id="host" name="host" required="true"
				value="${host}" data-dojo-type="dijit/form/ValidationTextBox" /></td>
		</tr>
		<tr>
			<td><label>Port:</label></td>
			<td><input type="text" id="port" name="port" required="true"
				data-dojo-type="dijit/form/ValidationTextBox" value="${port}" /></td>
		</tr>

		<tr>
			<td><label>Client unit ID:</label></td>
			<td><input type="text" id="unitId" name="unitId" required="true"
				data-dojo-type="dijit/form/ValidationTextBox" value="${unitId}" /></td>
		</tr>

		<tr>
			<td><label>Client ID:</label></td>
			<td><input type="text" id="clientId" name="clientId"
				required="true" data-dojo-type="dijit/form/ValidationTextBox"
				value="${clientId}" /></td>
		</tr>
	</table>

	<button id="connect" name="connect">Connect</button>
	<h4 id="callback"></h4>
</body>
</html>