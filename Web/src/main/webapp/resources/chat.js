// Websocket Endpoint url
const host = window.location.host;
const path = window.location.pathname;
const webCtx = path.substring(0, path.indexOf('/', 1));
const endPointURL = "ws://" + host + webCtx + "/chat";

let chatClient = null;

function connect () {
    chatClient = new WebSocket(endPointURL);
    let thisUserName;
    const allUserNamesSet = new Set();
    let id = 0;
    let receiverName;
    const inputFilesMap = new Map();
    const outputFilesMap = new Map();

    function getId() {
        id = id + 1;
        return id;
    }

    const loginModal = $("#login_modal").modal();

    chatClient.onerror = function(ev) {
        alert('Error '+ev.data);
    };

    chatClient.onmessage = function (event) {
        const jsonObj = JSON.parse(event.data);
        const type = jsonObj.type;

        if (type === "CREDENTIALS_REQUEST") {
            login.removeClass("hidden");
        } else if (type === "USER_ACCEPTED") {
            loginModal.modal("hide");
        } else if (type === "TEXT_MESSAGE") {
            addMessage(jsonObj.data);
        } else if (type === "ERROR_MESSAGE") {
            if (loginModal.hasClass("in")) {
                displayUserCredentialsError(jsonObj.data);
            } else {
                displayError(jsonObj.data);
            }
        } else if (type === "INFO_MESSAGE") {
            displayInfo(jsonObj.data);
        } else if (type === "USER_ADDED") {
            addUser(jsonObj.data);

        } else if (type === "USER_REMOVED") {
            removeUser(jsonObj.data);
            deleteUserFromOutputAndInputFilesMap(jsonObj.data)
        } else if (type === "FILE_MESSAGE_FOR_ALL") {
            askFileAllMessage(jsonObj);
        } else if (type === "FILE_MESSAGE") {
            askFileMessage(jsonObj);
        } else if (type === "FILE_MESSAGE_REQUEST") {
            processFileMessageRequest(jsonObj);
        } else if (type === "FILE_MESSAGE_RESPONSE") {
            processFileMessageResponse(jsonObj);
        }
    };

    function sendMessage(type, data, senderName, receiverName, bytes, senderInputStreamId, receiverOutputStreamId) {
        const jsonObj = {
            "type": type, "data": data, "senderName": senderName, "receiverName": receiverName,
            "bytes": bytes, "senderInputStreamId": senderInputStreamId, "receiverOutputStreamId": receiverOutputStreamId
        };
        chatClient.send(JSON.stringify(jsonObj));
    }

    const login = $("#login").click(function () {
        login.addClass("hidden");
        thisUserName = $("#name").val();
        sendMessage("USER_CREDENTIALS", $("#password").val(), thisUserName)
    });

    function displayUserCredentialsError(message) {
        const field = $("#credentials_error");
        field.text(message);
    }

    function addMessage(message) {
        const messagesArea = $("#messages");
        messagesArea.val(message + "\r\n" + messagesArea.val());
        messagesArea.scrollTop = messagesArea.scrollHeight;
    }

    const textMessage = $("#message_input");

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
        const id = getId();

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
            '</div>'
        );

        $("#message_error_" + id).text(message);

        const modal = $("#error_modal_" + id);

        $("#error_ok_" + id).click(function () {
            modal.modal('hide');
            modal.remove();
        });

        modal.modal();
    }

    function displayInfo(message) {
        const id = getId();

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
            '</div>'
        );

        $("#message_info_" + id).text(message);

        const modal = $("#info_modal_" + id);

        $("#info_ok_" + id).click(function () {
            modal.modal('hide');
            modal.remove();
        });

        modal.modal();
    }

    function addUser(userName) {
        allUserNamesSet.add(userName);
        let users = "";

        for (let userName of allUserNamesSet) {
            users = (users + userName + "\r\n");
        }

        $("#users").val(users);

        addMessage("User " + userName + " joined the chat.");
    }

    function removeUser(userName) {
        allUserNamesSet.delete(userName);
        let users = "";

        for (let userName of allUserNamesSet) {
            users = (users + userName + "\r\n");
        }

        $("#users").val(users);

        addMessage("User " + userName + " has left the chat");
    }

    $("#private_message").click(function () {
        userPMSelect();
    });

    function userPMSelect() {
        const usersSelect = $("#my_pm_select");
        usersSelect.empty();

        for (let userName of allUserNamesSet) {
            if (thisUserName !== userName) {
                usersSelect.append($('<option>', { value : userName }).text(userName));
            }
        }

        $("#user_pm_modal").modal();
    }

    $("#user_pm_ok").click(function () {
        $("#user_pm_modal").modal("hide");
        receiverName = $("#my_pm_select").find(":selected").attr('value');
        if (receiverName !== undefined) {
            $("#pm_modal").modal();
        }
    });

    $("#pm_send").click(function () {
        const privateMessage = $("#pm_input");
        $("#pm_modal").modal("hide");
        sendMessage("PRIVATE_MESSAGE", privateMessage.val(), null, receiverName);
        privateMessage.val("");
    });

    $("#file_message").click(function () {
        userFMSelect();
    });

    function userFMSelect() {
        const usersSelect = $("#my_fm_select");
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

    $("#file_select").click( function() {
        $("#file_input").trigger( 'click' );
    } );

    $("#fm_send").click(function () {
        const files = document.getElementById('file_input').files;

        if (!files.length) {
            displayError('Please select a file!');
            return;
        }

        const file = files[0];

        const id = getId();

        if (inputFilesMap.has(receiverName)) {
            inputFilesMap.get(receiverName).set(id, [file, 0]);
        } else {
            inputFilesMap.set(receiverName, new Map().set(id, [file, 0]));
        }

        sendMessage("FILE_MESSAGE", file.name, null, receiverName, null, id);

        $("#fm_modal").modal("hide");
    });

    $("#file_all_message").click(function () {
        $("#file_all_input").val('');

        $("#fma_modal").modal();
    });

    $("#file_all_select").click( function() {
        $("#file_all_input").trigger( 'click' );
    } );

    $("#fma_send").click(function () {
        const files = document.getElementById('file_all_input').files;

        if (!files.length) {
            displayError('Please select a file!');
            return;
        }

        const file = files[0];

        if (file.size > 1024*1024*10) {
            displayError("File must not exceed 10mb");
            $("#file_all_input").val('');
            return;
        }

        const promise = new Promise(getBuffer);

        promise.then(function(data) {
            sendMessage("FILE_MESSAGE_FOR_ALL", file.name, null, null, Array.from(data));
            $("#fma_modal").modal("hide");
        }).catch(function(err) {
            console.log('Error: ',err);
            displayError("File reading error");
        });

        function getBuffer(resolve) {
            const reader = new FileReader();
            reader.readAsArrayBuffer(new Blob([file]));
            reader.onload = function() {
                const arrayBuffer = reader.result;
                const bytes = new Uint8Array(arrayBuffer);
                resolve(bytes);
            }
        }
    });

    function askFileAllMessage(message) {
        const id = getId();

        $("#body").append(
            '<div class="modal" id="ask_all_file_modal_' + id + '" data-backdrop="static" data-keyboard="false" \n' +
            'tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;"> \n' +
            '<div class="modal-dialog"> \n' +
            '<div class="chatmodal-container"> \n' +
            '<h2>File Message</h2><br> \n' +
            '<form> \n' +
            '<p id="message_all_file_' + id + '" class="text-centred bg-danger"></p> \n' +
            '<input id="download_all_file_' + id + '" type="button" class="chatmodal-submit bg-success" value="yes">' +
            '<input id="deny_all_file_' + id + '" type="button" class="chatmodal-submit bg-success" value="no">' +
            '</form> \n' +
            '</div> \n' +
            '</div> \n' +
            '</div>'
        );

        $("#message_all_file_" + id).text("User\n" + message.senderName + "\nsend file\n" + message.data + "\nfor you\n" +
        "download the file?");

        const modal = $("#ask_all_file_modal_" + id);

        $("#download_all_file_" + id).click(function () {
            modal.modal('hide');
            modal.remove();

            const fileStream = streamSaver.createWriteStream(message.data);
            const writer = fileStream.getWriter();
            writer.write(new Uint8Array(message.bytes));
            writer.close();
        });

        $("#deny_all_file_" + id).click(function () {
            modal.modal('hide');
            modal.remove();
        });

        modal.modal();
    }

    function askFileMessage(message) {
        const id = getId();

        $("#body").append(
            '<div class="modal" id="ask_file_modal_' + id + '" data-backdrop="static" data-keyboard="false" \n' +
            'tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true" style="display: none;"> \n' +
            '<div class="modal-dialog"> \n' +
            '<div class="chatmodal-container"> \n' +
            '<h2>File Message</h2><br> \n' +
            '<form> \n' +
            '<p id="message_file_' + id + '" class="text-centred bg-danger"></p> \n' +
            '<input id="download_file_' + id + '" type="button" class="chatmodal-submit bg-success" value="yes">' +
            '<input id="deny_file_' + id + '" type="button" class="chatmodal-submit bg-success" value="no">' +
            '</form> \n' +
            '</div> \n' +
            '</div> \n' +
            '</div>'
        );

        $("#message_file_" + id).text("User\n" + message.senderName + "\nsend file\n" + message.data + "\nfor you\n" +
            "download the file?");

        const modal = $("#ask_file_modal_" + id);

        $("#download_file_" + id).click(function () {
            modal.modal('hide');
            modal.remove();

            const fileStream = streamSaver.createWriteStream(message.data, 2048);
            const writer = fileStream.getWriter();

            if (outputFilesMap.has(message.senderName)) {
                outputFilesMap.get(message.senderName).set(id, writer);
            } else {
                outputFilesMap.set(message.senderName, new Map().set(id, writer));
            }

            sendMessage("FILE_MESSAGE_RESPONSE", message.data, message.senderName, message.receiverName,
                null, message.senderInputStreamId, id);
        });

        $("#deny_file_" + id).click(function () {
            modal.modal('hide');
            modal.remove();

            sendMessage("FILE_MESSAGE_RESPONSE", message.data, message.senderName, message.receiverName,
                null, message.senderInputStreamId, -2);
        });

        modal.modal();
    }

    function processFileMessageRequest(message) {
        const writer = outputFilesMap.get(message.senderName).get(message.receiverOutputStreamId);

        const senderInputStreamId = message.senderInputStreamId;

        if (senderInputStreamId > 0) {
            writer.write(new Uint8Array(message.bytes));
            sendMessage("FILE_MESSAGE_RESPONSE", message.data, message.senderName, message.receiverName, null,
            message.senderInputStreamId, message.receiverOutputStreamId);
        } else if (senderInputStreamId === 0) {
            writer.write(new Uint8Array(message.bytes));
            writer.close();
            sendMessage("FILE_MESSAGE_RESPONSE", message.data, message.senderName, message.receiverName, null,
                message.senderInputStreamId, 0);

            outputFilesMap.get(message.senderName).delete(message.receiverOutputStreamId);
        } else if (senderInputStreamId === -1) {
            writer.abort();
            outputFilesMap.get(message.senderName).delete(message.receiverOutputStreamId);
        }
    }

    function processFileMessageResponse(message) {
        if (message.receiverOutputStreamId > 0) {
            let senderInputStreamId = message.senderInputStreamId;

            const inputArray = inputFilesMap.get(message.receiverName).get(senderInputStreamId);
            const file = inputArray[0];
            const startByte = inputArray[1];
            let stopByte;

            if (startByte + (1024 * 6) > file.size) {
                stopByte = file.size;
                senderInputStreamId = 0;
            } else {
                stopByte = startByte + (1024 * 6);
            }

            const promise = new Promise(getBuffer);

            promise.then(function(data) {
                sendMessage("FILE_MESSAGE_REQUEST", message.data, message.senderName, message.receiverName, Array.from(data),
                    senderInputStreamId, message.receiverOutputStreamId);

                inputArray[1] = stopByte;
            }).catch(function(err) {
                console.log('Error: ',err);
                displayError("File reading error");

                sendMessage("FILE_MESSAGE_REQUEST", message.data, message.senderName, message.receiverName, null,
                    -1, message.receiverOutputStreamId);

                inputFilesMap.get(message.receiverName).delete(message.senderInputStreamId);
                displayError("Error reading file: " + file.name + ", for user: " + message.receiverName)
            });

            function getBuffer(resolve) {
                const reader = new FileReader();
                const blob = file.slice(startByte, stopByte);
                reader.readAsArrayBuffer(blob);
                reader.onload = function() {
                    const arrayBuffer = reader.result;
                    const bytes = new Uint8Array(arrayBuffer);
                    resolve(bytes);
                }
            }
        } else {
            inputFilesMap.get(message.receiverName).delete(message.senderInputStreamId);
        }
    }

    function deleteUserFromOutputAndInputFilesMap(userName) {
        if (outputFilesMap.has(userName)) {
            for (const value of outputFilesMap.get(userName).values()) {
                value.abort();
            }

            outputFilesMap.delete(userName);
        }

        if (inputFilesMap.has(userName)) {
            for (const value of inputFilesMap.get(userName).values()) {
                displayError("Error sending file: " + value[0].name + " to user: " + userName)
            }

            inputFilesMap.delete(userName);
        }
    }
}

function disconnect () {
    chatClient.close();
}

$(document).ready(function(){
    function heightFunction() {
        const myHeat = $(window).height();
        const leftColon = myHeat - 155;
        const RightColon = myHeat - 327;
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