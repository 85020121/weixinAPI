<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport"
	content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no">
<title>申请安装服务</title>

<link rel="stylesheet"
	href="http://code.jquery.com/mobile/1.4.2/jquery.mobile-1.4.2.min.css">
<script src="http://code.jquery.com/jquery-1.11.0.min.js"></script>
<script
	src="http://code.jquery.com/mobile/1.4.2/jquery.mobile-1.4.2.min.js"></script>


<script src="<c:url value='/resources/js/jquery.cityselect.js'/>"></script>

<script
	src="<c:url value='/resources/js/mobiscroll.custom-2.6.2.min.js'/>"></script>
<link rel="stylesheet"
	href="<c:url value='/resources/css/mobiscroll.custom-2.6.2.min.css'/>" />

<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
<!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
<!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
      <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->

<script type="text/javascript">
	//初始化日期控件设置样式
	$(document).ready(function() {
		
		//初始化日期控件
		var opt = {
			preset : 'date', //日期
			theme : 'android-ics light', //皮肤样式
			display : 'modal', //显示方式 
			mode : 'scroller', //日期选择模式
			dateFormat : 'yy-mm-dd', // 日期格式
			setText : '确定', //确认按钮名称
			cancelText : '取消', //取消按钮名籍我
			dateOrder : 'yymmdd', //面板中日期排列格式
			dayText : '日',
			monthText : '月',
			yearText : '年', //面板中年月日文字
			endYear : 2020,
            //stepMinute: 10,
            //timeFormat: 'HH:ii',
            //timeWheels: 'HHii',
            //hourText: '时',
            //minuteText: '分'
		//结束年份
		};
		$('#buy-date').mobiscroll().date();
		$('#buy-date').mobiscroll(opt);
		
	    $('#appoint-date').mobiscroll().date();
	    $('#appoint-date').mobiscroll(opt);

		$("#jquery-city").citySelect();
		
		$("#send-form").click(function(){
			console.log("aaa");
			  alert("逗你玩！！！");
		});
	});
</script>
</head>
<body>

	<div data-role="page">

		<div data-role="header" style="background:white">
		    <img alt="" style="width:70%" src="/weixinAPI/resources/image/tcl_logo.png">
			<p style="margin:10px;text-align:center;">尊敬的用户，感谢您在网上申请安装服务，请详细登记您的个人信息和服务请求，我们将尽快与您联系解决，谢谢！</p>
		</div>

		<div data-role="content">
		  <div data-role="basic-info">
		  <div data-role="header" style="background:red">
		    <h4 style="clear:both;color:white;text-shadow:0 0">基本信息</h4>
		  </div>
		  <br>
			<label for="fname" class="ui-hidden-accessible">姓名：</label>
			<input type="text" name="fname" id="fname" placeholder="输入您的真实姓名">
			
			<select name="sex" id="sex" data-role="slider">
                <option value="男">男</option>
                <option value="女">女</option>
            </select>
            
            <label for="mobileCall" class="ui-hidden-accessible">手机号码</label>
            <input type="text" name="mobileCall" id="mobileCall" placeholder="手机号码">
            
            <label for="phoneCall" class="ui-hidden-accessible">联系电话</label>
            <input type="text" name="phoneCall" id="phoneCall" placeholder="座机(需带区号)：010-8888888">
            
            <label for="email" class="ui-hidden-accessible">联系电话</label>
            <input type="text" name="email" id="email" placeholder="电子邮件">
            
            <label for="city">所在地区</label>
            <div id="jquery-city">
	            <select id="prov" data-inline="true"></select>  
	            <select id="city" data-inline="true"></select> 
	            <select id="dist" data-inline="true"></select>
            </div>
            
            <input type="text" name="address-detail" id="address-detail" placeholder="详细地址:如乡镇、街道、门牌号等">
          </div> <!-- End of basic-info -->
          <br>
          
          <div data-role="item-info">
            <div data-role="header" style="background:red">
                <h4 style="clear:both;color:white;text-shadow:0 0">产品信息</h4>
            </div>

            <div>
	            <label for="catalog" data-inline="true">产品类别:&nbsp
	            <select name="catalog" id="catalog" data-inline="true">
	              <option value="" disabled selected>请选择</option>
			      <option value="冰箱">冰箱</option>
			      <option value="洗衣机">洗衣机</option>
			      <option value="空调">空调</option>
			      <option value="电脑">电脑</option>
	              <option value="小家电">小家电</option>
	              <option value="数码">数码</option>
	              <option value="电脑">手机</option>
	              <option value="小家电">通讯</option>
	              <option value="数码">其他</option>
	            </select>
	            </label>
            </div>
            
            <label for="item-model">产品型号</label>
            <input type="text" name="产品型号" id="产品型号">
            
            <div data-role="buy-date" data-inline="true" >
                <!-- <label for="buy-date">购买日期 </label> -->
                <input type="text" data-role="datebox" id="buy-date" name="buy-date" placeholder="购买日期" />
		    </div>
		    
		    <select name="garantie" id="garantie" data-role="slider" data-inline="true">
                <option value="no-garantie">无保修</option>
                <option value="has-garantie">有保修</option>
            </select>
            
            <input type="text" name="item-number" id="item-number" placeholder="购买商场(选填)">
            <input type="text" name="mall" id="mall" placeholder="机号(选填)">
          </div>
          <br>
          
          <div data-role="appoint-info">
            <div data-role="header" style="background:red" >
                <h4 style="clear:both;color:white;text-shadow:0 0">预约及反馈</h4>
            </div>
            
            <div data-role="appoint-date">
                <input type="text" id="appoint-date" name="appoint-date" placeholder="预约日期" />
                
                <div>
					<select name="ppoint-time" id="appoint-time">
					    <option value="" disabled selected>预约时间</option>
					    <option value='9:00-10:00'>9:00-10:00</option>
	                    <option value='10:00-11:00'>10:00-11:00</option>
						<option value='11:00-12:00'>11:00-12:00</option>
						<option value='12:00-13:00'>12:00-13:00</option>
						<option value='13:30-14:30'>13:30-14:30</option>
						<option value='14:30-15:30'>14:30-15:30</option>
						<option value='15:30-16:30'>15:30-16:30</option>
						<option value='16:30-17:30'>16:30-17:30</option>
					</select>
				</div>
            </div> <!-- End of appoint-date -->
            
           <label for="question" style="clear:both">反映问题</label>
           <textarea name="question" id="question"></textarea>
           
           <input type="text" name="lus-info" id="plus-info" placeholder="备注:(选填)">
          </div> <!-- End of appoint-info -->
		</div> <!-- End of content -->

		<div data-role="footer" style="background:red">
			<button id="send-form" style="background-color:red;border-color:red;color:white;font-size:1em;text-shadow:0 0;box-shadow:0 0">提交申请</button>
		</div>

	</div>
</body>
</html>