function connect_to_db() {
    var mysql = require('mysql');
    var connection = mysql.createConnection({
        host     : 'localhost',
        user     : 'root',
        password : 'pring-1',
        database : 'Playbox'
    });

    connection.connect(function(err) {
        if (err) throw err;
    });

    return connection;
}

function select_all_users() {

    var connection = connect_to_db();
    connection.query('SELECT * from users_tbl', function(err, result) {
        if (err) {
            console.log(error);
        }

        console.log('The result is: ', result[0]);
        connection.end();
    });
}

function addUserToTable(user_id, table_id) {

    var connection = connect_to_db();
    var sqlUpdateFields = "INSERT INTO baccarat_oauthid_tableid_mapping_tbl (id, oauth_id, table_id) VALUES ( " +
        "oauth_id=" + user_id + ", table_id="  + table_id + ")";

    connection.query(sqlUpdateFields, function(err, result) {
        if (err) {
            console.log(err);
        }

        connection.end();
    })
}

function getChipsBalanceById(user_id) {
    var connection = connect_to_db();
    var query = "SELECT balance from balance_info_tbl where oauth_uid=?";
    connection.query(query,[user_id], function(err, result) {
        if (err)  {
            console.log(error);
        }

        connection.end();
        return result[0].balance;
    });
}

function updateChipsBalanceById(user_id, value) {
    var connection = connect_to_db();

    // Gets the current chips balance
    var currentBalance = getChipsBalanceById(user_id);
    var newBalance = currentBalance - value;

    // Updating token and last_entered date
    var sqlUpdateFields = "UPDATE balance_info_tbl SET balance = ? WHERE oauth_id = ?";
    connection.query(sqlUpdateFields, [user_id, newBalance], function(err, result) {
        if (err) {
            console.log(err);
        }

        connection.end();
    })
}

function insertANewFacebookUser(profile) {

    var connection = connect_to_db();
    var datetime = new Date();

    var sqlGetUserQuery = "SELECT fb_id FROM users_tbl WHERE fb_id = '"+profile.id+"'";
    connection.query(sqlGetUserQuery, function (error, results) {
        if (error) {
            console.log(error);
        }

        if (results.length  > 0) {
            console.log("user already exists \r\n");
            var facebook_id = results[0].fb_id;

            // Maybe updates UA also? (maybe create a table for user-agents)
            // This way we can know all the user devices.

            // Updating token and last_entered date
             var sqlUpdateFields = "UPDATE users_tbl SET date_last_entered = ?,fb_token= ? WHERE fb_id = ?";
             connection.query(sqlUpdateFields, [datetime,profile._fbToken,facebook_id], function(err, result) {
                 if (err) {
                     console.log(err);
                 }
             })
        } else {
            console.log('user is not exists -> inserting...');

            var sql = "INSERT INTO users_tbl " +
                "(oauth_uid, " +
                "oauth_provider, " +
                "mobile_identifier_type, " +
                "mobile_identifier_value," +
                "fb_id, " +
                "fb_token, " +
                "fb_username, " +
                "fb_first_name, " +
                "fb_last_name, " +
                "fb_gender, " +
                "fb_email, " +
                "fb_country, " +
                "user_agent," +
                "date_registered, " +
                "date_last_entered) VALUES (";

            var values = null + ",'" +
                profile.provider + "'," +
                null + "," +
                null + "," +
                profile.id + ",'" +
                profile._fbToken + "','" +
                profile.username + "','" +
                profile.name.givenName + "','" +
                profile.name.familyName + "','" +
                profile.gender + "'," +
                (typeof profile.emails === "undefined" ? null : profile.emails) + "," +
                null + "," +
                null + "," +
                datetime + "," +
                datetime + ")";

            var sql_query = sql + values;
            connection.query(sql_query, function(err, result) {
                if (err) {
                    console.log(err);
                }

                console.log('The result is: ', result);
                //connection.end();
            });

            console.log("success! \r\n");
        }

        console.log(results);
    });
}

exports.select_all_users = select_all_users;
exports.insertANewFacebookUser = insertANewFacebookUser;
exports.getBalanceById = getChipsBalanceById;
exports.updateChipsBalanceById = updateChipsBalanceById;
exports.addUserToTable = addUserToTable;