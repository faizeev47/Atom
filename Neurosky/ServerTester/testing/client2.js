const WebSocket = require('ws');

const ws = new WebSocket("ws://192.168.100.9:8001");

ws.on("error", (err) => {
    console.log(err);
}).on("message", (data) => {
    console.log(data);
});