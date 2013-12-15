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
            callback(err);
        } else {
            // Extract last uid generated.
            var user_id = result.insertId;

            // Now we need to add user chips (!=0 meaning its a new user)
            if (user_id != 0) {
                var chipsForNewUser = 1000;
                db.addChipsToUserId(user_id, chipsForNewUser, function (err) {
                    if (err) {
                        console.log(err);
                        callback(err);
                    } else {
                        console.log("chips were added successfully for the new user!");
                        //callback(false);
                    }
                });
            }
            //callback(false);
        }
    });
}

function addANewFacebookUser (facebook_profile, user_agent, callback) {

    db.insertANewFacebookUser(facebook_profile, user_agent, function(err, result, oauth_uid1) {
        if (err) {
            console.log(err);
            callback(err);
        } else {
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
                    } else {
                        console.log("chips were added successfully for the new user!");
                        callback(false, result, oauth_uid);
                    }
                });
            }

            callback(false, result, oauth_uid);
        }
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
                        callback(err);
                    } else {
                        result[0]["gameId"] = gameId;
                        callback(false, result[0]);
                    }
                });
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
            callback(err);
        } else {
            callback(false, gameId);
        }
    });
}

/**
 *
 * @param params
 * @param callback
 */
function betEnded(params, callback) {

    // Do the following:
    // - First we validate that the given GameId exists
    // - Create new bet record for each for the player
    // - Updates the player chips balance in balance_tbl
    // - Updates baccarat_games_tbl status with: "BetEnded"

    db.getGameId(params.game_id, function (err, gameId) {
        if (err) {
            console.log(err);
            callback(err);
        } else {
            if (gameId.length == 0) {
                callback(new Error("The given gameId is not exists!"));
            } else if (gameId[0].game_status == "BetEnded") {
                callback(new Error("Bets were already ended for the given GameId"));
            } else {
                db.insertANewBet(params, function(err1) {
                    if (err1) {
                        console.log(err1);
                        callback(err1);
                    } else {
                        // calculate total bet
                        var totalBet = parseInt(params.banker) + parseInt(params.tie) + parseInt(params.player);
                        db.getChipsBalanceById(params.oauth_uid, params.token, function(err2, balance) {
                            if (err2) {
                                console.log(err2);
                                callback(err2);
                            } else {
                                var newBalance = removeChips(balance, totalBet);
                                db.updateChipsBalanceById(params.oauth_uid, newBalance, function(err3) {
                                    if (err3) {
                                        console.log(err3);
                                        callback(err3);
                                    } else {
                                        db.updateGameStatus(params.game_id, "BetEnded", function (err4) {
                                            if (err4) {
                                                console.log(err4);
                                                callback(err4);
                                            } else {
                                                callback(false);
                                            }
                                        });
                                    }
                                });
                            }
                        });
                    }
                });
            }
        }
    });
}

function gameEnded(user_id, game_id, bet_result) {

    // Do the following:
    // - Updates baccarat_games_tbl status with: "GameEnded"
    // - Updates the bet record with the bet result
    // - Updates the user's chips balance in balance_tbl

    db.updateGameStatus(game_id, "GameEnded");
    db.updateABetWithResult(game_id,  bet_result);

    var player_pair_ratio = 11;
    var player_ratio = 1;
    var tie_ratio = 8;
    var banker_ratio = 0.95;
    var banker_pair_ratio = 11;
    
    // TODO: calculate
    var bet_profit;

    // If not, we need to calculate it by our self
    db.updateChipsBalanceById(user_id, bet_profit);
}

/**
 *
 * @param balance
 * @param value
 * @returns {number}
 */
function removeChips(balance, value) {

    // For security reasons, we do Math.abs(value) on every given value
    // Because if we want to decrease balance, we can get from the user: "-50" instead of "50"
    // (some man in the middle) and it be: balance - (-50) = +50.
    return (balance - Math.abs(value));
}

/**
 *
 * @param balance
 * @param value
 * @returns {number|string}
 */
function addChips(balance, value) {

    // For security reasons, we do Math.abs(value) on every given value
    // Because if we want to decrease balance, we can get from the user: "-50" instead of "50"
    // (some man in the middle) and it be: balance - (-50) = +50.
    return (balance + Math.abs(value));
}

exports.addANewFacebookUser = addANewFacebookUser;
exports.addAnewGuestUser = addAnewGuestUser;

exports.getStatus = getStatus;
exports.betEnded = betEnded;