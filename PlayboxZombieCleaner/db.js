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
 *
 * @param callback
 */
function getAllLoggedUsers(callback) {

    var connection = connectToDB();

    connection.query("SELECT id FROM logged_users_tbl WHERE (last_action_date < (now() - interval 10 minute))",
        function(err, results) {

        if (err) {
            console.log(err);
            throw err;
        }

        connection.end();
        callback(false, results);
    });
}

/**
 *
 * @param callback
 */
function clearZombies (callback) {

    var connection = connectToDB();

    getAllLoggedUsers(function(err, results) {

        if (err) {
            console.log(err);
            throw err;
        }

        if (results.length > 0) {
            // meaning there are zombies to delete

            connection.query("DELETE FROM `logged_users_tbl` WHERE id IN (?)", [results],
                function(err, result) {
                    if (err) {
                        console.log(err);
                        throw err;
                    }

                    console.log("[" + result.affectedRows + "] Zombies were deleted successfully");
                    callback(false);
                });
        } else {
            // No zombies for now...
            console.log("No zombies were found...");
            callback(false);
        }
    });
}

exports.connectToDB = connectToDB;
exports.clearZombies = clearZombies;