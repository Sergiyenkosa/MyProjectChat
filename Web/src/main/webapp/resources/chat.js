// Websocket Endpoint url
var host = window.location.host;
var path = window.location.pathname;
var webCtx = path.substring(0, path.indexOf('/', 1));
var endPointURL = "ws://" + window.location.host + webCtx + "/chat";

var chatClient = null;

function connect () {
    chatClient = new WebSocket(endPointURL);
    var thisUserName;
    var allUserNamesSet = new Set();

    chatClient.onmessage = function (event) {
        var jsonObj = JSON.parse(event.data);

        if (jsonObj.type === "CREDENTIALS_REQUEST") {
            login.removeClass("hidden");
        } else if (jsonObj.type === "USER_ACCEPTED") {
            loginModal.modal("hide");
        } else if (jsonObj.type === "TEXT_MESSAGE") {
            addMessage(jsonObj.data)
        } else if (jsonObj.type === "ERROR_MESSAGE") {
            if (loginModal.hasClass("in")) {
                displayUserCredentialsError(jsonObj.data);
                displayUserCredentialsError(conceptName)
            } else {
                displayError(jsonObj.data)
            }
        }  else if (jsonObj.type === "INFO_MESSAGE") {
            displayInfo(jsonObj.data)
        } else if (jsonObj.type === "USER_ADDED") {
            allUserNamesSet.add(jsonObj.data);
            let users = "";

            for (let userName of allUserNamesSet) {
                users = (users + userName + "\r\n");
            }

            $("#users").val(users);

            addMessage("User " + jsonObj.data + " joined the chat.");
        } else if (jsonObj.type === "USER_REMOVED") {
            allUserNamesSet.delete(jsonObj.data);
            let users = "";

            for (let userName of allUserNamesSet) {
                users = (users + userName + "\r\n");
            }

            $("#users").val(users);

            addMessage("User " + jsonObj.data + " left the chat");
        }
    };
    
    var messagesArea = $("#messages");

    function addMessage(message) {
        messagesArea.val(message + "\r\n" + messagesArea.val());
        messagesArea.scrollTop = messagesArea.scrollHeight;
    }

    var loginModal = $("#login_modal").modal();

    var login = $("#login").click(function () {
        login.addClass("hidden");
        thisUserName = $("#name").val();
        sendMessage("USER_CREDENTIALS", $("#password").val(), thisUserName)
    });

    var textMessage = $("#message_input");
    textMessage.keydown(function(event) {
        if (event.keyCode === 13) {
            sendMessage("TEXT_MESSAGE", textMessage.val());
            textMessage.val("");
        }
    });

    $("#messageSend").click(function () {
        sendMessage("TEXT_MESSAGE", textMessage.val());
        textMessage.val("");
        textMessage.focus();
    });

    function displayUserCredentialsError(message) {
        var field = $("#credentials_error");
        field.text(message);
    }

    function displayError(message) {
        var field = $("#message_error");
        field.text(message);
        var errorModal = $("#error_modal");
        errorModal.modal();
        $("#error_ok").click(function () {
            errorModal.modal("hide");
        });
    }

    function displayInfo(message) {
        var field = $("#message_info");
        field.text(message);
        var infoModal = $("#info_modal");
        infoModal.modal();
        $("#error_ok").click(function () {
            infoModal.modal("hide");
        });
    }

    $("#private_message").click(function () {
        userPMSelect();
    });

    function userPMSelect() {
        var usersSelect = $("#my_pm_select");
        usersSelect.empty();

        for (let userName of allUserNamesSet) {
            if (thisUserName !== userName) {
                usersSelect.append($('<option>', { value : userName }).text(userName));
            }
        }

        $("#user_pm_modal").modal();
    }

    var receiverName;

    $("#user_pm_ok").click(function () {
        $("#user_pm_modal").modal("hide");
        receiverName = $("#my_pm_select").find(":selected").attr('value');
        $("#pm_modal").modal();
    });

    $("#pm_send").click(function () {
        var privateMessage = $("#pm_input").val();
        $("#pm_modal").modal("hide");
        sendMessage("PRIVATE_MESSAGE", privateMessage, null, receiverName);
    });
}

function disconnect () {
    chatClient.close();
}

function sendMessage(type, data, senderName, receiverName, bytes, senderInputStreamId, receiverOutputStreamId) {
    var jsonObj = {"type" : type, "data" : data, "senderName" : senderName, "receiverName" : receiverName,
        "bytes" : bytes, "senderInputStreamId" : senderInputStreamId, "receiverOutputStreamId" : receiverOutputStreamId};
    chatClient.send(JSON.stringify(jsonObj));
}

$(document).ready(function(){
    function heightFunction() {
        var myHeat = $(window).height();
        var leftColon = myHeat - 155;
        var RightColon = myHeat - 327;
        $('#messages').css({"height": leftColon});
        $('#users').css({"height": RightColon});
    }
    
    $( window ).resize(function() {
        heightFunction();
    });

    heightFunction();

    $("#pm_input").keypress(function(event) { return event.keyCode !== 13; });//fix input bug
});