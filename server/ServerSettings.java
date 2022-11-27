package server;

import java.io.*;

import objects.TournamentModuleSettingsMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import objects.BWAPISettings;
import objects.Map;

public class ServerSettings
{
	
	public Vector<Map> 		MapVector 			= new Vector<Map>();
	public String			ServerDir			= "./";
	public String			ServerReplayDir		= "N/A";
	public String			ServerRequiredDir	= "N/A";
	public String			ServerBotDir		= "N/A";
	public String			ServerLogFilePath	= "./logfile.txt";
	public int				ServerPort			= -1;

	// related to VM restarts (external commands to run)
	public String[]			PreRestartCommand	= new String[] {};
	public String[]			RestartAllCommand	= new String[] {};
	
	public String			DatabaseAddress		= "localhost";
	public int				DatabasePort		= 3306;
	public String			DatabaseName		= "defaultdbname";
	public String			DatabaseUser		= "defaultuser";
	public String			DatabasePassword	= "defaultpassword";
	public DatabaseLayer	database; 			
	public boolean			BreakTies			= false;
	public String			GmailFromEmail		= "default@gmail.com";
	public String			GmailEmailPassword	= "defaultgmailpassword";
	public String			AdminEmail			= "admin@email.com";
	public boolean			AllowEmailsToUsers	= false;
	public int				RestartAfterGames	= 0;
	public boolean			CompetitivePhase	= false;
	
	public BWAPISettings	bwapi = new BWAPISettings();
	public TournamentModuleSettingsMessage tmSettings = new TournamentModuleSettingsMessage();
	private static final ServerSettings INSTANCE = new ServerSettings();
	
	private ServerSettings()
	{
	}
	
	public static ServerSettings Instance() 
	{
        return INSTANCE;
    }
	
	public void parseSettingsFile(String filename)
	{
		try
		{
			boolean error = false;
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			
			while ((line = br.readLine()) != null)
			{
				line = line.trim();
				
				if (line.startsWith("#") || line.length() == 0)
				{
					continue;
				}
				
				// if parseLine is false there was an error
				if (!parseLine(line))
				{
					error = true;
				}
			}
			
			br.close();
			
			if (error)
			{
				System.exit(-1);
			}
		}
		catch (Exception e)
		{
			System.err.println("Error parsing settings file, exiting\n");
			e.printStackTrace();
			System.exit(-1);
		}
		
		if (!checkValidSettings())
		{
			System.err.println("\n\nError in server set-up, please check documentation: http://webdocs.cs.ualberta.ca/~cdavid/starcraftaicomp/tm.shtml#ss");
			System.exit(0);
		}
	}
	
	private boolean checkValidSettings()
	{
		boolean valid = true;
		
		// check if all setting variables are valid
		if (MapVector.size() <= 0)		{ System.err.println("ServerSettings: Must have at least 1 map in settings file"); valid = false; }
		if (ServerDir == null)			{ System.err.println("ServerSettings: ServerDir not specified in settings file"); valid = false; }
		if (ServerPort == -1)			{ System.err.println("ServerSettings: ServerPort must be specified as an integer in settings file"); valid = false; }
		
		// check if all required files are present
		if (!new File(ServerReplayDir).exists()) 	{ System.err.println("ServerSettings: Replay Dir (" + ServerReplayDir + ") does not exist"); valid = false; }
		if (!new File(ServerBotDir).exists()) 		{ System.err.println("ServerSettings: Bot Dir (" + ServerBotDir + ") does not exist"); valid = false; }
		if (!new File(ServerRequiredDir).exists()) 	{ System.err.println("ServerSettings: Required Files Dir (" + ServerRequiredDir + ") does not exist"); valid = false; }
		
		// Check if all the maps exist
		for (Map m : MapVector)
		{
			String mapLocation = ServerRequiredDir + "Starcraft/" + m.getMapLocation();
			if (!new File(mapLocation).exists())
			{
				System.err.println("Map Error: " + m.getMapName() + " file does not exist at specified location: " + mapLocation); valid = false;
			}
		}
		
		return valid;
	}
	
	private boolean parseLine(String line) throws Exception
	{
		
		boolean valid = true;

		// split line to parts (key, values)
		List<String> parts = new ArrayList<String>();
		Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(line);
		while (m.find()) parts.add(m.group(1).replace("\"", ""));
		String key = parts.get(0);
		parts.remove(0);
		List<String> values = parts;

		
		// Paths (absolute) to server requirements, bots and replays
		if (key.equalsIgnoreCase("ServerRequirementsDir")) {
			ServerRequiredDir = values.get(0);
		}
		else if (key.equalsIgnoreCase("BotsDir")) {
			ServerBotDir = values.get(0);
		}
		else if (key.equalsIgnoreCase("ReplaysDir")) {
			ServerReplayDir = values.get(0);
		}
		else if (key.equalsIgnoreCase("LogFilePath")) {
			ServerLogFilePath = values.get(0);
		}
		else if (key.equalsIgnoreCase("CompetitivePhase")) {
			CompetitivePhase = Boolean.valueOf(values.get(0));
		}

		else if (key.equalsIgnoreCase("RestartAfterGames")) {
			RestartAfterGames = Integer.valueOf(values.get(0));
		}
		else if (key.equalsIgnoreCase("PreRestartCommand")) {
			PreRestartCommand = values.get(0).split("\\s+"); 
		}
		else if (key.equalsIgnoreCase("RestartAllCommand")) {
			RestartAllCommand = values.get(0).split("\\s+");
		}

		
		// Database connection settings
		else if (key.equalsIgnoreCase("DatabaseAddress")) {
			DatabaseAddress = values.get(0);
		}
		else if (key.equalsIgnoreCase("DatabasePort")) {
			DatabasePort = Integer.valueOf(values.get(0));
		}
		else if (key.equalsIgnoreCase("DatabaseName")) {
			DatabaseName = values.get(0);
		}
		else if (key.equalsIgnoreCase("DatabaseUser")) {
			DatabaseUser = values.get(0);
		}
		else if (key.equalsIgnoreCase("DatabasePassword")) {
			DatabasePassword = values.get(0);
		}

		else if (key.equalsIgnoreCase("GmailFromEmail")) {
			GmailFromEmail = values.get(0);
		}
		else if (key.equalsIgnoreCase("GmailEmailPassword")) {
			GmailEmailPassword = values.get(0);
		}
		else if (key.equalsIgnoreCase("AdminEmail")) {
			AdminEmail = values.get(0);
		}
		else if (key.equalsIgnoreCase("AllowEmailsToParticipants")) {
			AllowEmailsToUsers = Boolean.valueOf(values.get(0));
		}

		
		// Deciding of Draw games using score
		else if (key.equalsIgnoreCase("BreakTies")) {
			BreakTies = Boolean.valueOf(values.get(0));
		}
		
		else if (key.equalsIgnoreCase("Map"))
		{
			MapVector.add(new Map(values.get(0)));
		}
		else if (key.equalsIgnoreCase("ServerPort"))
		{
			try
			{
				ServerPort = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: ServerPort option must be a valid integer port on your system");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMLocalSpeed"))
		{
			try
			{
				tmSettings.LocalSpeed = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMLocalSpeed must be an integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMFrameSkip"))
		{
			try
			{
				tmSettings.FrameSkip = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMFrameSkip must be an integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMGameFrameLimit"))
		{
			try
			{
				tmSettings.GameFrameLimit = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMGameFrameLimit must be an integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMRWSecondsNoKills"))
		{
			try
			{
				tmSettings.NoKillsTimeoutRealSecs = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMRWSecondsNoKills must be an integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMTimeout"))
		{
			try
			{
				int limit = Integer.parseInt(values.get(0));
				int bound = Integer.parseInt(values.get(1));
				
				tmSettings.TimeoutLimits.add(limit);
				tmSettings.TimeoutBounds.add(bound);
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMTimeout must be two integers");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMCameraMoveTime"))
		{
			try
			{
				tmSettings.CameraMoveTime = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMCameraMoveTime must be integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMCameraMoveTimeMin"))
		{
			try
			{
				tmSettings.CameraMoveTimeMin = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMCameraMoveTimeMin must be integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMZeroSpeedTime"))
		{
			try
			{
				tmSettings.ZeroSpeedTime = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMZeroSpeedTime must be integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMInitMaxSpeedTime"))
		{
			try
			{
				tmSettings.InitMaxSpeedTime = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMInitMaxSpeedTime must be integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMNoCombatSpeedUpTime"))
		{
			try
			{
				tmSettings.NoCombatSpeedUpTime = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMNoCombatSpeedUpTime must be integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMNoCombatSpeedUpDelay"))
		{
			try
			{
				tmSettings.NoCombatSpeedUpDelay = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMNoCombatSpeedUpDelay must be integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMScreenWidth"))
		{
			try
			{
				tmSettings.ScreenWidth = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMScreenWidth must be an integer");
				valid = false;
			}
		}
		else if (key.equalsIgnoreCase("TMScreenHeight"))
		{
			try
			{
				tmSettings.ScreenHeight = Integer.parseInt(values.get(0));
			}
			catch (Exception e)
			{
				System.err.println("ServerSettings: TMScreenHeight must be an integer");
				valid = false;
			}
		}

		else
		{
			System.err.println("Incorrect setting key in Server settings file:    '" + key+"'");
			valid = false;
		}
		
		return valid;
	}
	
	public String ensureSlash(String dir)
	{
		if (!dir.endsWith("\\") && !dir.endsWith("/"))
		{
			dir += "/";
		}
		
		return dir;
	}

	/*
	 * Connects the database. This should be called AFTER the settings file is parsed.
	 */
	public void connectToDB() {
		this.database = new DatabaseLayer(this.DatabaseAddress, this.DatabasePort, this.DatabaseName, this.DatabaseUser, this.DatabasePassword);
	}

}