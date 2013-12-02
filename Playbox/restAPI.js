var db = require("./db");

/**
 *
 * @param res
 * @param params
 */
function getChipsBalance(params, res) {

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        if (result == null) {
            res.json({'error' : 'token did not match to any user...'});
        } else {
            db.getChipsBalanceById(result, params.token, function (err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                }

                if (result == null) {
                    res.json({'error' : 'user requested has no chips attached'});
                } else {
                    res.json({'chips' : result});
                }
            });
        }
    });
}

/**
 *
 * @param res
 * @param params
 */
function joinATable(res, params) {

    // First we check if user is already playing at a table.
    // If so: we delete him from there and add him to the new table
    // (later on.. we will check if connection was disconnected in the middle and we need to return him
    // to the right table)

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        var user_id = result;
        if (result == null) {
            res.json({'error' : 'token did not match to any user...'});
        } else {
            db.isUserInATable(user_id, function(err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                }

                if (result) {
                    console.log("user was found in a table... removing...");
                    // User is found, we need to remove
                    db.removeUserFromTable(user_id, params.token, function (err) {
                        if (err) {
                            console.log(err);
                            throw err;
                        } else {

                            console.log("user removed from old table... inserting to a new table");
                            db.addUserToTable(user_id, params.token, params.table_id);
                            console.log("user_id = [" + user_id + "] was added successfully to table_id=[" +
                                params.table_id + "]");
                            res.json({'Result' : 'OK, user added'});
                        }
                    });
                } else {
                    db.addUserToTable(user_id, params.token, params.table_id);
                    console.log("user_id = [" + user_id + "] was added successfully to table_id=[" +
                        params.table_id + "]");
                    res.json({'Result' : 'OK, user added'});
                }
            });
        }
    });
}

/**
 *
 * @param res
 * @param params
 */
function leaveATable(res, params) {

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        if (result == null) {
            res.json({'error' : 'token did not match to any user...'});
        } else {
            // User is found, we need to remove
            db.removeUserFromTable(result, params.token, function (err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                } else {
                    if (result.affectedRows == 0) {
                        res.json({'Result' : 'FALSE, user is not in any table'});
                    } else {
                        res.json({'Result' : 'OK, user removed'});
                    }
                }
            });
        }
    });
}

function getUserProfile(params, res) {

    db.getLoggedUserIdByToken(params.token, function (err, result) {
        if (err) {
            console.log(err);
            throw err;
        }

        if (result == null) {
            res.json({'error' : 'token did not match to any user...'});
        } else {
            db.getUserProfile(result, function (err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                }

                if (result == null) {
                    res.json({'error' : 'user was not found...'});
                } else {
                    res.json({
                        'Firstname' : result.fb_first_name,
                        'Lastname' : result.fb_last_name
                    });
                }
            });
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
            throw err;
        }

        if (result == null) {
            res.json({'error' : 'token did not match to any user...'});
        } else {
            db.getOpenTables(params.token, function (err, result) {
                if (err) {
                    console.log(err);
                    throw err;
                }

                if (result == null) {
                    res.json({'error' : 'There are no open tables'});
                } else {
                    res.json(result);
                }
            });
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