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
 * Select users that weren't active for 30 min and more...
 * @param callback
 */
function getAllLoggedUsers(callback) {

    var connection = connectToDB();

    connection.query("SELECT id FROM logged_users_tbl WHERE (last_action_date > (now() - interval 30 minute))",
        function(err, results) {

            if (err) {
                console.log(err);
                throw err;
            }

            connection.end();

            // Formatting the result to one big array.
            var ids = [];

            for(var key in results) {
                ids.push(results[key].id);
            }


            callback(false, ids);
    });
}

/**
 *
 * @param callback
 */
function clearZombiesFromLoggedUsers (callback) {

    var connection = connectToDB();

    getAllLoggedUsers(function(err, results) {

        if (err) {
            console.log(err);
            throw err;
        }

        if (results.length > 0) {
            // meaning there are zombies to delete

            connection.query("DELETE FROM logged_users_tbl WHERE id IN (?)", [results],
                function(err, result) {
                    if (err) {
                        console.log(err);
                        throw err;
                    }

                    var numZombiesCleared = result.affectedRows;
                    callback(false, numZombiesCleared);
                });
        } else {
            // No zombies for now...
            console.log("No zombies were found...");
            callback(false, 0);
        }
    });
}

exports.connectToDB = connectToDB;
exports.clearZombiesFromLoggedUsers = clearZombiesFromLoggedUsers;