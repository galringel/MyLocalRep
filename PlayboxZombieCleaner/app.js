/**
 * Module dependencies.
 */
var express = require('express');
//var routes = require('./routes');
var db = require("./db");
var user = require('./routes/user');
var http = require('http');
var path = require('path');

var app = express();

// all environments
app.set('port', process.env.PORT || 3001);
app.set('views', path.join(__dirname, 'views'));
app.set('view engine', 'jade');
app.use(express.favicon());
app.use(express.logger('dev'));
app.use(express.bodyParser());
app.use(express.methodOverride());
app.use(app.router);
app.use(express.static(path.join(__dirname, 'public')));

// development only
if ('development' == app.get('env')) {
  app.use(express.errorHandler());
}

app.get('/', function(req, res){

    // Do Nothing.
    res.end("Clearing Zombies ;)");
});

// Clearing Zombies every 1 min for now.
var timeoutInMilli = 10000;
function ClearZombies() {

    setInterval(function () {
        console.log("Timeout elapsed. " + timeoutInMilli + " milli passed. going to clear some zombies! :)");

        db.clearZombiesFromLoggedUsers(function(err, numZombiesCleared) {
            if (err) {
                console.log(err);
            }

            console.log("[" + numZombiesCleared + "] Zombies were deleted successfully");
        });
    }, timeoutInMilli);
}

app.get('/users', user.list);

http.createServer(app).listen(app.get('port'), function(){
  console.log('Express server listening on port ' + app.get('port'));

    ClearZombies();
});
