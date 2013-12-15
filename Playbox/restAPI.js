var db = require("./db");
var gameLogic = require("./gameLogic");

/**
 *
 * @param res
 * @param params
 */
function getChipsBalance(params, res) {

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
        } else {
            if (result == null) {
                res.json({'ERROR' : 'token did not match to any user...'});
            } else {
                db.getChipsBalanceById(result, params.token, function (err, result) {
                    if (err) {
                        console.log(err);
                        res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                    } else {
                        if (result == null) {
                            res.json({'ERROR' : 'the given users has no chips at all! wrong user?'});
                        } else {
                            res.json({'chips' : result});
                        }
                    }
                });
            }
        }
    });
}

/**
 *
 * @param res
 * @param params
 */
function joinATable(params, res) {

    // First we check if user is already playing at a table.
    // If so: we delete him from there and add him to the new table
    // (later on.. we will check if connection was disconnected in the middle and we need to return him
    // to the right table)

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
        } else {
            var user_id = result;
            if (result == null) {
                res.json({'ERROR' : 'token did not match to any user...'});
            } else {
                db.isUserInATable(user_id, function(err, result) {
                    if (err) {
                        console.log(err);
                        res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                    } else {
                        if (result) {
                            console.log("user was found in a some table... removing...");
                            // User is found, we need to remove
                            db.removeUserFromTable(user_id, params.token, function (err) {
                                if (err) {
                                    console.log(err);
                                    res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                                } else {

                                    console.log("user removed from old table... adding to a new table");
                                    db.addUserToTable(user_id, params.token, params.table_id, function (err) {
                                        if (err) {
                                            console.log(err);
                                            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                                        } else {
                                            console.log("user_id = [" + user_id + "] was added successfully to table_id=[" +
                                                params.table_id + "]");
                                            res.json({'OK' : 'ser added'});
                                        }
                                    });
                                }
                            });
                        } else {
                            db.addUserToTable(user_id, params.token, params.table_id, function (err) {
                                if (err) {
                                    console.log(err);
                                    res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                                } else {
                                    console.log("user_id = [" + user_id + "] was added successfully to table_id=[" +
                                        params.table_id + "]");
                                    res.json({'OK' : 'user added'});
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
 * @param res
 * @param params
 */
function leaveATable(params, res) {

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
        } else {
            if (result == null) {
                res.json({'ERROR' : 'token did not match to any user...'});
            } else {
                // User is found, we need to remove
                db.removeUserFromTable(result, params.token, function (err, result) {
                    if (err) {
                        console.log(err);
                        res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                    } else {
                        if (result.affectedRows == 0) {
                            res.json({'ERROR' : 'the given user is not playing in any table'});
                        } else {
                            res.json({'OK' : 'user removed'});
                        }
                    }
                });
            }
        }
    });
}

function getUserProfile(params, res) {

    db.getLoggedUserIdByToken(params.token, function (err, oauth_uid) {
        if (err) {
            console.log(err);
            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
        } else {
            if (oauth_uid == null) {
                res.json({'ERROR' : 'token did not match to any user...'});
            } else {
                db.getUserProfile(oauth_uid, params.token, function (err, result) {
                    if (err) {
                        console.log(err);
                        res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                    } else {
                        if (result == null) {
                            res.json({'ERROR' : 'user was not found...'});
                        } else {
                            res.json({
                                'Firstname' : result.fb_first_name,
                                'Lastname' : result.fb_last_name
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
 * @param params
 * @param res
 */
function getBaccaratTables(params, res) {

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
        } else {
            if (result == null) {
                res.json({'ERROR' : 'token did not match to any user...'});
            } else {
                db.getOpenTables(params.token, function (err, result) {
                    if (err) {
                        console.log(err);
                        res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                    } else {
                        if (result == null) {
                            res.json({'ERROR' : 'There are no open tables'});
                        } else {
                            res.json(result);
                        }
                    }
                });
            }
        }
    });
}

/**
 *
 * @param params
 * @param res
 */
function getBaccaratTableStatus(params, res) {

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
        } else {
            if (result == null) {
                res.json({'ERROR' : 'token did not match to any user...'});
            } else {
                db.getTableStatus(params.table_id, function (err, result) {
                    if (err) {
                        console.log(err);
                        res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                    } else {
                        if (result.length == 0) {
                            res.json({'ERROR' : 'The given table is not exists'});
                        } else {
                            getStatus(params, result, res);
                        }
                    }
                });
            }
        }
    });
}

/**
 *
 * @param params
 * @param result
 * @param res
 */
function getStatus(params, result, res) {

    gameLogic.getStatus(params, result, function(err, result) {

        if (err) {
            console.log(err);
            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
        } else {
            if (result == "InProgress") {
                // some player connection is querying VIVO's server,
                // so we call the function again, until he finish and updates our db.
                // TODO: maybe use process.nextTick() instead of recursive call
                getBaccaratTableStatus(params, res);
            } else {
                res.json(result);
            }
        }
    });

}

function betEnded(params, res) {

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
        } else {
            if (result == null) {
                res.json({'ERROR' : 'token did not match to any user...'});
            } else {

                params["oauth_uid"] = result;
                gameLogic.betEnded(params, function(err) {
                    if (err) {
                        console.log(err);
                        res.json({'ERROR' : 'Something is wrong in database.', 'Exception' : err.message });
                    } else {
                        res.json({'OK' : 'betEnded was executed successfully'});
                    }
                });
            }
        }
    });
}

// Chips REST API actions
exports.getChipsBalance = getChipsBalance;

// Profile REST API actions
exports.getUserProfile = getUserProfile;

// Table REST API actions
exports.joinATable = joinATable;
exports.leaveATable = leaveATable;
exports.getBaccaratTables = getBaccaratTables;
exports.getBaccaratTableStatus = getBaccaratTableStatus;

// Game REST API Actions
exports.betEnded = betEnded;