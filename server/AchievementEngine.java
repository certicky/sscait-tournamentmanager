package server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import com.sun.xml.internal.ws.api.config.management.policy.ManagementAssertion.Setting;

import objects.Bot;
import objects.Game;
import objects.Map;

public class AchievementEngine {
	
	public static final int MINIMUM_GAMES_FOR_ACHIEVEMENT = 20;	// don'd add any achievements to bots with less games
	public static final int NUMBER_OF_DAYS = 4;					// number of days for Experienced, Veteran and Godlike achievements
	public static final int FLOWER_CHILD_NUMBER_DAYS = 1;			// number of days for Flower Child achievement
	
	private static final AchievementEngine INSTANCE = new AchievementEngine();
	private DatabaseLayer database = ServerSettings.Instance().database;
	private AchievementEngine() {
	}
	public static AchievementEngine Instance() 
	{
        return INSTANCE;
    }
	
	/*
	 * Returns current Unix timestamp in seconds.
	 */
	private int nowTimeStamp() {
		//java.util.Date date= new java.util.Date();
		//int nowTs = (int) new Timestamp(date.getTime()).getTime();
		//return nowTs;
		Date now = new Date();  	
		Long longTime = new Long(now.getTime()/1000);
		return longTime.intValue();
	}
	
	//==============================================================================
	
	/*
	 * Adds achievements to the bot after he's won the game.
	 */
	public void processWin(Bot bot) throws SQLException {
		
		// collect newly unlocked achievements in this array, so we can send them all in one email
		ArrayList<String> unlockedAchievements = new ArrayList<String>();
		
		// "I don't discriminate" Defeat all 3 races
		boolean zerg = false;
		boolean protoss = false;
		boolean terran = false;
		if (database.getNumRows("SELECT * FROM games, fos_user WHERE bot1='"+bot.getId()+"' and result='1' AND bot2=fos_user.id AND fos_user.bot_race='Zerg' LIMIT 1;") > 0) zerg = true;
	        if (database.getNumRows("SELECT * FROM games, fos_user WHERE bot2='"+bot.getId()+"' and result='2' AND bot1=fos_user.id AND fos_user.bot_race='Zerg' LIMIT 1;") > 0) zerg = true;
	        if (database.getNumRows("SELECT * FROM games, fos_user WHERE bot1='"+bot.getId()+"' and result='1' AND bot2=fos_user.id AND fos_user.bot_race='Protoss' LIMIT 1;") > 0) protoss = true;
	        if (database.getNumRows("SELECT * FROM games, fos_user WHERE bot2='"+bot.getId()+"' and result='2' AND bot1=fos_user.id AND fos_user.bot_race='Protoss' LIMIT 1;") > 0) protoss = true;
	        if (database.getNumRows("SELECT * FROM games, fos_user WHERE bot1='"+bot.getId()+"' and result='1' AND bot2=fos_user.id AND fos_user.bot_race='Terran' LIMIT 1;") > 0) terran = true;
	        if (database.getNumRows("SELECT * FROM games, fos_user WHERE bot2='"+bot.getId()+"' and result='2' AND bot1=fos_user.id AND fos_user.bot_race='Terran' LIMIT 1;") > 0) terran = true;
		if (zerg && protoss && terran) {
			if (addAchievement(bot,"equalOpportunity")) unlockedAchievements.add("equalOpportunity");
		}

		// Winning Streak 3/5/10
		String[] nums = {"3","5","10"};
		for (String number : nums) {
			boolean allWins = true;
			
	        Statement st1 = database.con.createStatement();
	        ResultSet rs1 = st1.executeQuery("SELECT * FROM games WHERE bot1='"+bot.getId()+"' OR bot2='"+bot.getId()+"' ORDER BY datetime DESC LIMIT "+number+";");
	        int numRows1 = 0;
	        while (rs1.next()) {
	        	numRows1 += 1;
	        	if (rs1.getString("bot1").equals(bot.getId()) && !rs1.getString("result").equals("1")) {
	        		allWins = false;
	        	}
	        	if (rs1.getString("bot2").equals(bot.getId()) && !rs1.getString("result").equals("2")) {
	        		allWins = false;
	        	}
	        }
	        st1.close();
	        rs1.close();
	        
			if (allWins == true && numRows1 == Integer.parseInt(number)) {
				if (addAchievement(bot, "winningStreak"+number)) unlockedAchievements.add("winningStreak"+number);
			}
			
		}

		// Experienced / Veteran / Godlike
		double wins   = database.getNumRows("SELECT game_id FROM `games` WHERE datetime > "+(nowTimeStamp()-86400*NUMBER_OF_DAYS)+" AND ((bot1='"+bot.getId()+"' AND result='1') or (bot2='"+bot.getId()+"' AND result='2'))");
	    double losses = database.getNumRows("SELECT game_id FROM `games` WHERE datetime > "+(nowTimeStamp()-86400*NUMBER_OF_DAYS)+" AND ((bot1='"+bot.getId()+"' AND result='2') or (bot2='"+bot.getId()+"' AND result='1'))");
		double draws  = database.getNumRows("SELECT game_id FROM `games` WHERE datetime > "+(nowTimeStamp()-86400*NUMBER_OF_DAYS)+" AND ((bot1='"+bot.getId()+"' AND result='draw') or (bot2='"+bot.getId()+"' AND result='draw'))");
		double winRate = wins / (wins+losses+draws) * 100;
		
		if (winRate >= 50) if (addAchievement(bot,"experienced")) unlockedAchievements.add("experienced");
		if (winRate >= 65) if (addAchievement(bot,"veteran")) unlockedAchievements.add("veteran");
		if (winRate >= 85) if (addAchievement(bot,"godlike")) unlockedAchievements.add("godlike");

		
		//  Piece of Cake / Let's Rock / Come Get Some / Damn, I'm Good!
        Statement st2 = database.con.createStatement();
        ResultSet rs2 = st2.executeQuery("SELECT count(game_id) FROM `games` WHERE ((bot1='"+bot.getId()+"' AND result='1') or (bot2='"+bot.getId()+"' AND result='2'))");
        rs2.next();
        int winsTotal = rs2.getInt("count(game_id)");
        st2.close();
        rs2.close();
        if (winsTotal >= 100) if (addAchievement(bot,"pieceOfCake")) unlockedAchievements.add("pieceOfCake");
	    if (winsTotal >= 500) if (addAchievement(bot,"letsRock")) unlockedAchievements.add("letsRock");
	    if (winsTotal >= 2000) if (addAchievement(bot,"comeGetSome")) unlockedAchievements.add("comeGetSome");
		if (winsTotal >= 5000) if (addAchievement(bot,"damnImGood")) unlockedAchievements.add("damnImGood");

		
		// vs Zerg/Terran/Toss 50/200/500
		String[] races = {"Zerg","Terran","Protoss"};
		String[] numbers = {"50","200","500"};
		for (String r : races) {
			for (String number : numbers) {
				String query = "SELECT datetime FROM games,fos_user WHERE (bot1='"+bot.getId()+"' AND result='1' AND bot2=fos_user.id AND fos_user.bot_race='"+r+"') OR (bot2='"+bot.getId()+"' AND result='2' AND bot1=fos_user.id AND fos_user.bot_race='"+r+"');";
				int winsRace = database.getNumRows(query);
				if (winsRace >= Integer.parseInt(number)) if (addAchievement(bot,"vs"+r+number)) unlockedAchievements.add("vs"+r+number);
			}
		}

		// Cheese!
		int halfHourWins = database.getNumRows("SELECT datetime FROM `games` WHERE datetime > "+(nowTimeStamp()-1800)+" AND ((bot1='"+bot.getId()+"' AND result='1') or (bot2='"+bot.getId()+"' AND result='2'))");
		if (halfHourWins >= 3) if (addAchievement(bot,"cheese")) unlockedAchievements.add("cheese");
		
		// send emails if something was unlocked
		sendNotificationsViaEmail(bot,unlockedAchievements);
	}

	//==============================================================================

	/*
	 * Adds achievements to the bot after he's lost the game.
	 */
	public void processLoss(Bot bot) throws SQLException {

		// collect newly unlocked achievements in this array, so we can send them all in one email
		ArrayList<String> unlockedAchievements = new ArrayList<String>();
		
		// send emails if something was unlocked
		sendNotificationsViaEmail(bot,unlockedAchievements);

	}
	
	//==============================================================================

	/*
	 * Adds achievements to the bot after a draw game.
	 */
	public void processDraw(Bot bot) throws SQLException {

		// collect newly unlocked achievements in this array, so we can send them all in one email
		ArrayList<String> unlockedAchievements = new ArrayList<String>();

		// Flower Child
        if (database.getNumRows("SELECT datetime FROM `games` WHERE datetime > "+String.valueOf(nowTimeStamp()-86400*FLOWER_CHILD_NUMBER_DAYS)+" AND ((bot1='"+bot.getId()+"' AND result='draw') or (bot2='"+bot.getId()+"' AND result='draw'))") >= 3) {
            addAchievement(bot,"flowerChild");
        }

		// send emails if something was unlocked
		sendNotificationsViaEmail(bot,unlockedAchievements);

	}
	
	/*
	 * Adds an achievement to DB if it's not already there and returns true if successfull.
	 */
	private boolean addAchievement(Bot bot, String type) throws SQLException {
		
		boolean ret = false;
		
		// don't do anything, if the bot has less than 10 finished games
		if (database.getNumberOfFinishedGames(bot) < MINIMUM_GAMES_FOR_ACHIEVEMENT) return false; 
		
		// if the bot already has this achievement, do nothing
		if (database.hasAchievement(bot, type)) return false;
		
		// add the achievement into DB if it's not already there
		java.util.Date date= new java.util.Date();
		String ts = String.valueOf(new Timestamp(date.getTime()).getTime() / 1000);
		database.customUpdate("INSERT INTO achievements (`type` ,`bot_id`, `datetime`) VALUES ('"+type+"', '"+bot.getId()+"', '"+ts+"');");
		ret = true;

		// check for the "Legendary" achievement (recursive call)
		if (!database.hasAchievement(bot, "legendary")) {
			int thisBotHas = database.getNumberOfBotsAchievements(bot);
			if (thisBotHas >= 22) addAchievement(bot, "legendary");
		}

		return ret;
	}
	
	// returns the link for sharing the achievement on Facebook
	private String getFBShareLink(String achTitle, String text, String type) {
		String ret;
		ret =  "https://www.facebook.com/dialog/feed?app_id=313159742390&link=http://www.sscaitournament.com/&name="+achTitle.replace(" ","%20");
		ret += "&picture=http://www.sscaitournament.com/images/achievements/"+type+".png";
		ret += "&caption=";
		ret += "My bot just unlocked the '"+achTitle+"' achievement ("+text.trim()+") in the SC tournament of Artificial Intelligences!".replace(" ","%20"); 
		ret += "&redirect_uri=http://www.sscaitournament.com/";
		return ret;
	}

	// Send mail notifications if we unlocked some achievements
	private void sendNotificationsViaEmail(Bot bot, ArrayList<String> unlockedAchievements) throws SQLException {
		
		// if sending emails to users isn't allowed in settings, don't do anything
		if (!ServerSettings.Instance().AllowEmailsToUsers) return;
		
		// if nothing new is unlocked, don't do anything
		if (unlockedAchievements.size() == 0) return;
		
		int total = database.getNumberOfAllPossibleAchievements();
		int have = database.getNumberOfBotsAchievements(bot);
        Statement st = null;
        ResultSet rs = null;

        
		String recipEmail = bot.getEmail();
        
		// single achievement
		if (unlockedAchievements.size() == 1) {

			st = this.database.con.createStatement();
	        rs = st.executeQuery("SELECT * FROM achievement_texts WHERE type='"+unlockedAchievements.get(0)+"' LIMIT 1;");
	        rs.next();

	        String subj = "Achievement unlocked: "+rs.getString("title")+" ("+have+"/"+total+")";
			String mess = "Congratulations!<br/>Your bot has just unlocked the <b>"+rs.getString("title")+"</b> achievement <b>("+rs.getString("text")+")</b><br/><a href=\""+getFBShareLink(rs.getString("title"),rs.getString("text"),unlockedAchievements.get(0))+"\">Share on FaceBook!</a>.";
			
	    	EmailUtil.sendEmailViaGmail(
	    			ServerSettings.Instance().GmailFromEmail, 
	    			recipEmail, 
	    			ServerSettings.Instance().GmailEmailPassword, 
	    			"SSCAIT: "+subj,
	    			mess);
			
		} else {
		// multiple achievements

	        String subj = unlockedAchievements.size() +" Achievements unlocked! ("+have+"/"+total+")";
			String mess = "Congratulations!<br/>Your bot has unlocked following "+unlockedAchievements.size()+" achievements:<br/><br/>";
			for (String ach : unlockedAchievements) {
				st = database.con.createStatement();
		        rs = st.executeQuery("SELECT * FROM achievement_texts WHERE type='"+ach+"' LIMIT 1;");
		        rs.next();
				mess += "<b>"+rs.getString("title")+" ("+rs.getString("text")+")</b> <a href=\""+getFBShareLink(rs.getString("title"),rs.getString("text"),ach)+"\">Share on FaceBook!</a><br/>";
			}
	    	EmailUtil.sendEmailViaGmail(
	    			ServerSettings.Instance().GmailFromEmail, 
	    			recipEmail, 
	    			ServerSettings.Instance().GmailEmailPassword, 
	    			"SSCAIT: "+subj,
	    			mess);
		}

		st.close();
        rs.close();
        
	}

	
}
