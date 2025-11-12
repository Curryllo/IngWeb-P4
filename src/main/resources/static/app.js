const stompClient = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/eliza'
});

stompClient.onConnect = (frame) => {
    setConnected(true);
    console.log('Connected ' + frame);
    stompClient.subscribe('/topic/broadcast', (message) => {
        showMessage(JSON.parse(message.body).content);
        updateCounter(JSON.parse(message.body).counter);
    });
};



stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};


stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
}

function sendMessage(){
    stompClient.publish({
        destination: "/app/chat",
        body: JSON.stringify({'message': $("#message").val()})
    });
}
function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    //$("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    //else {
    //    $("#conversation").hide();
    //}
    $("#messages").html("");
}

function connect() {
    stompClient.activate();
    console.log("Cliente conectado")
}

function showMessage(message) {
    $("#messages").prepend("<tr><td>" + message + "</td></tr>");
}

function updateCounter(counter){
    $("#messagesSent").text(counter);
}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $( "#connect" ).click(() => connect());
    $( "#send" ).click(() => sendMessage());
});