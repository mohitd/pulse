var commandLineArgs = require('command-line-args');

var net = require('net');
 
var cli = commandLineArgs([
  { name: 'address', alias: 'a', type: String, defaultOption: 'localhost' },
])

var options = cli.parse();

console.log(options);

var HOST = options.address;
var PORT = 6969;


var dataSet = [];

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

        try {
            req = JSON.parse(data);
        } catch (e) {
            console.log(e);
            return;
        }

        if(req.requestType === "send"){
            sock.write("Received chunk.");
            dataSet = dataSet.concat(req.dataSet);            
        }
        else if(req.requestType === "receive"){
            sock.write(JSON.stringify(dataSet));
        }
        else{
            sock.write("Invalid command");
        }

        sock.end();
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

