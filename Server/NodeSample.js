var commandLineArgs = require('command-line-args');

var net = require('net');

var mysql      = require('mysql');
 
var cli = commandLineArgs([
  { name: 'address', alias: 'a', type: String},
  { name: 'mysqlUser', alias: 'u', type: String},
  { name: 'mysqlPassword', alias: 'p', type: String}
])

var options = cli.parse();

console.log(options);

var HOST = options.address;
var PORT = 6969;

var dataSet = [];

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

        var createQuery = "CREATE TABLE `HeartRate` (`id` int(11) unsigned NOT NULL AUTO_INCREMENT,`time` int(11) DEFAULT NULL,`rate` float DEFAULT NULL,`deviceID` varchar(255) DEFAULT NULL,`latitude` double DEFAULT NULL,`longitude` double DEFAULT NULL,PRIMARY KEY (`id`)) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=latin1;";        mysqlCon.query(createQuery,function(err){
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
                    
                    var req = {};

                    try{
                        req = JSON.parse(""+data);
                    }
                    catch(e){
                        return;
                    }




                    if(req.requestType.toLowerCase() === "send"){
                        sock.write("Received chunk.");


                        for(var i = 0; i < req.dataSet.length; i++){
                            var HRObject = req.dataSet[i];
                            insertHRObject(HRObject);
                        }

                    }
                    else if(req.requestType.toLowerCase() === "receive"){
                        sock.write(JSON.stringify(dataSet));
                    }
                    else{
                        sock.write("Invalid command");
                    }
                  
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



