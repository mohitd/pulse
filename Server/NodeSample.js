var commandLineArgs = require('command-line-args');

var net = require('net');

cors = require('cors')



var express = require('express');

var app = express();

app.use(cors());

var mysql      = require('mysql');
 
var cli = commandLineArgs([
  { name: 'address', alias: 'a', type: String},
  { name: 'mysqlUser', alias: 'u', type: String},
  { name: 'mysqlPassword', alias: 'p', type: String}
]);

var options = cli.parse();

console.log(options);


app.get('/getDataPoints', function(req, res) {
    res.setHeader('Content-Type', 'application/json');

    var lati = parseFloat(req.query.lat);
    var longi = parseFloat(req.query.lon);
    var radi = parseFloat(req.query.rad);
    
    console.log(lati,longi,radi);

    if(!isNaN(lati) && !isNaN(longi) && !isNaN(radi) && radi > 0.0){
        grabPointsInRange(lati,longi,radi,res);
    }
    else{
        res.send("Invalid input");
        console.log("Invalid input");
    }
});

app.listen(3000);

var HOST = options.address;
var PORT = 6969;

var mysqlCon = mysql.createConnection({
  host     : 'pulsedata.ckvl8bq6n9hh.us-east-1.rds.amazonaws.com',
  user     : options.mysqlUser,
  password : options.mysqlPassword,
  database : "HeartRate"
});

mysqlCon.connect(function(err) {
    if (err) {
        console.error('error connecting: ' + err.stack);
        return;
    }

    mysqlCon.query("DROP TABLE IF EXISTS HeartRate",function(err){
        if (err) {
            console.error('error with initial drop query: ' + err.stack);
            return;
        }

        var createQuery = "CREATE TABLE `HeartRate` (`id` int(11) unsigned NOT NULL AUTO_INCREMENT,`time` bigint(255) DEFAULT NULL,`rate` float DEFAULT NULL,`deviceID` varchar(255) DEFAULT NULL,`latitude` double DEFAULT NULL,`longitude` double DEFAULT NULL,PRIMARY KEY (`id`)) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;";        mysqlCon.query(createQuery,function(err){
            if (err) {
                console.error('error with initial create query: ' + err.stack);
                return;
            }

            makeServer();

        });
    });


});

function makeServer(){
            // Create a server instance, and chain the listen function to it
            // The function passed to net.createServer() becomes the event handler for the 'connection' event
            // The sock object the callback function receives UNIQUE for each connection
            var server = net.createServer(function(sock) {
                // We have a connection - a socket object is assigned to the connection automatically
                console.log('CONNECTED: ' + sock.remoteAddress +':'+ sock.remotePort);
                
                // Add a 'data' event handler to this instance of socket
                sock.on('data', function(data) {
                    console.log('DATA ' + sock.remoteAddress + ': ' + data);
                    
                    var req = String(data);

                    var rIndex = req.indexOf('rate":')+'rate":'.length;
                    var rate = parseFloat(req.substring(rIndex,req.indexOf(",",rIndex)));

                    var lIndex = req.indexOf('lat":')+'lat":'.length;
                    var lat = parseFloat(req.substring(lIndex,req.indexOf(",",lIndex)));

                    var tIndex = req.indexOf('time":')+'time":'.length;
                    var time = parseFloat(req.substring(tIndex,req.indexOf(",",tIndex)));

                    var loIndex = req.indexOf('lon":')+'lon":'.length;
                    var lon = parseFloat(req.substring(loIndex,req.indexOf("}",loIndex)));

                    console.log(rate);
                    console.log(time);
                    console.log(lat);
                    console.log(lon);

                    var HRObj = {
                        rate:rate,
                        time:time,
                        lat:lat,
                        lon:lon
                    }

                    insertHRObject(HRObj);

                    // try{
                    //     req = JSON.parse(""+data);
                    // }
                    // catch(e){
                    //     sock.write("Failed to parse JSON");
                    //     console.log(e);
                    //     return;
                    // }

                    // if(req.requestType.toLowerCase() === "send"){
                    //     sock.write("Received chunk.");

                    //     for(var i = 0; i < req.dataSet.length; i++){
                    //         var HRObject = req.dataSet[i];
                    //         insertHRObject(HRObject);
                    //     }

                    // }
                    // else{
                    //     sock.write("Invalid command");
                    // }
                  
                });
                
                // Add a 'close' event handler to this instance of socket
                sock.on('close', function(data) {
                    console.log('CLOSED: ' + sock.remoteAddress +' '+ sock.remotePort);
                });
                
            }).listen(PORT, HOST);

            server.on('error', function (e) {
              if (e.code == 'EADDRINUSE') {
                console.log('Address in use, retrying...');
                setTimeout(function () {
                  server.close();
                  server.listen(PORT, HOST);
                }, 1000);
              }
            });

            console.log('Server listening on ' + HOST +':'+ PORT);
}


function insertHRObject(HRObject){

    var rate = HRObject.rate;
    var lati = HRObject.lat;
    var longi = HRObject.lon;
    var time = HRObject.time;

    var qObj = {
                  sql: 'INSERT INTO HeartRate (rate,time,latitude,longitude) VALUES (?,?,?,?)',
                  timeout: 40000, // 40s
                  values: [rate,time,lati,longi]
                };

    mysqlCon.query(qObj,function(err){
        if (err) {
            console.error('error with inserton: ' + err.stack);
            return;
        }
       
    });

}


function grabPointsInRange(lati,longi,radius,res){

	var milliseconds = (new Date).getTime();

    var ss = 'SELECT latitude,longitude,rate FROM HeartRate WHERE SQRT(POW('+lati+'-latitude,2)+POW('+longi+'-longitude,2)) < '+radius;

	ss += ' AND ABS(time-'+milliseconds+') < 100000'

    var qObj = {
                    sql: ss,
                    timeout: 40000, // 40s
                    values: []
                };

    mysqlCon.query(qObj,function(err,rows){
        if (err) {
            console.error('error with inserton: ' + err.stack);
            return;
        }
        res.send(JSON.stringify(rows));
    });

}



