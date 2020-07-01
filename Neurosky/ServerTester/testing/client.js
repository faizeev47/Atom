const net = require("net");

var os = require('os');
var ifaces = os.networkInterfaces();

var hostname = "localhost";
Object.keys(ifaces).forEach(function (ifname) {
  ifaces[ifname].forEach(function (iface) {
    if (iface.mac == '60:f6:77:8b:86:20' 
            && iface.family == 'IPv4') {
        hostname = iface.address;
    }
  });
});


var socketConnection =  
    net.createConnection(8001, hostname);

socketConnection.once("connect", () => {
    console.log("connected to server!");
    process.stdin.pipe(socketConnection);
})
.on("data", (data) => {
    console.log("server said: " + data.toString().trim());
})
.on("error", (err) => {
    console.log("oops!" +  err.message);
})
.on("close", () => {
    process.stdin.unpipe(socketConnection);
    socketConnection.destroy();
})


