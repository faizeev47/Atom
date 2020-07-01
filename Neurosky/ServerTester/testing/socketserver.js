const net = require("net");
const WebSocket = require("ws");

const DATASTREAM_PORT = 8000;
const SERVER_MAC_ADDRESS = "60:f6:77:8b:86:20";
const SERVER_PORT = 8001;


var os = require('os');
var ifaces = os.networkInterfaces();

var hostname = "localhost";
Object.keys(ifaces).forEach(function (ifname) {
  ifaces[ifname].forEach(function (iface) {
    if (iface.mac == SERVER_MAC_ADDRESS
            && iface.family == 'IPv4') {
        hostname = iface.address;
    }
  });
});

var dataStreamConnection = 
    net.createConnection({
        port: DATASTREAM_PORT
    });


dataStreamConnection.once("connect", () =>{
    console.log("DATA STREAM: connected!");

    var wss = new WebSocket.Server({
        host: hostname,
        port: SERVER_PORT
    }).on("listening", () => {
        console.log("LISTENING: ");
        console.log(wss.options);
    }).on("connection", (socket, request) => {
        socket.send("Hello");
        dataStreamConnection.on("data", (data) => {
            socket.send(data.toString());
        })
    }).on("close", () => {
        console.log("CLOSED");
    }).on("error", (error) => {
        console.log("ERROR -> " + error);
    });
    
    dataStreamConnection.once("close", () => {
        console.log("DATA STREAM: closed!\nClosing client server...");
        wss.close();
        console.log("Client server closed!");
    })
})
.on("data", (data) => {
    console.log("DATA STREAM: received: " + data.toString().trim())
})
.on("error", (err) => {
    console.log("DATA STREAM: " + err);
});

