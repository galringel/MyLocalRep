var db = require("./db");

/**
 * Generate a random string at length 10
 * @returns {string}
 */
function generateGameId()
{
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for( var i=0; i < 10; i++ ) {
        text += possible.charAt(Math.floor(Math.random() * possible.length));
    }

    return text;
}

function addAnewGuestUser (params, callback) {

    db.insertANewGuestUser(params.mobile_type, params.mobile_value, function (err, result) {
        if (err) {
            console.log(err);
            callback(err, null);
        }

        // Extract last uid generated.
        var user_id = result.insertId;

        // Now we need to add user chips (!=0 meaning its a new user)
        if (user_id != 0) {
            var chipsForNewUser = 1000;
            db.addChipsToUserId(user_id, chipsForNewUser, function (err) {
                if (err) {
                    console.log(err);
                    callback(err);
                }

                console.log("chips were added successfully for the new user!");
                callback(false);
            });
        }

        callback(false);
    });
}

function addANewFacebookUser (facebook_profile, user_agent, callback) {

    db.insertANewFacebookUser(facebook_profile, user_agent, function(err, result, oauth_uid1) {
        if (err) {
            throw err;
        }

        var oauth_uid = oauth_uid1;

        // Extract last uid generated.
        var user_id = result.insertId;

        // Now we need to add user chips (!=0 meaning its a new user)
        if (user_id != 0) {
            var chipsForNewUser = 1000;
            db.addChipsToUserId(user_id, chipsForNewUser, function (err) {
                if (err) {
                    console.log(err);
                    callback(err);
                }

                console.log("chips were added successfully for the new user!");
                callback(false, result, oauth_uid);
            });
        }

        callback(false, result, oauth_uid);
    });
}

/**
 *
 * @param table_id
 */
function betStarted(table_id) {

    // Do the following:
    // - Generate a new game_id
    // - Create a new game record in baccarat_games_tbl with status: "Bet_Started"

    // TODO: think of a better uniqueId
    var gameId = generateGameId();
    var params = {
        token : req.query.token,
        table_id : req.query.table_id
    };
    db.createANewGame(table_id, gameId, "Bet_Started");
}


/**
 *
 * @param user_id
 * @param game_id
 * @param table_id
 * @param bet_type
 * @param bet_value
 */
function betEnded(user_id, game_id, table_id, bet_type, bet_value) {

    // Do the following:
    // - Create new bet record for each play
    // - Updates the user's chips balance in balance_tbl
    // - Updates baccarat_games_tbl status with: "Bet_Ended"

    db.insertANewBet(user_id, game_id, table_id, bet_type, bet_value);
    db.updateChipsBalanceById(user_id, bet_value);
    db.updateGameStatus(game_id, "Bet_Ended");
}

/**
 *
 * @param user_id
 * @param game_id
 * @param bet_result
 * @param bet_profit
 */
function gameEnded(user_id, game_id, bet_result, bet_profit) {

    // Do the following:
    // - Updates baccarat_games_tbl status with: "Game_Ended"
    // - Updates the bet record with the bet result
    // - Updates the user's chips balance in balance_tbl

    db.updateGameStatus(game_id, "Game_Ended");
    db.updateABetWithResult(game_id,  bet_result);

    // TODO: Find out how we get the player profit as well from VIVO'S server
    // If not, we need to calculate it by our self
    db.updateChipsBalanceById(user_id, bet_profit);
}

exports.betStarted = betStarted;
exports.betEnded = betEnded;
exports.gameEnded = gameEnded;

exports.addANewFacebookUser = addANewFacebookUser;
exports.addAnewGuestUser = addAnewGuestUser;