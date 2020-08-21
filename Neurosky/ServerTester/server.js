const net = require("net");
const WebSocket = require("ws");
const admin = require("firebase-admin");
const serviceAccount = require("/mnt/c/Users/Faizan/atom-e0e04-firebase-adminsdk-vh4im-f38cb02a0f.json");

const DATA_PORTS = {"THINKGEAR": 13854,
                    "MOCKSERVER": 8000};

const DATASTREAM_PORT = DATA_PORTS["THINKGEAR"];

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://atom-e0e04.firebaseio.com"
});

// Specifications for the server
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

var db = admin.database();
var ref = db.ref("/");
ref.child("hostOptions").set({
    hostname: hostname,
    port: SERVER_PORT
});

var initiateStreamMessage ='{"enableRawOutput": false,"format": "Json"}';

var dataStreamConnection = 
    net.createConnection({
        port: DATASTREAM_PORT
    });


var scanStringIdx = 0;


dataStreamConnection.once("connect", () =>{
    console.log("DATA STREAM: connected!");
    dataStreamConnection.write(initiateStreamMessage)
    var attention = 0;
    var wss = new WebSocket.Server({
        host: hostname,
        port: SERVER_PORT
    }).on("listening", () => {
        console.log("LISTENING: ");
        console.log(wss.options);
    }).on("connection", (socket, request) => {
        console.log("CONNECTED: active -> " + wss.clients.size);
        dataStreamConnection.on("data", (data) => {
            var parsedObject = parsePacket(data);
            if (parsedObject.hasOwnProperty("eSense")) {
                attention = parsedObject.eSense.attention;
            }
            socket.send(JSON.stringify(parsePacket(data, attention)));
        });
        socket.on("close", (code, reason) => {
            dataStreamConnection.removeAllListeners("data");
            dataStreamConnection.on("data", logDataFromStream);
            console.log("DISCONNECTED: active clients -> " + wss.clients.size);
        });
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
.on("data", logDataFromStream)
.on("error", (err) => {
    console.log("DATA STREAM: " + err);
});

function getDisconnectedPacket(status) {
    return {"status": status, "disconnected": true};
}

function getAttentionObject(attention) {
    return {"attention": attention, "connected": true}
}

function parsePacket(data, previousAttention) {
    var packet = JSON.parse(data.toString().trim());
    if (!packet.hasOwnProperty("status")) {
        if (packet.hasOwnProperty("eSense")) {
            return getAttentionObject(packet["eSense"]["attention"]);
        } else {
            return getAttentionObject(previousAttention);
        }
    }
    else {
        return getDisconnectedPacket(packet["status"]);
    }
}

function logDataFromStream(data) {
    data = data.toString().trim();
    if (data != '{"poorSignalLevel":200,"status":"scanning"}') {
        console.log("\nDATA STREAM: received: " + data);
    }
    else {
        scanLoading(data);
    }
}

function resolveAfter1Second(string) {
    var P = [string + ".   ", string + "..  ", string + "... ", string + "...."];
    return new Promise(resolve => {
        setTimeout(() => {
            resolve("\r" + P[scanStringIdx++] + "\r");
        }, 250);
    });
}

async function scanLoading(data) {
    const string = "DATA STREAM: received: " + data;
    const result = await resolveAfter1Second(string);
    process.stdout.write(result);
    scanStringIdx &= 3;
}

function toByteArray(string) {
    var bytes = [];
    for (var i = 0, l = string.length; i < l; i++) {
        var code = string.charCodeAt(i);
        bytes = bytes.concat([code & 0xff]);
    }
    return bytes;
}
