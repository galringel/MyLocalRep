//var cp = require('child_process');
var db = require("./db");
var rAPI = require("./restAPI");
var gameLogic = require("./gameLogic");
var express = require('express')
  , passport = require('passport')
  , util = require('util')
  , FacebookStrategy = require('passport-facebook').Strategy;

var FACEBOOK_APP_ID = "190846697770134";
var FACEBOOK_APP_SECRET = "2bddd05d1fa7d137551ffd40ec5f603c";


// Passport session setup.
//   To support persistent Playbox sessions, Passport needs to be able to
//   serialize users into and deserialize users out of the session.  Typically,
//   this will be as simple as storing the user ID when serializing, and finding
//   the user by ID when deserializing.  However, since this example does not
//   have a database of user records, the complete Facebook profile is serialized
//   and deserialized.
passport.serializeUser(function(user, done) {
  done(null, user);
});

passport.deserializeUser(function(obj, done) {
  done(null, obj);
});

// Use the FacebookStrategy within Passport.
//   Strategies in Passport require a `verify` function, which accept
//   credentials (in this case, an accessToken, refreshToken, and Facebook
//   profile), and invoke a callback with a user object.
passport.use(new FacebookStrategy({
    clientID: FACEBOOK_APP_ID,
    clientSecret: FACEBOOK_APP_SECRET,
    callbackURL: "http://localhost:3000/auth/facebook/callback"
  },
  function(accessToken, refreshToken, profile, done) {
    // asynchronous verification, for effect...
    process.nextTick(function () {

        // for debug:
        console.log("\n Access Token: " + accessToken);

      // To keep the example simple, the user's Facebook profile is returned to
      // represent the logged-in user.  In a typical application, you would want
      // to associate the Facebook account with a user record in your database,
      // and return that user instead.
      return done(null, profile);
    });
  }
));

var app = express();

// configure Express
app.configure(function() {
  app.set('views', __dirname + '/views');
  app.set('view engine', 'ejs');
  app.use(express.logger());
  app.use(express.cookieParser());
  app.use(express.bodyParser());
  app.use(express.methodOverride());
  app.use(express.session({ secret: 'OMGPringlesNodeJS1234' }));
  // Initialize Passport!  Also use passport.session() middleware, to support
  // persistent Playbox sessions (recommended).
  app.use(passport.initialize());
  app.use(passport.session());
  app.use(app.router);
  app.use(express.static(__dirname + '/public'));
});

// Routes
app.get('/', function(req, res){
    res.render('index', { user: req.user });
});

app.get('/session/getInfo', ensureAuthenticated, function(req, res){

    res.json({'info' : 'logged-in successfully!', 'token': req.sessionID });
});

app.get('/account', ensureAuthenticated, function(req, res){
  res.render('account', { user: req.user });
});

app.get('/login', function(req, res){
  res.render('login', { user: req.user });
});

app.get('/guest', function(req, res){

    if(!req.query.hasOwnProperty('mobile_type') ||
        !req.query.hasOwnProperty('mobile_value')) {

        res.json({'error' : 'Bad GET parameters! expected: mobile_type, mobile_value'});
    } else {
        var params = {
            mobile_type : req.query.mobile_type,
            mobile_value : req.query.mobile_value
        };

        gameLogic.addAnewGuestUser(params, function(err) {
            if (err) {
                console.log(err);
                res.json({'ERROR' : 'Something went wrong', 'Exception' : err.message });
            } else {
                console.log("Guest user was added successfully");
                res.render('index', { user : params.mobile_value });
            }
        });
    }
});

app.get('/chips/getChips',ensureAuthenticated, function (req, res) {

    if (!req.query.hasOwnProperty('token')) {
        res.json({'ERROR' : 'Bad GET parameters! expected: token'});
    } else {
        var params = {
            token : req.query.token
        };

        rAPI.getChipsBalance(params, res);
    }
});

app.get('/profile/getProfile', ensureAuthenticated, function (req, res) {

    if (!req.query.hasOwnProperty('token')) {
        res.json({'ERROR' : 'Bad GET parameters! expected: token'});
    } else {
        var params = {
            token : req.query.token
        };

        rAPI.getUserProfile(params, res);
    }
});

app.post('/tables/join', ensureAuthenticated, function (req, res) {

    if(!req.body.hasOwnProperty('token') ||
        !req.body.hasOwnProperty('table_id')) {

        res.json({'ERROR' : 'Bad POST parameters! expected: token and table_id'});
    } else {
        var params = {
            token : req.body.token,
            table_id : req.body.table_id
        };

        rAPI.joinATable(params, res);
    }
});

app.get('/tables/leave', ensureAuthenticated, function (req, res) {

    if(!req.query.hasOwnProperty('token')) {

        res.json({'ERROR' : 'Bad GET parameters! expected: token'});
    } else {
        var params = {
            token : req.query.token
        };

        rAPI.leaveATable(params, res);
    }
});

app.get('/tables/getTables', ensureAuthenticated, function (req, res) {

    if(!req.query.hasOwnProperty('token')) {

        res.json({'ERROR' : 'Bad GET parameters! expected: token'});
    } else {
        var params = {
            token : req.query.token
        };

        rAPI.getBaccaratTables(params, res);
    }
});

app.get('/games/getBaccaratTableStatus', ensureAuthenticated, function (req, res) {

    if(!req.query.hasOwnProperty('token')  ||
        !req.query.hasOwnProperty('table_id')) {

        res.json({'ERROR' : 'Bad GET parameters! expected: token, table_id'});
    } else {

        var params = {
            token : req.query.token,
            table_id : req.query.table_id
        };

        // Adding game_id parameter if sent.
        if (req.query.hasOwnProperty("game_id")) {
            params["game_id"] = req.query.game_id;
        }

        rAPI.getBaccaratTableStatus(params, res);
    }
});

app.post('/games/betEnded', ensureAuthenticated, function (req, res) {

    if(!req.body.hasOwnProperty('token') ||
        !req.body.hasOwnProperty('table_id') ||
        !req.body.hasOwnProperty('game_id') ||
        !req.body.hasOwnProperty('banker') ||
        !req.body.hasOwnProperty('tie') ||
        !req.body.hasOwnProperty('player') ||
        !req.body.hasOwnProperty('player_pair') ||
        !req.body.hasOwnProperty('banker_pair')){

        res.json({'ERROR' : 'Bad POST parameters! expected: token and table_id'});
    } else {
        var params = {
            token : req.body.token,
            table_id : req.body.table_id,
            game_id : req.body.game_id,
            banker : req.body.banker,
            tie : req.body.tie,
            player : req.body.player,
            player_pair: req.body.player_pair,
            banker_pair: req.body.banker_pair
        };

        rAPI.betEnded(params, res);
    }
});

//   GET /auth/facebook
//   Use passport.authenticate() as route middleware to authenticate the
//   request.  The first step in Facebook authentication will involve
//   redirecting the user to facebook.com.  After authorization, Facebook will
//   redirect the user back to this application at /auth/facebook/callback
app.get('/auth/facebook',
  passport.authenticate('facebook'),
  function(req, res){
    // The request will be redirected to Facebook for authentication, so this
    // function will not be called.
  });

// GET /auth/facebook/callback
//   Use passport.authenticate() as route middleware to authenticate the
//   request.  If authentication fails, the user will be redirected back to the
//   Playbox page.  Otherwise, the primary route function function will be called,
//   which, in this example, will redirect the user to the home page.
app.get('/auth/facebook/callback', 
  passport.authenticate('facebook', { failureRedirect: '/login' }),
  function(req, res) {

      var user_agent = req.headers['user-agent'];
      gameLogic.addANewFacebookUser(req.user, user_agent, function(err, result, oauth_uid) {
          if (err) {
              console.log(err);
              res.json({'ERROR' : 'could not add new Facebook User', 'Exception' : err.message });
          } else {
              // user oauth_uid;
              var associated_oauth_uid = oauth_uid;
              // get the session token id;
              var token = req.sessionID;

              db.getLoggedUserIdByToken(token, function(err, result) {

                  if (err) {
                      console.log(err);
                      res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                  } else {
                      if (result == null) {
                          // user is not added as logged in
                          db.addLoggedUser(associated_oauth_uid, token, user_agent, function(err) {
                              if (err) {
                                  console.log(err);
                                  res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                              } else {
                                  res.redirect("/session/getInfo");
                              }
                          });
                      } else {
                          // user is already logged in,
                          // we just need to update it datetime
                          db.updateLoggedUsersActionDate(associated_oauth_uid, token, function(err) {
                              if (err) {
                                  console.log(err);
                                  res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                              } else {
                                  res.redirect("/session/getInfo");
                              }
                          });
                      }
                  }
              });
          }
      });
  });

app.get('/logout', function(req, res){
  req.logout();
  res.redirect('/');
});

process.on('uncaughtException', function (err) {
    console.error((new Date).toUTCString() + ' uncaughtException:', err.message)
    console.error(err.stack)
    process.exit(1)
});

process.on('SIGTERM', function () {
    console.log("Closing server...");
    app.close();
});

app.on('close', function () {
    console.log("Closed");
    redis.quit();
});

// ============================
app.listen(3000, function() {

    console.log("Server is up and running on port: 3000\r\n");
    db.CreateMySqlConnectionPool();
});

// Simple route middleware to ensure user is authenticated.
//   Use this route middleware on any resource that needs to be protected.  If
//   the request is authenticated (typically via a persistent Playbox session),
//   the request will proceed.  Otherwise, the user will be redirected to the
//   Playbox page.
function ensureAuthenticated(req, res, next) {
  if (req.isAuthenticated()) { return next(); }
  res.redirect('/login')
}