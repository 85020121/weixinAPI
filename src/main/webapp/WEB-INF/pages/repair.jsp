<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<!DOCTYPE html>
<html lang="zh_CN">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>申请维修服务</title>

    <!-- Bootstrap -->
    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="http://libs.baidu.com/jquery/1.10.2/jquery."></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
        <!-- Latest compiled and minified CSS -->
        <link rel="stylesheet" href="http://libs.baidu.com/bootstrap/3.0.3/css/bootstrap.min.css">
        
        <!-- Optional theme -->
        <link rel="stylesheet" href="http://libs.baidu.com/bootstrap/3.0.3/css/bootstrap-theme.min.css">
        
        <!-- Latest compiled and minified JavaScript -->
        <script src="http://libs.baidu.com/bootstrap/3.0.3/js/bootstrap.min.js"></script>

    <!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"></script>
      <script src="https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"></script>
    <![endif]-->
  </head>
  <body>
    <h1>申请维修服务</h1>


        
        <!--  -->
        <div class="row">
            <div class="panel panel-default">
              <div class="panel-heading">
                <h3 class="panel-title">您的基本信息</h3>
              </div>
              
              <div class="panel-body">
                
                
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">姓名(必填)</span>
                      </span>
                    <input type="text" class="form-control" placeholder="您的真实姓名">
                    </div>
                
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">性别(必填)</span>
                      </span>
                        <div class="btn-group" data-toggle="button">
                          <label class="btn">
                            <input type="radio" name="options" id="option_male"> 男
                          </label>
                          <label class="btn">
                            <input type="radio" name="options" id="option_female"> 女
                          </label>
                        </div>
                    </div>
                
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">联系电话(必填)</span>
                      </span>
                      <input type="text" class="form-control" placeholder="座机需带区号:010-8888888">
                    </div>
                
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">手机号码(必填)</span>
                      </span>
                      <input type="text" class="form-control">
                    </div>
                
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">电子邮件(必填)</span>
                      </span>
                      <input type="text" class="form-control">
                    </div>
                    
                    
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">电子邮件(必填)</span>
                      </span>
                      <input type="text" class="form-control">
                    </div>
                    
                    <div class="input-group">
                        <span class="input-group-addon">
                            <span class="label label-danger">地址(必填)</span>                          
                        </span>
                        <span class="input-group-addon">
                            <span class="label label-danger">省份</span>
                        </span>
                        <input type="text" class="form-control">
                        <span class="input-group-addon">
                            <span class="label label-danger">城市</span>
                        </span>
                        <input type="text" class="form-control">
                        <span class="input-group-addon">
                            <span class="label label-danger">区域</span>
                        </span>
                        <input type="text" class="form-control">
                    </div>
                    
              </div>
                          
            </div>
            
            
            <div class="panel panel-default">
              <div class="panel-heading">
                <h3 class="panel-title">产品信息</h3>
              </div>              
              <div class="panel-body">
                
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">产品类别(必填)</span>
                      </span>
                    <input type="text" class="form-control">
                    </div>
                    
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">产品型号(必填)</span>
                      </span>
                    <input type="text" class="form-control">
                    </div>
                    
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">购买日期(必填)</span>
                      </span>
                    <input type="text" class="form-control">
                    </div>
                    
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">是否保修(必填)</span>
                      </span>
                        <div class="btn-group" data-toggle="button">
                          <label class="btn">
                            <input type="radio" name="options" id="option_baoxiu_yes"> 是
                          </label>
                          <label class="btn">
                            <input type="radio" name="options" id="option_baoxiu_no"> 否
                          </label>
                        </div>
                    </div>
                    
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">购买商场</span>
                      </span>
                    <input type="text" class="form-control">
                    </div>
                    
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">机号</span>
                      </span>
                    <input type="text" class="form-control">
                    </div>

                </div>
            </div>
            
            <div class="panel panel-default">
              <div class="panel-heading">
                <h3 class="panel-title">预约时间及问题反馈</h3>
              </div>              
              <div class="panel-body">
                
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">日期</span>
                      </span>
                    <input type="text" class="form-control">
                    </div>
                    
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">时间</span>
                      </span>
                    <input type="text" class="form-control">
                    </div>
                    
                    <div class="input-group">
                      <span class="input-group-addon">
                        <span class="label label-danger">反映问题</span>
                      </span>
                    <input type="text" class="form-control">
                    </div>
                
              </div>
            </div>

            
            
        </div>
        
  </body>
</html>