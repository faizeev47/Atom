var admin = require("firebase-admin");

var serviceAccount = require("/mnt/c/Users/Faizan/atom-e0e04-firebase-adminsdk-vh4im-f38cb02a0f.json");

admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: "https://atom-e0e04.firebaseio.com"
});

var db = admin.database();
var ref = db.ref("/");
ref.child("hostOptions").set({
    hostname: '192.168.10.1',
    port: '8001'
});