const net = require("net");

const packet = {
            "eSense": {
                "attention": 29,
                "meditation": 84
            }, 
            "eegPower": {
                "delta": 182763, 
                "theta": 52122, 
                "lowAlpha": 5007, 
                "highAlpha": 10425, 
                "lowBeta": 6174, 
                "highBeta": 1873, 
                "lowGamma": 1338, 
                "highGamma": 80388
            }, 
            "poorSignalLevel": 0
        };

const DISCONNECTED_PACKET = '{"poorSignalLevel":200,"status":"scanning"}';
const SCANNING_COUNT = 8;

const DAMPING_FACTOR = 10;
const STARTING_ATTENTION = 1;

var x = DAMPING_FACTOR * Math.PI * (Math.asin((STARTING_ATTENTION - 50)/50) + Math.PI/2);
function getPacket() {
    //50sin(x/4 - (pi/2)) + 50
    packet.eSense.attention = Math.round(50 * Math.sin((x/(Math.PI * DAMPING_FACTOR)) - (Math.PI/2)) + 50);
    x += 1
    return packet;
}

process.stdin.on("data", (data) => {
    const parsed = parseInt(data.toString());
    if (isNaN(parsed)) {
        x = DAMPING_FACTOR * Math.PI * (Math.asin((STARTING_ATTENTION - 50)/50) + Math.PI/2);
    } else {
        x = DAMPING_FACTOR * Math.PI * (Math.asin((parsed - 50)/50) + Math.PI/2);
    }
    
});


var socketServer = net.createServer( (connection) => {
    var packet_count = 0;
    socketServer.getConnections((err, count) => {
        console.log("CONNECTED -> active: " + count)
    });
    connection.on("close", (had_error) => {})
    .on("end", () => {
        console.log("ENDED");
    })
    .on("error", (err) => {
        console.log("ERROR: " + err.message.split(": ")[1]);
    })
    function resolveAfter2Seconds() {
        return new Promise(resolve => {
          setTimeout(() => {
            if (packet_count < SCANNING_COUNT) {
                resolve(DISCONNECTED_PACKET);
            } else {
                resolve(JSON.stringify(getPacket()));
            }
          }, 1000);
        });
      }
      
    async function asyncCall() {
        console.log('calling');
        while (connection) {
            const result = await resolveAfter2Seconds();
            packet_count++;
            connection.write(result);
        }
    }
    asyncCall();
  }).listen(8000);


if (socketServer.listening) {
    var address = socketServer.address().address;
    if (address === "::") {
        address = "localhost:"
    }
    console.log("Listening at " + 
        address +
        socketServer.address().port);
    
} else {
    console.log("Not listening");
}

process.on("beforeExit", (code) => {
    socketServer.close();
})