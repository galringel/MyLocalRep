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

            //var isBetStarted = false;
            if (table_status == "BetStarted") {

                // BetStarted can come in two ways:
                // 1. Fresh - BetStarted with no GameId, meaning it's the first game the player plays
                // 2. NewGame - BetStarted with GameId, meaning we need to close the last game, and open a new one.

                // If it's Fresh:
                // No action from the client is needed, we just open a new virtual game,
                // and return the client gameId of it and more details.

                // If it's NewGame:
                // We need to Update the last game result, and continue as usual

                if (typeof params.game_id != 'undefined') {
                    // Meaning it's case 2: "NewGame" and not fresh, we update last game result
                    gameEnded(params.oauth_uid, params.table_id, params.game_id,
                        params.token, function(err, gameEndedParams) {
                        if (err) {
                            console.log(err);
                            callback(err);
                        } else {
                            result[0]["last_winner_cards"] = gameEndedParams.last_winner_cards;
                            result[0]["profit"] = gameEndedParams.profit;
                        }
                    });
                }

                //isBetStarted = true;
                betStarted(params.table_id, function (err, gameId) {
                    if (err) {
                        console.log(err);
                        callback(err);
                    } else {

                        // Adding the generated gameId to the result
                        result[0]["gameId"] = gameId;
                        callback(false, result[0]);
                    }
                });
            } else if (table_status == "BetEnded") {
                // This case means the user did getStatus() while game is already running
                // We need to reply him that he needs to wait
                callback(false, result[0]);
            }

            // If it was BetStarted we need to open a game and return GameId
            // Before returning gameStatus() to the client, so we wait for betStarted() to finish
            // and we callback from there.
            //if (!isBetStarted) {
            //    callback(false, result[0]);
           // }
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

    db.getGameRecordById(params.game_id, function (err, gameId) {
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

/**
 *
 * @param oauth_uid
 * @param game_id
 * @param table_id
 * @param token
 * @param callback
 */
function gameEnded(oauth_uid, table_id, game_id, token, callback) {

    //var player_pair_ratio = 11;
    //var banker_pair_ratio = 11;

    // Get some information first:
    // - Get the winner from LastGameResult
    // - Get the user Bet from LastGame according to the last winner
    // - Calculate Profit

    db.getLastGameWinner(table_id, function (err, lastGameWinner) {

        if (err) {
            console.log(err);
            callback(err);
        } else {
            if (lastGameWinner.length == 0) {
                callback(new Error("There was no lastWinner details, something is wrong."));
            } else {
                var lastWinner = lastGameWinner[0].last_winner;
                var SplittedLastWinner = lastWinner.split(":");
                var winnerType = SplittedLastWinner[0];
                //var winnerCardValue = SplittedLastWinner[1];

                db.getBetByGameIdAndTableId(game_id, table_id, function(err, betRecord) {
                    if (err) {
                        console.log(err);
                        callback(err);
                    } else {
                        if (betRecord.length == 0) {
                            callback(new Error("There was no bet Record for the given gameId"));
                        } else {

                            var calculatedProfit = 0;
                            switch(winnerType)
                            {
                                case "B":
                                    // Banker
                                    var banker_ratio = 0.95;
                                    calculatedProfit = betRecord[0].banker_bet * banker_ratio;
                                    break;
                                case "P":
                                    // Player
                                    var player_ratio = 1;
                                    calculatedProfit = betRecord[0].player_bet * player_ratio;
                                    break;
                                case "T":
                                    // Tie
                                    var tie_ratio = 8;
                                    calculatedProfit = betRecord[0].tie_bet * tie_ratio;
                                    break;
                            }

                            // Now we do the following:
                            // - Updates baccarat_games_tbl status with: "GameEnded"
                            // - Updates the bet record with the bet result
                            // - Updates the user's chips balance in balance_tbl

                            db.updateGameStatus(game_id, "GameEnded", function(err) {
                                if (err) {
                                    console.log(err);
                                    callback(err);
                                } else {
                                    db.updateABetWithResult(game_id,  winnerType, function (err) {
                                        if (err) {
                                            console.log(err);
                                            callback(err);
                                        } else {

                                            db.getChipsBalanceById(oauth_uid, token, function(err, currentBalance) {

                                                if (err) {
                                                    console.log(err);
                                                    callback(err);
                                                } else {
                                                    var updatedBalance = addChips(currentBalance, calculatedProfit);

                                                    db.updateChipsBalanceById(oauth_uid, updatedBalance, function (err) {
                                                        if (err) {
                                                            console.log(err);
                                                            callback(err);
                                                        } else {

                                                            var gameEndedParams = {
                                                                last_winner_cards : lastWinner,
                                                                profit : calculatedProfit
                                                            };

                                                            callback(false, gameEndedParams);
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
        }
    });
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