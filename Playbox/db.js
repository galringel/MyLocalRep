/** Global variables **/
var mysql;
var mysqlPool;

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
 * returns the date string in mySQL format as prototype for further use and fix localtime
 * @returns {string}
 */
Date.prototype.toMysqlFormat = function() {

    var date = fixUTCToLocalTimeZone(this);
    return date.getUTCFullYear() + "-" +
        twoDigits(1 + date.getUTCMonth()) + "-" +
        twoDigits(date.getUTCDate()) + " " +
        twoDigits(date.getUTCHours()) + ":" +
        twoDigits(date.getUTCMinutes()) + ":" +
        twoDigits(date.getUTCSeconds());
};


/**
 *
 * @returns {Date}
 */
function fixUTCToLocalTimeZone(date) {
    var utcOffSet = date.getTimezoneOffset();
    //Deal with dates in milliseconds for most accuracy

    if (utcOffSet < 0) {
        utcOffSet = Math.abs(utcOffSet);
    }

    var utc = date.getTime() + (utcOffSet * 60000);
    return new Date(utc);
}

/**
 *
 *
 */
function CreateMySqlConnectionPool() {

    mysql = require('mysql');
    mysqlPool  = mysql.createPool({
        host     : 'localhost',
        user     : 'root',
        password : 'pring-1',
        database : 'Playbox',
        waitForConnections : true,
        connectionLimit : 10,
        queueLimit : 0
    });

    // In order to avoid SQL Injection attacks
    mysqlPool.escape();
}

/**
 * Gets all users from database
 */
function getUserProfile(user_id, token, callback) {

    mysqlPool.getConnection(function (err, connection) {

        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var sqlGetProfile = "SELECT fb_first_name, fb_last_name from users_tbl where oauth_uid=?";
            connection.query(sqlGetProfile, [user_id], function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    updateLoggedUsersActionDate(user_id, token, function(err) {
                        if (err) {
                            console.log(err);
                            callback(err);
                        }
                    });

                    if (result.length > 0) {
                        callback(false, result[0]);
                    } else {
                        callback(false, null);
                    }
                }
            });
        }
    });
}

/**
 * Get oauth_uid from database by facebook_id
 * @param facebook_id
 */
function getOAuthUidByFacebookId(facebook_id) {

    mysqlPool.getConnection(function (err, connection) {

        if (err)  {
            console.log(err);
            throw err;
        } else {
            var sqlGetOAuthUid = "SELECT oauth_uid from users_tbl WHERE fb_id=?";
            connection.query(sqlGetOAuthUid, [facebook_id], function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    throw err;
                }

                var oauth_uid = null;
                if (result.length  > 0) {
                    // User is found in db
                    oauth_uid = result[0].oauth_uid;
                }

                return oauth_uid;
            });
        }
    });
}

/**
 * Gets a user_id and table_id and add him to the table
 * Thats how we can monitor which users are currently playing and on which tables
 * @param user_id
 * @param table_id
 * @param token
 * @param callback
 */
function addUserToTable(user_id, token, table_id, callback) {

    mysqlPool.getConnection(function (err, connection) {

        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var values  = {
                oauth_uid: user_id,
                table_id : table_id
            };

            connection.query('INSERT INTO baccarat_oauthid_tableid_mapping_tbl SET ?', values, function(err) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    updateLoggedUsersActionDate(user_id, token, function(err) {
                        if (err) {
                            console.log(err);
                            callback(err);
                        }
                    });

                    callback(false);
                }
            });
        }
    });
}

/**
 *
 * @param user_id
 * @param callback
 */
function isUserInATable(user_id, callback) {

    mysqlPool.getConnection(function (err, connection) {

        if (err)  {
            console.log(err);
            callback(err);
        } else {
            connection.query('SELECT table_id FROM baccarat_oauthid_tableid_mapping_tbl WHERE oauth_uid=?',
                [user_id], function(err, result) {

                    connection.release();

                    if (err) {
                        console.log(err);
                        callback(err);
                    } else {
                        var res = false;
                        if (result.length > 0) {
                            res = true;
                        }

                        callback(false, res);
                    }
                });
        }
    });
}

/**
 *
 * @param callback
 * @param token
 */
function getOpenTables(token, callback) {

    mysqlPool.getConnection(function (err, connection) {

        if (err)  {
            console.log(err);
            callback(err);
        } else {
            connection.query('SELECT * FROM baccarat_tables_tbl', function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    getLoggedUserIdByToken(token, function(err, result) {
                        if (err) {
                            console.log(err);
                            callback(err);
                        }

                        if (result == null) {
                            // user is not logged in, do nothing.
                        } else {
                            updateLoggedUsersActionDate(result, token, function(err) {
                                if (err) {
                                    console.log(err);
                                    callback(err);
                                }
                            });
                        }
                    });

                    callback(false, result);
                }
            });
        }
    });
}

/**
 *
 * @param user_id
 * @param callback
 * @param token
 */
function removeUserFromTable(user_id, token, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        }

        var sqlRemoveUserFromTable = "DELETE FROM baccarat_oauthid_tableid_mapping_tbl WHERE oauth_uid=?";
        connection.query(sqlRemoveUserFromTable, [user_id], function(err, result) {

            connection.release();
            if (err) {
                console.log(err);
                callback(err);
            } else {
                updateLoggedUsersActionDate(user_id, token, function(err) {
                    if (err) {
                        console.log(err);
                        callback(err);
                    }
                });

                callback(false, result);
            }
        });
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

    var date = new Date().toMysqlFormat();
    var values  = {
        game_id: game_id,
        table_id : table_id,
        game_status : game_status,
        date_created : date
    };

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            connection.query('INSERT INTO baccarat_games_tbl SET ?', values, function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false, result);
                }
            });
        }
    });
}

/**
 *
 * @param game_id
 * @param callback
 */
function getGameRecordById(game_id, callback) {

    mysqlPool.getConnection(function (err, connection) {

        if (err) {
            console.log(err);
            callback(err);
        } else {
            connection.query('SELECT * FROM baccarat_games_tbl WHERE game_id=?', [game_id], function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false, result);
                }
            });
        }
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

    var datetime = new Date().toMysqlFormat();

    // making sure client exists.
    getLoggedUserIdByOAuthUid(oauth_uid, function(err, result) {
        if (err) {
            console.log(err);
            callback(err);
        } else {
            if (result == null) {
                // user is not exists. nothing to do.
                callback(new Error("The given user id is not exists"));
            } else {

                mysqlPool.getConnection(function (err, connection) {
                    if (err)  {
                        console.log(err);
                        callback(err);
                    } else {
                        var sqlUpdateLoggedUserActionDate = "UPDATE logged_users_tbl SET last_action_date=? " +
                            "WHERE oauth_uid=? AND token=?";

                        connection.query(sqlUpdateLoggedUserActionDate, [datetime, oauth_uid, token], function(err, result) {

                            connection.release();
                            if (err) {
                                console.log(err);
                                callback(err);
                            } else {
                                callback(false, result);
                            }
                        });
                    }
                });
            }
        }
    });
}

/**
 * Happens after getting two types of events from VIVO'S server:
 * 1. "No more bets" - We change status to "Game Started"
 * 2. "Game Result" - We change status to "Game Ended"
 * @param game_id
 * @param game_status
 * @param callback
 */
function updateGameStatus(game_id, game_status, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {

            // Updating the game id status
            var sqlUpdateGameStatus = "UPDATE baccarat_games_tbl SET game_status = ? WHERE game_id = ?";
            connection.query(sqlUpdateGameStatus, [game_status, game_id], function(err) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false);
                }
            });
        }
    });
}


function insertANewBet(params, callback) {

    var values  = {
        oauth_uid : params.oauth_uid,
        game_id: params.game_id,
        table_id : params.table_id,
        banker_bet : params.banker,
        tie_bet : params.tie,
        player_bet : params.player
    };

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            connection.query('INSERT INTO baccarat_bets_tbl SET ?', values, function(err) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    updateLoggedUsersActionDate(params.oauth_uid, params.token, function(err) {
                        if (err) {
                            console.log(err);
                            callback(err);
                        } else {
                            callback(false);
                        }
                    });
                }
            });
        }
    });
}

/**
 * Update the bet after the game is finished with the result:
 * "P" = Player, "T" = Tie, "B" = "Banker"
 * @param game_id
 * @param bet_result
 * @param callback
 */
function updateABetWithResult(game_id, bet_result, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            // Updating the game id status
            var sqlUpdateBetResult = "UPDATE baccarat_bets_tbl SET bet_result = ? WHERE game_id = ?";
            connection.query(sqlUpdateBetResult, [bet_result, game_id], function(err) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false);
                }
            });
        }
    });
}

/**
 *
 * @param user_id
 * @param callback
 * @param token
 */
function getChipsBalanceById(user_id, token, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var sqlGetChipsBalanceById = "SELECT balance from balance_info_tbl where oauth_uid=?";
            connection.query(sqlGetChipsBalanceById, user_id, function(err, result) {

                connection.release();
                if (err)  {
                    console.log(err);
                    callback(err);
                } else {
                    updateLoggedUsersActionDate(user_id, token, function(err) {
                        if (err) {
                            console.log(err);
                            callback(err);
                        }else {
                            callback(false, result[0].balance);
                        }
                    });
                }
            });
        }
    });

}

/**
 *
 * @param value
 * @param oauth_uid
 * @param callback
 */
function updateChipsBalanceById(oauth_uid, value, callback) {

        console.log("updateChipsBalanceById");
        // calculate new balance
        mysqlPool.getConnection(function (err, connection) {
            if (err)  {
                console.log(err);
                callback(err);
            } else {
                // Updating token and last_entered date
                var sqlUpdateFields = "UPDATE balance_info_tbl SET balance=? WHERE oauth_uid=?";
                connection.query(sqlUpdateFields, [value, oauth_uid], function(err) {

                    connection.release();
                    if (err) {
                        console.log(err);
                        callback(err);
                    } else {
                        callback(false);
                    }
                });
            }
        });
}

/**
 *
 * @param user_id
 * @param value
 * @param callback
 */
function addChipsToUserId (user_id, value, callback) {

    var values  = {
        oauth_uid : user_id,
        balance : value
    };

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            connection.query('INSERT INTO balance_info_tbl SET ?', values, function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false, result);
                }
            });
        }
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
    var values  = {
        oauth_uid : user_id,
        token : token,
        user_agent : user_agent,
        logged_date : current_date,
        last_action_date : current_date
    };

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            connection.query('INSERT INTO logged_users_tbl SET ?', values, function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false, result);
                }
            });
        }
    });
}

function removeLoggedUser (token, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var sqlRemoveLoggedUser = "DELETE FROM logged_users_tbl WHERE token=?";
            connection.query(sqlRemoveLoggedUser, [token], function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false, result);
                }
            });
        }
    });
}

/**
 * get token and return oauth_uid.
 * if not found, return null
 * @param callback
 * @param oauth_uid
 */
function getLoggedUserIdByOAuthUid(oauth_uid, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            connection.query('SELECT oauth_uid FROM logged_users_tbl WHERE oauth_uid=?', [oauth_uid], function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    var user_id = null;
                    if (result.length  > 0) {
                        user_id = result[0].oauth_uid;
                    }

                    callback(false, user_id);
                }
            });
        }
    });
}

function getLoggedUserIdByToken(token, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            connection.query('SELECT oauth_uid FROM logged_users_tbl WHERE token=?', [token], function(err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    var oauth_uid = null;
                    if (result.length  > 0) {
                        oauth_uid = result[0].oauth_uid;
                    }

                    callback(false, oauth_uid);
                }
            });
        }
    });
}

/**
 *
 * @param mobile_type
 * @param mobile_value
 * @param callback
 */
function insertANewGuestUser(mobile_type, mobile_value, callback) {

    var datetime = new Date().toMysqlFormat();
    var provider = "Guest";

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var sqlIsGuestExists = "SELECT mobile_identifier_value FROM users_tbl WHERE mobile_identifier_value=?";
            connection.query(sqlIsGuestExists, [mobile_value], function (err, result) {
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    if (result.length  > 0) {
                        console.log("user already exists \r\n");

                        // Maybe updates UA also? (maybe create a table for user-agents)
                        // This way we can know all the user devices.

                        // Updating last_entered date
                        var sqlUpdateFields = "UPDATE users_tbl SET date_last_entered = ? WHERE mobile_identifier_value = ?";
                        connection.query(sqlUpdateFields, [datetime,mobile_value], function(err, result) {

                            connection.release();
                            if (err) {
                                console.log(err);
                                callback(err);
                            } else {
                                callback(false, result);
                            }
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

                            connection.release();
                            if (err) {
                                console.log(err);
                                callback(err);
                            } else {
                                callback(false, result);
                            }
                        });
                    }
                }
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

    var datetime = new Date().toMysqlFormat();

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var sqlGetUserQuery = "SELECT fb_id, oauth_uid FROM users_tbl WHERE fb_id=?";
            connection.query(sqlGetUserQuery,[profile.id], function (err, result) {

                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    if (result.length  > 0) {
                        console.log("user already exists \r\n");
                        var facebook_id = result[0].fb_id;
                        var oauth_uid = result[0].oauth_uid;

                        // Updating token and last_entered date
                        var sqlUpdateFields = "UPDATE users_tbl SET date_last_entered = ?,fb_token= ? WHERE fb_id = ?";
                        connection.query(sqlUpdateFields, [datetime,profile._fbToken,facebook_id], function(err, result) {

                            connection.release();
                            if (err) {
                                console.log(err);
                                callback(err);
                            } else {
                                callback(err, result, oauth_uid);
                            }
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

                            connection.release();
                            if (err) {
                                console.log(err);
                                callback(err);
                            } else {
                                callback(err, result);
                            }
                        });
                    }
                }
            });
        }
    });
}

function getLastGameWinner(table_id, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var sqlGetLastGameWinner = "SELECT last_winner FROM baccarat_status WHERE table_id=?";
            connection.query(sqlGetLastGameWinner,[table_id], function (err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false,result)
                }
            });
        }
    });
}


function getBetByGameIdAndTableId (game_id, table_id, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var sqlGetTableStatus = "SELECT * FROM baccarat_bets_tbl WHERE table_id = ? and gameId = ?";
            connection.query(sqlGetTableStatus,[table_id, game_id], function (err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false,result)
                }
            });
        }
    });
}

/**
 *
 * @param table_id
 * @param callback
 */
function getTableStatus(table_id, callback) {

    mysqlPool.getConnection(function (err, connection) {
        if (err)  {
            console.log(err);
            callback(err);
        } else {
            var sqlGetTableStatus = "SELECT * FROM baccarat_status WHERE table_id=?";
            connection.query(sqlGetTableStatus,[table_id], function (err, result) {

                connection.release();
                if (err) {
                    console.log(err);
                    callback(err);
                } else {
                    callback(false,result)
                }
            });
        }
    });
}

exports.CreateMySqlConnectionPool = CreateMySqlConnectionPool;

// Users actions
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
exports.getGameRecordById = getGameRecordById;
exports.getLastGameWinner = getLastGameWinner;

// Bets actions
exports.insertANewBet = insertANewBet;
exports.updateABetWithResult = updateABetWithResult;
exports.getBetByGameIdAndTableId = getBetByGameIdAndTableId;

// Logged users actions
exports.addLoggedUser = addLoggedUser;
exports.removeLoggedUser = removeLoggedUser;
exports.getLoggedUserIdByToken = getLoggedUserIdByToken;
exports.getLoggedUserIdByOAuthUid = getLoggedUserIdByOAuthUid;
exports.updateLoggedUsersActionDate = updateLoggedUsersActionDate;

// Table Status API
exports.getTableStatus = getTableStatus;