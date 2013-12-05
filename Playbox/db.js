/** Global variables **/

/**
 * formatting function to pad numbers to two digits.
 * @param d
 * @returns {*}
 */
function twoDigits(d) {
    if(0 <= d && d < 10) return "0" + d.toString();
    if(-10 < d && d < 0) return "-0" + (-1*d).toString();
    return d.toString();
}

/**
 * returns the date string in mySQL format as prototype for further use
 * @returns {string}
 */
Date.prototype.toMysqlFormat = function() {
    return this.getUTCFullYear() + "-" +
        twoDigits(1 + this.getUTCMonth()) + "-" +
        twoDigits(this.getUTCDate()) + " " +
        twoDigits(this.getUTCHours()) + ":" +
        twoDigits(this.getUTCMinutes()) + ":" +
        twoDigits(this.getUTCSeconds());
};

/**
 * Open a connection to our mySQL database
 * @returns {*}
 */
function connectToDB() {

    var mysql = require('mysql');
    var connection = mysql.createConnection({
        host     : 'localhost',
        user     : 'root',
        password : 'pring-1',
        database : 'Playbox'
    });

    // In order to avoid SQL Injection attacks
    connection.escape();

    connection.connect(function(err) {
        if (err) {
            console.log(err);
            throw err;
        }
    });

    return connection;
}

/**
 * Gets all users from database
 */
function getAllUsers() {

    var connection = connectToDB();
    connection.query('SELECT * from users_tbl', function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        //console.log('The result is: ', result[0]);
        connection.end();
        return result;
    });
}

/**
 * Gets all users from database
 */
function getUserProfile(user_id, token, callback) {

    var connection = connectToDB();
    var sqlGetProfile = "SELECT fb_first_name, fb_last_name from users_tbl where oauth_uid=?";
    connection.query(sqlGetProfile, [user_id], function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        updateLoggedUsersActionDate(user_id, token, function(err) {
            if (err) {
                console.log(err);
                throw err;
            }

            //connection.end();

            console.log("getUserProfile was executed, user action timestamp logged");
        });

        if (result.length > 0) {
            callback(false, result[0]);
        } else {
            callback(false, null);
        }
    });
}

/**
 * Get oauth_uid from database by facebook_id
 * @param facebook_id
 */
function getOAuthUidByFacebookId(facebook_id) {

    var connection = connectToDB();
    var sqlGetOAuthUid = "SELECT oauth_uid from users_tbl WHERE fb_id=?";
    connection.query(sqlGetOAuthUid, [facebook_id], function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        var oauth_uid = null;
        if (result.length  > 0) {
            // User is found in db
            oauth_uid = result[0].oauth_uid;
        }

        connection.end();
        return oauth_uid;
    });
}

/**
 * Gets a user_id and table_id and add him to the table
 * Thats how we can monitor which users are currently playing and on which tables
 * @param user_id
 * @param table_id
 * @param token
 */
function addUserToTable(user_id, token, table_id) {

    var connection = connectToDB();
    var values  = {
        oauth_uid: user_id,
        table_id : table_id
    };

    connection.query('INSERT INTO baccarat_oauthid_tableid_mapping_tbl SET ?', values, function(err) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();

        updateLoggedUsersActionDate(user_id, token, function(err) {
            if (err) {
                console.log(err);
                throw err;
            }

            console.log("getUserProfile was executed, user action timestamp logged");
        });
    });
}

/**
 *
 * @param user_id
 * @param callback
 */
function isUserInATable(user_id, callback) {

    var connection = connectToDB();
    connection.query('SELECT table_id FROM baccarat_oauthid_tableid_mapping_tbl WHERE oauth_uid=?',
        [user_id], function(err, result) {

        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
        var res = false;
        if (result.length > 0) {
            res = true;
        }

        callback(false, res);
    });
}

/**
 *
 * @param callback
 * @param token
 */
function getOpenTables(token, callback) {
    var connection = connectToDB();
    connection.query('SELECT * FROM baccarat_tables_tbl', function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
        getLoggedUserIdByToken(token, function(err, result) {

            if (err) {
                console.log(err);
                throw err;
            }

            connection.end();

            if (result == null) {
                // user is not logged in, do nothing.
            } else {
                updateLoggedUsersActionDate(result, token, function(err) {
                    if (err) {
                        console.log(err);
                        throw err;
                    }

                    console.log("getUserProfile was executed, user action timestamp logged");
                });
            }
        });

        callback(false, result);
    });
}

/**
 *
 * @param user_id
 * @param callback
 * @param token
 */
function removeUserFromTable(user_id, token, callback) {

    var connection = connectToDB();
    var sqlRemoveUserFromTable = "DELETE FROM baccarat_oauthid_tableid_mapping_tbl WHERE oauth_uid=?";

    connection.query(sqlRemoveUserFromTable, [user_id], function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();

        updateLoggedUsersActionDate(user_id, token, function(err) {
            if (err) {
                console.log(err);
                throw err;
            }

            console.log("getUserProfile was executed, user action timestamp logged");
        });

        callback(false, result);
    });
}

/**
 * Happens after getting "Bet Started" event From VIVO'S server
 * Status = "Bet Started"
 * @param game_id
 * @param table_id
 * @param game_status
 * @param callback
 */
function createANewGame(table_id, game_id, game_status, callback) {

    var connection = connectToDB();
    var values  = {
        game_id: game_id,
        table_id : table_id,
        game_status : game_status
    };

    connection.query('INSERT INTO baccarat_games_tbl SET ?', values, function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
        callback(false, result);
    });
}

/**
 * Each action the client side is making will update it timestamp,
 * In order to later on run a zombie cleaner on this date.
 * @param oauth_uid
 * @param callback
 * @param token
 */
function updateLoggedUsersActionDate(oauth_uid, token, callback) {

    var connection = connectToDB();
    var datetime = new Date().toMysqlFormat();

    // making sure client exists.
    getLoggedUserIdByOAuthUid(oauth_uid, function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        if (result == null) {
            // user is not exists
            // do nothing.
            console.log("tried to update logged user timestamp but it didn't exists.");
        } else {
            var sqlUpdateLoggedUserActionDate = "UPDATE logged_users_tbl SET last_action_date=? " +
                "WHERE oauth_uid=? AND token=?";

            connection.query(sqlUpdateLoggedUserActionDate, [datetime, oauth_uid, token], function(err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                }

                connection.end();
                callback(false, result);
            });
        }
    });
}

/**
 * Happens after getting two types of events from VIVO'S server:
 * 1. "No more bets" - We change status to "Game Started"
 * 2. "Game Result" - We change status to "Game Ended"
 * @param game_id
 * @param game_status
 */
function updateGameStatus(game_id, game_status) {

    var connection = connectToDB();

    // Updating the game id status
    var sqlUpdateGameStatus = "UPDATE baccarat_games_tbl SET status = ? WHERE game_id = ?";
    connection.query(sqlUpdateGameStatus, [game_status, game_id], function(err) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
    });
}

/**
 *
 * @param user_id
 * @param game_id
 * @param table_id
 * @param bet_type
 * @param bet_value
 */
function insertANewBet(user_id, game_id, table_id, bet_type, bet_value) {

    var connection = connectToDB();
    var values  = {
        oauth_uid : user_id,
        game_id: game_id,
        table_id : table_id,
        bet_type : bet_type,
        bet_value : bet_value
    };

    connection.query('INSERT INTO baccarat_bets_tbl SET ?', values, function(err) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();

        updateLoggedUsersActionDate(user_id, function(err) {
            if (err) {
                console.log(err);
                throw err;
            }

            console.log("getUserProfile was executed, user action timestamp logged");
        });
    });
}

/**
 * Update the bet after the game is finished with the result:
 * "Player", "Tie", "Banker"
 * @param game_id
 * @param bet_result
 */
function updateABetWithResult(game_id, bet_result) {

    var connection = connectToDB();

    // Updating the game id status
    var sqlUpdateBetResult = "UPDATE baccarat_bets_tbl SET bet_result = ? WHERE game_id = ?";
    connection.query(sqlUpdateBetResult, [bet_result, game_id], function(err) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
    });
}

/**
 *
 * @param user_id
 * @param callback
 * @param token
 */
function getChipsBalanceById(user_id, token, callback) {

    var connection = connectToDB();
    var sqlGetChipsBalanceById = "SELECT balance from balance_info_tbl where oauth_uid=?";
    connection.query(sqlGetChipsBalanceById, user_id, function(err, result) {
        if (err)  {
            console.log(err);
            throw err;
        }

        connection.end();

        updateLoggedUsersActionDate(user_id, token, function(err) {
            if (err) {
                console.log(err);
                throw err;
            }

            console.log("getUserProfile was executed, user action timestamp logged");
        });

        callback(false, result[0].balance);
    });
}

/**
 *
 * @param user_id
 * @param value
 * @param token
 */
function updateChipsBalanceById(user_id, token, value) {
    var connection = connectToDB();

    // Gets the current chips balance
    var currentBalance = getChipsBalanceById(user_id, token, null);
    var newBalance = currentBalance - value;

    // Updating token and last_entered date
    var sqlUpdateFields = "UPDATE balance_info_tbl SET balance=? WHERE oauth_id=?";
    connection.query(sqlUpdateFields, [user_id, newBalance], function(err) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
    });
}

/**
 *
 * @param user_id
 * @param value
 * @param callback
 */
function addChipsToUserId (user_id, value, callback) {

    var connection = connectToDB();
    var values  = {
        oauth_uid : user_id,
        balance : value
    };

    connection.query('INSERT INTO balance_info_tbl SET ?', values, function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
        callback(false, result);
    });
}

/**
 *
 * @param user_id
 * @param token
 * @param callback
 * @param user_agent
 */
function addLoggedUser (user_id, token, user_agent, callback) {

    var current_date = new Date().toMysqlFormat();
    var connection = connectToDB();
    var values  = {
        oauth_uid : user_id,
        token : token,
        user_agent : user_agent,
        logged_date : current_date,
        last_action_date : current_date
    };

    connection.query('INSERT INTO logged_users_tbl SET ?', values, function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
        callback(false, result);
    });
}

function removeLoggedUser (token, callback) {

    var connection = connectToDB();
    var sqlRemoveLoggedUser = "DELETE FROM logged_users_tbl WHERE token=?";

    connection.query(sqlRemoveLoggedUser, [token], function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
        callback(false, result);
    });
}

/**
 * get token and return oauth_uid.
 * if not found, return null
 * @param callback
 * @param oauth_uid
 */
function getLoggedUserIdByOAuthUid(oauth_uid, callback) {

    var connection = connectToDB();
    connection.query('SELECT oauth_uid FROM logged_users_tbl WHERE oauth_uid=?', [oauth_uid], function(err, result) {
            if (err) {
                console.log(err);
                throw err;
            }

            connection.end();

            var user_id = null;
            if (result.length  > 0) {
                user_id = result[0].oauth_uid;
            }

            callback(false, user_id);
        });
}

function getLoggedUserIdByToken(token, callback) {

    var connection = connectToDB();
    connection.query('SELECT oauth_uid FROM logged_users_tbl WHERE token=?', [token], function(err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();

        var oauth_uid = null;
        if (result.length  > 0) {
            oauth_uid = result[0].oauth_uid;
        }

        callback(false, oauth_uid);
    });
}

/**
 *
 * @param mobile_type
 * @param mobile_value
 * @param callback
 */
function insertANewGuestUser(mobile_type, mobile_value, callback) {

    var connection = connectToDB();
    var datetime = new Date().toMysqlFormat();
    var provider = "Guest";

    var sqlIsGuestExists = "SELECT mobile_identifier_value FROM users_tbl WHERE mobile_identifier_value=?";
    connection.query(sqlIsGuestExists, [mobile_value], function (err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        if (result.length  > 0) {
            console.log("user already exists \r\n");

            // Maybe updates UA also? (maybe create a table for user-agents)
            // This way we can know all the user devices.

            // Updating last_entered date
            var sqlUpdateFields = "UPDATE users_tbl SET date_last_entered = ? WHERE mobile_identifier_value = ?";
            connection.query(sqlUpdateFields, [datetime,mobile_value], function(err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                }

                connection.end();
                callback(false, result);
            });
        } else {

            console.log('user guest is not exists -> inserting...');
            var values  = {
                oauth_provider : provider,
                mobile_identifier_type: mobile_type,
                mobile_identifier_value : mobile_value,
                date_registered : datetime,
                date_last_entered : datetime
            };

            connection.query('INSERT INTO users_tbl SET ?', values, function(err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                }

                connection.end();
                callback(false, result);
            });
        }
    });
}

/**
 *
 * @param profile
 * @param callback
 * @param user_agent
 */
function insertANewFacebookUser(profile, user_agent, callback) {

    var connection = connectToDB();
    var datetime = new Date().toMysqlFormat();

    var sqlGetUserQuery = "SELECT fb_id, oauth_uid FROM users_tbl WHERE fb_id=?";
    connection.query(sqlGetUserQuery,[profile.id], function (err, result) {
        if (err) {
            console.log(err);
            callback(err, result);
        }

        if (result.length  > 0) {
            console.log("user already exists \r\n");
            var facebook_id = result[0].fb_id;
            var oauth_uid = result[0].oauth_uid;

            // Maybe updates UA also? (maybe create a table for user-agents)
            // This way we can know all the user devices.

            // Updating token and last_entered date
             var sqlUpdateFields = "UPDATE users_tbl SET date_last_entered = ?,fb_token= ? WHERE fb_id = ?";
             connection.query(sqlUpdateFields, [datetime,profile._fbToken,facebook_id], function(err, result) {
                 if (err) {
                     console.log(err);
                     throw err;
                 }

                 connection.end();
                 callback(err, result, oauth_uid);
             });
        } else {

            console.log('user is not exists -> inserting...');
            var values  = {
                oauth_provider : profile.provider,
                fb_token: profile._fbToken,
                fb_id : profile.id,
                fb_username : profile.username,
                fb_first_name : profile.name.givenName,
                fb_last_name : profile.name.familyName,
                fb_gender : profile.gender,
                fb_email : typeof profile.emails === "undefined" ? null : profile.emails,
                user_agent: user_agent,
                date_registered : datetime,
                date_last_entered : datetime
            };

            connection.query('INSERT INTO users_tbl SET ?', values, function(err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                }

                connection.end();
                callback(err, result);
            });
        }
    });
}

exports.connectToDB = connectToDB;

// Users table functions
exports.getAllUsers = getAllUsers;
exports.getOAuthUidByFacebookId = getOAuthUidByFacebookId;
exports.getUserProfile = getUserProfile;

// Adding new users
exports.insertANewFacebookUser = insertANewFacebookUser;
exports.insertANewGuestUser = insertANewGuestUser;

// Chips actions
exports.getChipsBalanceById = getChipsBalanceById;
exports.updateChipsBalanceById = updateChipsBalanceById;
exports.addChipsToUserId = addChipsToUserId;

// Table actions
exports.addUserToTable = addUserToTable;
exports.removeUserFromTable = removeUserFromTable;
exports.isUserInATable = isUserInATable;
exports.getOpenTables = getOpenTables;

//Game actions
exports.createANewGame = createANewGame;
exports.updateGameStatus = updateGameStatus;

// Bets actions
exports.insertANewBet = insertANewBet;
exports.updateABetWithResult = updateABetWithResult;

// Logged users actions
exports.addLoggedUser = addLoggedUser;
exports.removeLoggedUser = removeLoggedUser;
exports.getLoggedUserIdByToken = getLoggedUserIdByToken;
exports.getLoggedUserIdByOAuthUid = getLoggedUserIdByOAuthUid;
exports.updateLoggedUsersActionDate = updateLoggedUsersActionDate;