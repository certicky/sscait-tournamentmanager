package server;

import java.security.Timestamp;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.Vector;

import objects.Bot;
import objects.Game;
import objects.Map;

public class DatabaseLayer {

	public Connection con = null;
	
	public DatabaseLayer(String databaseAddress, int databasePort,
			String databaseName, String databaseUser, String databasePassword) {
		// try to connect to DB
		String url = "jdbc:mysql://"+databaseAddress+":"+String.valueOf(databasePort)+"/"+databaseName;
		try {
            this.con = DriverManager.getConnection(url, databaseUser, databasePassword);
        } catch (SQLException ex) {
            System.err.println(ex.getMessage() +"\n"+ ex);
            // hint
            if (ex.getMessage().contains("suitable driver")) {
            	System.out.println("\nHINT: This might be a problem with unavailable mysql-connector-java.jar. Make sure it's accessible and that libmysql-java is installed on the system.");
            }
        	System.exit(-1);
        } 
	}
	
	/*
	 * Disables a bot specified by botId and sends a mail to the admin.
	 */
	private void disableBot(String botId, String name, String race, String botEmail) throws SQLException {
        Statement st = this.con.createStatement();
        st.executeUpdate("UPDATE fos_user SET bot_enabled='0' WHERE id='"+botId+"' LIMIT 1;");
        st.close();
        EmailUtil.sendEmailViaGmail(
	        ServerSettings.Instance().GmailFromEmail, 
			ServerSettings.Instance().AdminEmail, 
			ServerSettings.Instance().GmailEmailPassword, 
			"SSCAIT: Bot disabled ("+name+")",
			"Bot has been disabled because it was inactive (score 350/450/550/650 while its opponent didn't crash) for a few games in a row.\n\nID: "+botId+"\nName: "+name+"\nRace: "+race+"\nEmail: "+botEmail);
	}
	
	private boolean isInitialScore(int score) {
		// return (score == 350 || score == 450 || score == 550 || score == 650);

		// now that we consider raze + kill score, 0 indicates the initial amount
		return (score == 0);
	}
	
	/*
	 * Checks for inactive bots (not doing anything for 2 games in a row) and disables them.
	 */
	public void disableInactiveBots(Game g) throws SQLException
	{
		// TODO: commented out until we figure out why bots still get disabled when they shouldn't
		// disableInactiveBot(g.getHomebot().getId(), g.getHomebot().getName(), g.getHomebot().getRace(), g.getHomebot().getEmail());
		// disableInactiveBot(g.getAwaybot().getId(), g.getAwaybot().getName(), g.getAwaybot().getRace(), g.getAwaybot().getEmail());
	}

	private void disableInactiveBot(String botId, String name, String race, String botEmail) throws SQLException {
		// disable a bot after N inactive games
		int disableAfterNumberOfInactiveGames = 2;

		// gets the number of inactive games from the the last <disableAfterNumberOfInactiveGames> games for bot
		Statement statement = this.con.createStatement();
		ResultSet resultSet = statement.executeQuery("SELECT COUNT(1) as inactiveGamesCount FROM games g1 WHERE ((g1.bot1 = '" + String.valueOf(botId) + "' AND g1.bot1Score = 0 AND g1.bot2Score > 0) OR (g1.bot2 = '" + String.valueOf(botId) + "' AND g1.bot2Score = 0 AND g1.bot1Score > 0)) AND g1.result != 'unfinished' AND g1.game_id >= (SELECT MIN(innerGameId) FROM (SELECT g2.game_id as innerGameId FROM games g2 WHERE (g2.bot1 = '" + String.valueOf(botId) + "' OR g2.bot2 = '" + String.valueOf(botId) + "') AND (g2.bot1Score > 0 OR g2.bot2Score > 0) AND g2.result != 'unfinished' ORDER BY g2.game_id DESC LIMIT " + String.valueOf(disableAfterNumberOfInactiveGames) + ") as innerGame);");
        resultSet.next();
        int inactiveGamesCount = resultSet.getInt("inactiveGamesCount");

		if (inactiveGamesCount == disableAfterNumberOfInactiveGames) {
			System.out.println("Disabling bot " + botId + " due to inactivity.");
			disableBot(botId, name, race, botEmail);
		}

		statement.close();
		resultSet.close();
	}

	/*
	 * Schedules some more games (adds them to DB).
	 */
	public void scheduleMoreGames() throws SQLException {
		
		int gamesToAdd = 10;
		Random rand = new Random();
		Vector<Bot> allBots = this.getAllEnabledBots();
		
		// add "gamesToAdd" new games
		for (int i = 0; i <gamesToAdd; i++) {
	        // select random map
	        Vector<Map> maps = ServerSettings.Instance().MapVector;
	        Map randMap = maps.get(rand.nextInt(maps.size()));
	        
	        // select random bots
	        Bot bot1 = allBots.get(rand.nextInt(allBots.size()));
	        Bot bot2 = null;
	        while (bot2 == null || bot2 == bot1) bot2 = allBots.get(rand.nextInt(allBots.size()));
	        
	        // insert the game into DB
	        Statement st = this.con.createStatement();
	        st.executeUpdate("INSERT INTO games (`game_id`,`datetime`,`bot1`,`bot2`,`map`,`result`,`note`,`bot1score`,`bot2score`) VALUES (NULL,'0','"+bot1.getId()+"','"+bot2.getId()+"','"+randMap.getMapLocation()+"','unfinished','',0,0);");
	        st.close();
		}
        
	}
	 
	/*
	 * Returns the instance of Bot classs, based on bot ID.
	 */
	public Bot getBotById(int botId) throws SQLException {
        Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT id,full_name,bot_race,bot_type,bot_path,email FROM fos_user WHERE id='"+String.valueOf(botId)+"' LIMIT 1;");
        rs.next();
        Bot ret = new Bot(rs.getInt("id"), rs.getString("full_name"), rs.getString("bot_race"), rs.getString("bot_type"), rs.getString("bot_path"), rs.getString("email")); 
        st.close();
        rs.close();
        return ret;
	}
	
	/*
	 * Returns the next scheduled match, according to database.
	 * If needed, initiates scheduling of new games.
	 */
	public Game getNextGame() throws SQLException {

        Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT game_id,bot1,bot2,map FROM games WHERE result='unfinished' ORDER BY game_id ASC;");
        
        // get vector of all the scheduled games
        Vector<Game> games = new Vector<>();
        while (rs.next()) {
        	games.add(new Game(rs.getInt("game_id"), 0, getBotById(rs.getInt("bot1")), getBotById(rs.getInt("bot2")), new Map(rs.getString("map"))));
        }
        
        // if there are too few games, schedule some more (only if not in competitive phase)
        if (games.size() < 7 && !ServerSettings.Instance().CompetitivePhase) scheduleMoreGames();
        
        // if (for some reason) there are no games, return null
        if (games.size() == 0) {
        	// if this is competitive phase, send email to admin and shut down
        	if (ServerSettings.Instance().CompetitivePhase) {
	        	EmailUtil.sendEmailViaGmail(
		    			ServerSettings.Instance().GmailFromEmail, 
		    			ServerSettings.Instance().AdminEmail, 
		    			ServerSettings.Instance().GmailEmailPassword, 
		    			"SSCAIT: ALL GAMES FINISHED (shutting down the server)",
		    			"All the games have been played and, since the server ran in Competitive Phase mode, more games are not scheduled. Shutting down the server."
		    			);
	        	System.exit(0);
        	}
        	return null;
        }
        
        st.close();
        rs.close();
        
		return games.get(0);
	}

	/*
	 * Returns an instance of Game class, based on gameID.
	 */
	public Game getGameById(int gameID) throws SQLException {
		Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT game_id,bot1,bot2,map FROM games WHERE game_id='"+String.valueOf(gameID)+"' LIMIT 1;");
        rs.next();
        Game ret = new Game(gameID, 0, getBotById(rs.getInt("bot1")), getBotById(rs.getInt("bot2")), new Map(rs.getString("map")));
        st.close();
        rs.close();
        return ret;
	}
	
	/*
	 * Returns the vector of all enabled bots.
	 */
	public Vector<Bot> getAllEnabledBots() throws SQLException {
        Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT id,full_name,bot_race,bot_type,bot_path,email FROM fos_user WHERE bot_enabled='1';");
        Vector<Bot> bots = new Vector<>();
        while (rs.next()) {
        	bots.add(new Bot(rs.getInt("id"), rs.getString("full_name"), rs.getString("bot_race"), rs.getString("bot_type"), rs.getString("bot_path"), rs.getString("email")));
        }
        st.close();
        rs.close();
        return bots;
	}

	/*
	 * Saves the game result to database.
	 */
	public synchronized void saveGameResult(int gameID, String result, String note, int bot1Score, int bot2Score) throws SQLException {

		long timestamp = System.currentTimeMillis() / 1000;
	
		Statement st = this.con.createStatement();
		if (bot1Score != 0) {
			st.executeUpdate("UPDATE games SET result='"+result+"', note='"+note+"', datetime='"+String.valueOf(timestamp)+"', bot1score='"+String.valueOf(bot1Score)+"' WHERE game_id='"+String.valueOf(gameID)+"';");
		} else if (bot2Score != 0) {
			st.executeUpdate("UPDATE games SET result='"+result+"', note='"+note+"', datetime='"+String.valueOf(timestamp)+"', bot2score='"+String.valueOf(bot2Score)+"' WHERE game_id='"+String.valueOf(gameID)+"';");
		} else {
			st.executeUpdate("UPDATE games SET result='"+result+"', note='"+note+"', datetime='"+String.valueOf(timestamp)+"', bot1score='"+String.valueOf(bot1Score)+"', bot2score='"+String.valueOf(bot2Score)+"' WHERE game_id='"+String.valueOf(gameID)+"';");
		}
        
        st.close();
	}

	/*
	 * Sets the game result in DB according to higher in-game score.
	 */
	public synchronized void setResultByScore(int gameID) throws SQLException {

		Statement st = this.con.createStatement();
		st.executeUpdate("UPDATE games SET result='1' WHERE game_id='"+String.valueOf(gameID)+"' AND bot1score >= bot2score;");
		st.executeUpdate("UPDATE games SET result='2' WHERE game_id='"+String.valueOf(gameID)+"' AND bot1score <  bot2score;");
        st.close();
	}

	
	/*
	 * Performs cusotom UPDATE query on a database.
	 */
	public synchronized void customUpdate(String query) throws SQLException {
		Statement st = this.con.createStatement();
		st.executeUpdate(query);
        st.close();
	}
	
	/*
	 * Returns the number of finished games played by a certain bot.
	 */
	public synchronized int getNumberOfFinishedGames(Bot bot) throws SQLException {
		int ret;
        Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM games WHERE (bot1='"+bot.getId()+"' OR bot2='"+bot.getId()+"') AND result != 'unfinished';");
        rs.next();
        ret = rs.getInt("COUNT(*)");
        st.close();
        rs.close();
		return ret;
	}
	
	/*
	 * Returns true if certain bot already has certain achievement.
	 */
	public synchronized boolean hasAchievement(Bot bot, String achievementType) throws SQLException {
        boolean ret;
		Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM achievements WHERE bot_id='"+bot.getId()+"' AND type='"+achievementType+"';");
        rs.next();
        ret = rs.getInt("COUNT(*)") >= 1;
        st.close();
        rs.close();
        return ret;
	}

	/*
	 * Returns the number of all the achievements that a ceratin bot has.
	 */
	public synchronized int getNumberOfBotsAchievements(Bot bot) throws SQLException {
        int ret;
		Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM achievements WHERE bot_id='"+bot.getId()+"';");
        rs.next();
        ret = rs.getInt("COUNT(*)");
        st.close();
        rs.close();
        return ret;
	}

	/*
	 * Returns the number of all the possible achievements in the system.
	 */
	public synchronized int getNumberOfAllPossibleAchievements() throws SQLException {
        int ret;
		Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM achievement_texts;");
        rs.next();
        ret = rs.getInt("COUNT(*)");
        st.close();
        rs.close();
        return ret;
	}	

	/*
	 * Returns the bot's ID based on its name.
	 */
	public int getBotIdFromName(String botName) throws SQLException {
        Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery("SELECT id FROM fos_user WHERE full_name='"+botName+"' LIMIT 1;");
        rs.next();
        int ret = rs.getInt("id");
        st.close();
        rs.close();
		return ret;
	}
	
	/*
	 * Returns number of rows resulting from specified SELECT query.
	 */
	public synchronized int getNumRows(String selectQuery) throws SQLException {
        Statement st = this.con.createStatement();
        ResultSet rs = st.executeQuery(selectQuery);
        int ret = 0;
        while (rs.next()) {
        	ret += 1;
        }
        st.close();
        rs.close();
        return ret;
	}


}
