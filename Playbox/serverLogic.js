var db = require("./db");
function joinATable() {

    // Do the following:
    // Add a new record of the user_id and table_id to: "baccarat_oauthid_tableid_mapping_tbl"

    // TODO: change the user_id ad table_id to real ones
    db.addUserToTable(1,1);

}

function betStarted() {

    // Do the following:
    // - Create a new game record in baccarat_games_tbl with status: "BetStrated"
}

function betEnded() {

    // Do the following:
    // - Create new bet record for each play
    // - Updates the user's chips balance in balance_tbl
    // - Updates baccarat_games_tbl status with: "BetEnded"
}

function gameEnded() {

    // Do the following:
    // - Updates baccarat_games_tbl status with: "GameEnded"
    // - Updates the user's chips balance in balance_tbl
}

exports.joinATable = joinATable;
exports.betStarted = betStarted;
exports.betEnded = betEnded;
exports.gameEnded = gameEnded;