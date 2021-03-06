<%--
  Created by IntelliJ IDEA.
  User: s.sergienko
  Date: 13.04.2017
  Time: 13:27
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Chat</title>
    <meta name="viewport" content="initial-scale=1, maximum-scale=1">
    <link rel='stylesheet' href='webjars/bootstrap/3.2.0/css/bootstrap.min.css'>
    <link rel="stylesheet" href="resources/chat.css">
    <link rel="shortcut icon" href="data:image/x-icon;," type="image/x-icon">
    <script src="webjars/jquery/2.1.1/jquery.min.js"></script>
    <script src="webjars/bootstrap/3.2.0/js/bootstrap.min.js"></script>
    <script src="resources/chat.js"></script>
    <script src="resources/StreamSaver.js"></script>
    <script src="https://cdn.rawgit.com/creatorrr/web-streams-polyfill/master/dist/polyfill.min.js"></script>
</head>
<body id="body" onload="connect();" onunload="disconnect();">
<h1> Chat </h1>

<div class="modal fade" id="login_modal" data-backdrop="static" data-keyboard="false" tabindex="-1" role="dialog"
     aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;">
    <div class="modal-dialog">
        <div class="chatmodal-container">
            <h2>Login to Chat</h2><br>
            <form>
                <p id="credentials_error" class="text-centred bg-danger"></p>
                <input id="name" type="text" name="name" placeholder="Username">
                <input id="password" type="password" name="pass" placeholder="Password">
                <input id="login" type="button" name="login" class="chatmodal-submit hidden" value="Sign in">
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="user_pm_modal" tabindex="-1" role="dialog"
     aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;">
    <div class="modal-dialog">
        <div class="chatmodal-container">
            <h2>Select User</h2><br>
            <form>
                <select id="my_pm_select"></select>

                <input id="user_pm_ok" type="button" class="chatmodal-submit bg-success" value="ok">
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="pm_modal" tabindex="-1" role="dialog"
     aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;">
    <div class="modal-dialog">
        <div class="chatmodal-container">
            <h2>Private message</h2><br>
            <form>
                <input id="pm_input" type="text" placeholder="Private message" >

                <input id="pm_send" type="button" class="chatmodal-submit bg-success" value="send message">
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="user_fm_modal" tabindex="-1" role="dialog"
     aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;">
    <div class="modal-dialog">
        <div class="chatmodal-container">
            <h2>Select User</h2><br>
            <form>
                <select id="my_fm_select"></select>

                <input id="user_fm_ok" type="button" class="chatmodal-submit bg-success" value="ok">
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="fm_modal" tabindex="-1" role="dialog"
     aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;">
    <div class="modal-dialog">
        <div class="chatmodal-container">
            <h2>File message</h2><br>
            <form>
                <input id="file_input" type="file"/>
                <input id="file_select" type="button" class="chatmodal-submit bg-success" value="select file">

                <input id="fm_send" type="button" class="chatmodal-submit bg-success" value="send file">

                <div id="byte_range"></div>
                <div id="byte_content"></div>
            </form>
        </div>
    </div>
</div>

<div class="modal fade" id="fma_modal" tabindex="-1" role="dialog"
     aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;">
    <div class="modal-dialog">
        <div class="chatmodal-container">
            <h2>File message for all</h2><br>
            <form>
                <input id="file_all_input" type="file"/>
                <input id="file_all_select" type="button" class="chatmodal-submit bg-success" value="select file">

                <input id="fma_send" type="button" class="chatmodal-submit bg-success" value="send file">
            </form>
        </div>
    </div>
</div>

<div class="page">
    <div class="left_block">
        <div class="wrapper">
            <input id="message_input" class="text-field" type="text" placeholder="Message"/>
        </div>
        <div class="wrapper_chat">
            <textarea id="messages" class="panel message-area" readonly ></textarea>
        </div>
    </div>
    <div class="right_block">
        <input id="messageSend" class="button" type="submit" value="Send"/>
        <textarea id="users" class="panel message-area" readonly ></textarea>
        <input type="submit" value="Send private message" id="private_message" class="button">
        <input type="submit" value="Send file message" id="file_message" class="button">
        <input type="submit" value="Send file message for all" id="file_all_message" class="button">
    </div>
</div>
</body>
</html>
