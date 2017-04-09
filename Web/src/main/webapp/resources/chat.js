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
    var id = 0;

    function getId() {
        id = id + 1;
        return id;
    }

    var loginModal = $("#login_modal").modal();

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
            } else {
                displayError(jsonObj.data)
            }
        } else if (jsonObj.type === "INFO_MESSAGE") {
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

    var login = $("#login").click(function () {
        login.addClass("hidden");
        thisUserName = $("#name").val();
        sendMessage("USER_CREDENTIALS", $("#password").val(), thisUserName)
    });

    function displayUserCredentialsError(message) {
        var field = $("#credentials_error");
        field.text(message);
    }

    function addMessage(message) {
        var messagesArea = $("#messages");
        messagesArea.val(message + "\r\n" + messagesArea.val());
        messagesArea.scrollTop = messagesArea.scrollHeight;
    }

    var textMessage = $("#message_input");

    textMessage.keydown(function (event) {
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

    function displayError(message) {
        var id = getId();

        $("#body").append(
            '<div class="modal" id="error_modal_' + id + '" data-backdrop="static" data-keyboard="false" \n' +
            'tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;"> \n' +
            '<div class="modal-dialog"> \n' +
            '<div class="chatmodal-container"> \n' +
            '<h2>Error</h2><br> \n' +
            '<form> \n' +
            '<p id="message_error_' + id + '" class="text-centred bg-danger"></p> \n' +
            '<input id="error_ok_' + id + '" type="button" class="chatmodal-submit" value="ok"> \n' +
            '</form> \n' +
            '</div> \n' +
            '</div> \n' +
            '</div>');

        $("#message_error_" + id).text(message);

        var modal = $("#error_modal_" + id);

        $("#error_ok_" + id).click(function () {
            modal.modal('hide');
            modal.remove();
        });

        modal.modal();
    }

    function displayInfo(message) {
        var id = getId();

        $("#body").append(
            '<div class="modal" id="info_modal_' + id + '" data-backdrop="static" data-keyboard="false" \n' +
            'tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;"> \n' +
            '<div class="modal-dialog"> \n' +
            '<div class="chatmodal-container"> \n' +
            '<h2>Info</h2><br> \n' +
            '<form> \n' +
            '<p id="message_info_' + id + '" class="text-centred bg-danger"></p> \n' +
            '<input id="info_ok_' + id + '" type="button" class="chatmodal-submit bg-success" value="ok">' +
            '</form> \n' +
            '</div> \n' +
            '</div> \n' +
            '</div>');

        $("#message_info_" + id).text(message);

        var modal = $("#info_modal_" + id);

        $("#info_ok_" + id).click(function () {
            modal.modal('hide');
            modal.remove();
        });

        modal.modal();
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
        if (receiverName !== undefined) {
            $("#pm_modal").modal();
        }
    });

    $("#pm_send").click(function () {
        var privateMessage = $("#pm_input").val();
        $("#pm_modal").modal("hide");
        sendMessage("PRIVATE_MESSAGE", privateMessage, null, receiverName);
        $("#pm_input").val("");
    });

    $("#file_message").click(function () {
        userFMSelect();
    });

    function userFMSelect() {
        var usersSelect = $("#my_fm_select");
        usersSelect.empty();

        for (let userName of allUserNamesSet) {
            if (thisUserName !== userName) {
                usersSelect.append($('<option>', { value : userName }).text(userName));
            }
        }

        $("#user_fm_modal").modal();
    }

    $("#user_fm_ok").click(function () {
        $("#user_fm_modal").modal("hide");
        receiverName = $("#my_fm_select").find(":selected").attr('value');
        if (receiverName !== undefined) {
            $("#file_input").val('');

            $("#fm_modal").modal();
        }
    });

    $("#file_choose").click( function() {
        $("#file_input").trigger( 'click' );
    } );

    var map = new Map();

    $("#fm_send").click(function () {
        var files = document.getElementById('file_input').files;

        if (!files.length) {
            displayError('Please select a file!');
            return;
        }

        var file = files[0];

        var id = getId();

        if (map.has(receiverName)) {
            map.get(receiverName).set(id, [file, 0]);
        } else {
            map.set(receiverName, new Map().set(id, [file, 0]));
        }

        sendMessage("FILE_MESSAGE", file.name, null, receiverName, null, id);

        $("#fm_modal").modal("hide");

        // readBlob(1, 7);
        // function readBlob(opt_startByte, opt_stopByte) {
        //
        //     var files = document.getElementById('file_input').files;
        //
        //     if (!files.length) {
        //         displayError('Please select a file!');
        //         return;
        //     }
        //
        //     var file = files[0];
        //
        //     var id = getId();
        //
        //     if (map.has(receiverName)) {
        //         map.get(receiverName).set(id, [file, 0]);
        //     } else {
        //         map.set(receiverName, new Map().set(id, [file, 0]));
        //     }
        //
        //     sendMessage("FILE_MESSAGE", file.name, null, receiverName, null, id);
        //
        //     console.log(map.get(receiverName).get(id)[0].name);
        //     console.log(file.name);
        //
        //     return;
        //
        //     var start = parseInt(opt_startByte) || 0;
        //     var stop = parseInt(opt_stopByte) || file.size - 1;
        //
        //     var reader = new FileReader();
        //
        //     reader.onloadend = function(evt) {
        //         if (evt.target.readyState == FileReader.DONE) { // DONE == 2
        //             document.getElementById('byte_content').textContent = evt.target.result;
        //             document.getElementById('byte_range').textContent =
        //                 ['Read bytes: ', start + 1, ' - ', stop + 1,
        //                     ' of ', file.size, ' byte file'].join('');
        //         }
        //     };
        //
        //     var blob = file.slice(start, stop + 1);
        //
        //     reader.readAsBinaryString(blob);
        // }
    });

    $("#file_all_message").click(function () {
        $("#file_all_input").val('');

        $("#fma_modal").modal();
    });

    $("#file_all_choose").click( function() {
        $("#file_all_input").trigger( 'click' );
    } );

    $("#fma_send").click(function () {
        var files = document.getElementById('file_all_input').files;

        if (!files.length) {
            displayError('Please select a file!');
            return;
        }

        var file = files[0];

        if (file.size > 1024*1024*10) {
            displayError("File must not exceed 10mb");
            $("#file_all_input").val('');
            return;
        }

        var promise = new Promise(getBuffer);

        promise.then(function(data) {
            sendMessage("FILE_MESSAGE_FOR_ALL", file.name, null, null, Array.from(data));
            $("#fma_modal").modal("hide");
        }).catch(function(err) {
            console.log('Error: ',err);
            displayError("File reading error");
        });

        function getBuffer(resolve) {
            var reader = new FileReader();
            reader.readAsArrayBuffer(new Blob([file]));
            reader.onload = function() {
                var arrayBuffer = reader.result;
                var bytes = new Uint8Array(arrayBuffer);
                resolve(bytes);
            }
        }
    });
}

function disconnect () {
    chatClient.close();
}

function sendMessage(type, data, senderName, receiverName, bytes, senderInputStreamId, receiverOutputStreamId) {
    var jsonObj = {
        "type": type, "data": data, "senderName": senderName, "receiverName": receiverName,
        "bytes": bytes, "senderInputStreamId": senderInputStreamId, "receiverOutputStreamId": receiverOutputStreamId
    };
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
    $("#fm_input").keypress(function(event) { return event.keyCode !== 13; });//fix input bug
});