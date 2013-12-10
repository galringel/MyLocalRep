var db = require("./db");

/**
 * Generate a random string at length 15
 * @returns {string}
 */
function generateGameId()
{
    var text = "";
    var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    for(var i = 0; i < 15; i++ ) {
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
            console.log(err);
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
                    throw err;
                }

                console.log("chips were added successfully for the new user!");
                callback(false, result, oauth_uid);
            });
        }

        callback(false, result, oauth_uid);
    });
}


function getStatus(params, result, callback) {

        var update_status = result[0].update_status;
        var table_status = result[0].table_status;
        var last_update_date = new Date(result[0].last_update_date).getTime();

        if (update_status == "Updated") {

            var now_date = new Date().getTime();
            var interval_check = 1000 * 20; // 20 seconds
            var isPast = (now_date - last_update_date >= interval_check);

            if (isPast) {
                // We need to do two things:
                // Update the table status as: "InProgress" (so other users won't query also)
                // Query nimi's server and update table info
                // TODO: using nimi's api
            }

            // our data in db is updated, lets return it to the client
            // but first, lets create a virtual game for the player.

            var isBetStarted = false;
            if (table_status == "BetStarted") {
                // No action from the client is needed, we just open a new virtual game,
                // and return the client gameId of it and more details.

                isBetStarted = true;
                betStarted(params.table_id, function (err, gameId) {
                    if (err) {
                        console.log(err);
                        throw err;
                    }
                    result[0]["gameId"] = gameId;
                    callback(false, result[0]);
                })
            }

            // If it was BetStarted we need to open a game and return GameId
            // Before returning gameStatus() to the client.
            if (!isBetStarted) {
                callback(false, result[0]);
            }
        } else if (update_status == "InProgress") {
            // Meaning some client already cause a query to nimi
            // we wait and then select data from DB again.
            callback(false, "InProgress");
        }
}


/**
 *
 * @param table_id
 * @param callback
 */
function betStarted(table_id, callback) {

    // Do the following:
    // - Generate a new game_id
    // - Create a new game record in baccarat_games_tbl with status: "BetStarted"

    var gameId = generateGameId();
    db.createANewGame(table_id, gameId, "BetStarted", function (err) {
        if (err) {
            console.log(err);
            throw err;
        }

        callback(false, gameId);
    });
}

/**
 *
 * @param params
 * @param callback
 */
function betEnded(params, callback) {

    // Do the following:
    // - Create new bet record for each play
    // - Updates the user's chips balance in balance_tbl
    // - Updates baccarat_games_tbl status with: "Bet_Ended"

    db.insertANewBet(params, function(err) {
        if (err) {
            console.log(err);
            callback(true, err);
        }
    });

    db.updateChipsBalanceById(user_id, bet_value);
    db.updateGameStatus(game_id, "Bet_Ended");
}

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

exports.addANewFacebookUser = addANewFacebookUser;
exports.addAnewGuestUser = addAnewGuestUser;

exports.getStatus = getStatus;
exports.betEnded = betEnded;