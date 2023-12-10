package client;

import java.io.*;
import java.util.*;

import objects.*;
import utility.FileUtils;
import utility.WindowsCommandTools;

public class ClientCommands
{
	private static int startWithLoaderTimeOut 	 = 5000;
	private static int startWithLoaderMaxAttempts = 1;

	public static void Client_InitialSetup()
	{
		Client.log("     Client_InitialSetup()\n");

		// Make sure Starcraft isn't running
		Client_KillStarcraft();
		
		// Set up local firewall access
		WindowsCommandTools.RunWindowsCommand("netsh firewall add allowedprogram program = " + ClientSettings.Instance().ClientStarcraftDir + "starcraft.exe name = Starcraft mode = ENABLE scope = ALL", true, false);
		WindowsCommandTools.RunWindowsCommand("netsh firewall add allowedprogram program = client.jar name = AIIDEClient mode = ENABLE scope = ALL", true, false);
		WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening TCP 12345 \"Open Port 12345TCP\"", true, false);
		WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening UDP 12345 \"Open Port 12345UDP\"", true, false);
		
		// Remove the local replay backup if it exists and remake it
		//WindowsCommandTools.RunWindowsCommand("rmdir /S /Q " + ClientSettings.Instance().ClientStarcraftDir + "SentReplays", true, false);
		//WindowsCommandTools.RunWindowsCommand("mkdir " + ClientSettings.Instance().ClientStarcraftDir + "SentReplays", true, false);
		
		FileUtils.CleanDirectory(new File(ClientSettings.Instance().ClientStarcraftDir + "SentReplays"));
		
		// Clean the Starcraft directory of old files and folders
		Client_CleanStarcraftDirectory();
		
	}
	
	public static void Client_RunProxyScript(InstructionMessage instructions)
	{
		
		String proxyBatPath = ClientSettings.Instance().ClientStarcraftDir + "bwapi-data/AI/run_proxy.bat";
		proxyBatPath = proxyBatPath.replace("/", File.separator).replace("\\", File.separator);
		
		// if the Bat file isn't there, try to create it
		File f = new File(proxyBatPath);
		if(!f.exists() || f.isDirectory()) {
			// get the bot path
			String botFileName;
			if (instructions.isHost) {
				botFileName = instructions.hostBot.getBotPath();
			} else {
				botFileName = instructions.awayBot.getBotPath();
			}
			// convert it to basename (filename only) if there are some slashes
			if (botFileName.contains("\\")) {
				botFileName = botFileName.substring(botFileName.lastIndexOf("\\")+1, botFileName.length());
			}
			if (botFileName.contains("/")) {
				botFileName = botFileName.substring(botFileName.lastIndexOf("/")+1, botFileName.length());
			}
			// write new BAT file
			try{
				BufferedWriter out = new BufferedWriter(new FileWriter(proxyBatPath));
				// go to the StarCraft folder before running the bot to ensure we have correct CWD
				out.write("cd "+ClientSettings.Instance().ClientStarcraftDir);
				out.newLine();

				// java bots with .JAR extension
				if (botFileName.toLowerCase().contains(".jar")) {
					out.write("java -Xmx1024m -Xms512m -Djava.library.path="+ClientSettings.Instance().ClientRequirementsDir+" -jar "+ClientSettings.Instance().ClientStarcraftDir + "bwapi-data\\AI\\"+botFileName);
				} else {
				// .EXE bots and other extensions
					out.write(ClientSettings.Instance().ClientStarcraftDir + "bwapi-data\\AI\\"+botFileName);
				}
                out.newLine();
	            out.close();
        	} catch (IOException e) {}
		}
		
		// Run the proxy bot:
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) { e.printStackTrace();	}
		
		Client.log("     Running "+ proxyBatPath +"\n");
		WindowsCommandTools.RunWindowsCommand(proxyBatPath, false, false);
		
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) { e.printStackTrace();	}
	}
	
	// makes edits to the Windows registry
	public static void Client_RegisterStarCraft()
	{
		Client.log("     Client_RegisterStarCraft()\n");
		
		// 32-bit machine StarCraft settings
		String sc32KeyName =     "HKEY_LOCAL_MACHINE\\SOFTWARE\\Blizzard Entertainment\\Starcraft";
		String sc32UserKeyName = "HKEY_CURRENT_USER\\SOFTWARE\\Blizzard Entertainment\\Starcraft";
		WindowsCommandTools.RegEdit(sc32KeyName,     "InstallPath", "REG_SZ",    ClientSettings.Instance().ClientStarcraftDir + "\\");
		WindowsCommandTools.RegEdit(sc32KeyName,     "Program",     "REG_SZ",    ClientSettings.Instance().ClientStarcraftDir + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc32KeyName,     "GamePath",    "REG_SZ",    ClientSettings.Instance().ClientStarcraftDir + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc32UserKeyName, "introX",      "REG_DWORD", "00000000");
		
		// 64-bit machine StarCraft settings
		String sc64KeyName =     "HKEY_LOCAL_MACHINE\\SOFTWARE\\Wow6432Node\\Blizzard Entertainment\\Starcraft";
		String sc64UserKeyName = "HKEY_CURRENT_USER\\SOFTWARE\\Wow6432Node\\Blizzard Entertainment\\Starcraft";
		WindowsCommandTools.RegEdit(sc64KeyName, "InstallPath", "REG_SZ", ClientSettings.Instance().ClientStarcraftDir + "\\");
		WindowsCommandTools.RegEdit(sc64KeyName, "Program",     "REG_SZ", ClientSettings.Instance().ClientStarcraftDir + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc64KeyName, "GamePath",    "REG_SZ", ClientSettings.Instance().ClientStarcraftDir + "StarCraft.exe");
		WindowsCommandTools.RegEdit(sc64UserKeyName, "introX",      "REG_DWORD", "00000000");
	}	
	
	public static void Client_KillStarcraft()
	{
		Client.log("     Client_KillStarcraft()\n");
		while (WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
		{
			WindowsCommandTools.RunWindowsCommand("taskkill /T /F /IM StarCraft.exe", true, false);
			try { Thread.sleep(100); } catch (InterruptedException e) {}
		} 
	}
	
	public static void Client_KillExcessWindowsProccess(Vector<Integer> startingProc)
	{
		Client.log("     Client_KillExcessWindowsProccess()\n");
		
		// Kill any processes that weren't running before startcraft started
		// This is helpful to kill any proxy bots or java threads that may still be going
		WindowsCommandTools.KillExcessWindowsProccess(startingProc);
	}
	
	public static void Client_CleanStarcraftDirectory()
	{
		Client.log("     Client_CleanStarcraftDirectory()\n");
		
		// Sleep for a second before deleting local directories
		try 
		{ 
			Thread.sleep(2000); 
		
			// Delete local folders which now contain old data
			FileUtils.DeleteDirectory(new File(ClientSettings.Instance().ClientStarcraftDir + "bwapi-data"));
			FileUtils.DeleteDirectory(new File(ClientSettings.Instance().ClientStarcraftDir + "maps"));
			FileUtils.DeleteDirectory(new File(ClientSettings.Instance().ClientStarcraftDir + "characters"));
			FileUtils.DeleteDirectory(new File(ClientSettings.Instance().ClientStarcraftDir + "Errors"));
			
			// Delete the BWAPI.dll that was used by the previous bot
			FileUtils.DeleteFile(new File(ClientSettings.Instance().ClientStarcraftDir + "BWAPI.dll"));
			FileUtils.DeleteFile(new File(ClientSettings.Instance().ClientStarcraftDir + "version.txt"));
			
			// Delete all the other DLLs that might be left in the StarCraft folder (they tend to mess up future games)
			FileUtils.DeleteFilesWithExtension(new File(ClientSettings.Instance().ClientStarcraftDir),"dll");
			
			// Delete the old game state file
			File oldGameState = new File(ClientSettings.Instance().ClientStarcraftDir + "gameState.txt");
			while (oldGameState.exists()) 
			{
				Client.log("Old game state file exists, deleting... ");
				oldGameState.delete();
				try { Thread.sleep(100); } catch (InterruptedException e) {}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void Client_RenameCharacterFile(InstructionMessage instructions)
	{
		Client.log("     Client_RenameCharacterFile()\n");
		String botName = instructions.isHost ? instructions.hostBot.getName() : instructions.awayBot.getName();
		String charDir = ClientSettings.Instance().ClientStarcraftDir + "characters\\";
		
		WindowsCommandTools.RunWindowsCommand("RENAME " + charDir + "*.mpc \"" + botName + ".mpc\"", true, false);
		WindowsCommandTools.RunWindowsCommand("RENAME " + charDir + "*.spc \"" + botName + ".spc\"", true, false);
	}
	
	public static void Client_StartWithAppropriateLauncher()
	{
		Client.log("     Client_StartWithAppropriateLauncher()\n");
		
		// Set the correct screen resolution before starting
		WindowsCommandTools.RunWindowsExeLocal("C:\\Program Files\\VMware\\VMware Tools\\", "VMWareResolutionSet.exe 0 1 , 0 0 1280 720", true, false);
		
		// Try running SC a few times if unsuccessful.
		// We consider it successfully ran, if there is a StarCraft.exe process running.
		int tries = 0;
		while (tries < startWithLoaderMaxAttempts && !WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe")) {
			tries += 1;

			// Tries to run any alternative loaders (e.g. ChaosLauncher) before InsectLoader which is very buggy
			String loaderExe = ClientSettings.Instance().ClientStarcraftDir + "Chaoslauncher.exe";
			File loaderExeFile = new File(loaderExe);
			if (!loaderExeFile.exists() || loaderExeFile.isDirectory()) {
				loaderExe = ClientSettings.Instance().ClientStarcraftDir + "InsectLoader.exe";
			}

			Client.log("     Trying to run SC/" + loaderExe + " (attempt "+ tries +")\n");
			WindowsCommandTools.RunWindowsExeLocal(ClientSettings.Instance().ClientStarcraftDir, loaderExe, false, false);

			try {
				Thread.sleep(startWithLoaderTimeOut);
			} catch (InterruptedException e) { e.printStackTrace();	}
		}
	}

	public static void Client_WriteTournamentModuleSettings(TournamentModuleSettingsMessage tmSettings)  
	{
		Client.log("     Client_WriteTournamentModuleSettings()\n");
		String tmSettingsFile = ClientSettings.Instance().ClientStarcraftDir + "\\bwapi-data\\tm_settings.ini";
		
		String tm = tmSettings.getSettingsFileString();
		
		try 
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(tmSettingsFile)));
			out.write(tm);
			out.close();
		} 
		catch (Exception e) 
		{
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	public static void Client_WriteBWAPISettings(InstructionMessage instructions)  
	{
		String newLine = System.getProperty("line.separator");
		
		Client.log("     Client_WriteBWAPISettings()\n");
		String bwapiDest = ClientSettings.Instance().ClientStarcraftDir + "\\bwapi-data\\bwapi.ini";
		BWAPISettings bwapi = instructions.bwapi;
		Bot thisBot  = instructions.isHost ? instructions.hostBot : instructions.awayBot;
		Bot otherBot = instructions.isHost ? instructions.awayBot : instructions.hostBot;
		int id = instructions.game_id;
		
		Client.log("     * This bot: "+thisBot.getName()+" "+thisBot.getRace()+" "+thisBot.getType());
		Client.log("     * Other bot: "+otherBot.getName()+" "+otherBot.getRace()+" "+otherBot.getType());
	
		String BWINI = "";
		
		BWINI += ";BWAPI written by SSCAIT Tournament Manager " + newLine;
		
		BWINI += "[ai]" + newLine;
		BWINI += "; Paths and revisions for AI" + newLine;
		BWINI += ";   - Use commas to specify AI for multiple instances." + newLine;
		BWINI += ";   - If there are more instances than the amount of " + newLine;
		BWINI += ";         DLLs specified, then the last entry is used." + newLine;
		BWINI += ";   - Use a colon to forcefully load the revision specified." + newLine;
		BWINI += ";   - Example: SomeAI.dll:3400, SecondInstance.dll, ThirdInstance.dll" + newLine;
		
		if (thisBot.getType().equalsIgnoreCase("proxy")) {
			BWINI += "ai     = NULL" + newLine;
			BWINI += "ai_dbg = NULL" + newLine + newLine;			
		} else {
			File f = new File(thisBot.getBotPath());
			BWINI += "ai     = bwapi-data\\AI\\" + f.getName() + newLine;
			BWINI += "ai_dbg = bwapi-data\\AI\\" + f.getName() + newLine + newLine;
		}

		BWINI += "; Used only for tournaments" + newLine;
		BWINI += "; Tournaments can only be run in RELEASE mode" + newLine;
		BWINI += "tournament = " + ClientSettings.Instance().TournamentModuleFilename + newLine + newLine;

		BWINI += "[auto_menu]" + newLine;
		BWINI += "; auto_menu = OFF | SINGLE_PLAYER | LAN | BATTLE_NET" + newLine;
		BWINI += "; for replays, just set the map to the path of the replay file" + newLine;
		BWINI += "auto_menu = " + bwapi.auto_menu + newLine + newLine;

		BWINI += "; pause_dbg = ON | OFF" + newLine;
		BWINI += "; This specifies if auto_menu will pause until a debugger is attached to the process." + newLine;
		BWINI += "; Only works in DEBUG mode." + newLine;
		BWINI += "pause_dbg = " + bwapi.pause_dbg + newLine + newLine;

		BWINI += "; lan_mode = Same as the text that appears in the multiplayer connection list" + newLine;			// FINISH
		BWINI += ";            Examples: Local Area Network (UDP), Local PC, Direct IP" + newLine;
		BWINI += "lan_mode = " + bwapi.lan_mode + newLine + newLine;

		BWINI += "; auto_restart = ON | OFF" + newLine;
		BWINI += "; if ON, BWAPI will automate through the end of match screen and start the next match" + newLine;
		BWINI += "; if OFF, BWAPI will pause at the end of match screen until you manually click OK," + newLine;
		BWINI += "; and then BWAPI resume menu automation and start the next match" + newLine;
		BWINI += "auto_restart = " + bwapi.auto_restart + newLine + newLine;

		BWINI += "; map = path to map relative to Starcraft folder, i.e. map = maps\\(2)Boxer.scm" + newLine;
		BWINI += "; leaving this field blank will join a game instead of creating it" + newLine;
		BWINI += "; The filename(NOT the path) can also contain wildcards, example: maps\\(?)*.sc?" + newLine;
		BWINI += "; A ? is a wildcard for a single character and * is a wildcard for a string of characters" + newLine;
		BWINI += "map = " + bwapi.map + newLine + newLine;

		BWINI += "; game = name of the game to join" + newLine;
		BWINI += ";	i.e., game = BWAPI" + newLine;
		BWINI += ";	will join the game called \"BWAPI\"" + newLine;
		BWINI += ";	If the game does not exist and the \"map\" entry is not blank, then the game will be created instead" + newLine;
		BWINI += ";	If this entry is blank, then it will follow the rules of the \"map\" entry" + newLine;
		BWINI += "game = JOIN_FIRST" + newLine + newLine;

		BWINI += "; mapiteration =  RANDOM | SEQUENCE" + newLine;
		BWINI += "; type of iteration that will be done on a map name with a wildcard" + newLine;
		BWINI += "mapiteration = " + bwapi.mapiteration + newLine + newLine;

		BWINI += "; race = Terran | Protoss | Zerg | Random" + newLine;
		BWINI += "race = " + thisBot.getRace() + newLine + newLine;

		BWINI += "; enemy_count = 1-7, for 1v1 games, set enemy_count = 1" + newLine;
		BWINI += "; only used in single player games" + newLine;
		BWINI += "enemy_count = " + bwapi.enemy_count + newLine + newLine;

		BWINI += "; enemy_race = Terran | Protoss | Zerg | Random | RandomTP | RandomTZ | RandomPZ" + newLine;
		BWINI += "; only used in single player games" + newLine;
		BWINI += "enemy_race = " + bwapi.enemy_race + newLine + newLine;

		BWINI += "; enemy_race_# = Default" + newLine;
		BWINI += "; Values for enemy_race are acceptable, Default will use the value specified in enemy_race" + newLine;
		BWINI += "enemy_race_1 = " + bwapi.enemy_race_1 + newLine;
		BWINI += "enemy_race_2 = " + bwapi.enemy_race_2 + newLine;
		BWINI += "enemy_race_3 = " + bwapi.enemy_race_3 + newLine;
		BWINI += "enemy_race_4 = " + bwapi.enemy_race_4 + newLine;
		BWINI += "enemy_race_5 = " + bwapi.enemy_race_5 + newLine;
		BWINI += "enemy_race_6 = " + bwapi.enemy_race_6 + newLine;
		BWINI += "enemy_race_7 = " + bwapi.enemy_race_7 + newLine;

		BWINI += ";game_type = TOP_VS_BOTTOM | MELEE | FREE_FOR_ALL | ONE_ON_ONE | USE_MAP_SETTINGS | CAPTURE_THE_FLAG" + newLine;
		BWINI += ";           | GREED | SLAUGHTER | SUDDEN_DEATH | TEAM_MELEE | TEAM_FREE_FOR_ALL | TEAM_CAPTURE_THE_FLAG" + newLine;
		BWINI += "game_type = " + bwapi.game_type + newLine + newLine;

		BWINI += "; save_replay = path to save replay to" + newLine;
		BWINI += "; Accepts all environment variables including custom variables. See wiki for more info." + newLine;
		int thisBotNameLength = Math.min(4, thisBot.getName().length());
		int otherBotNameLength = Math.min(4, otherBot.getName().length());
		BWINI += "save_replay = " + "maps\\replays\\" + thisBot.getName().toUpperCase() + "\\" 
				+ String.format("%04d", id) + "-" 
				+ thisBot.getName().substring(0,thisBotNameLength)+ "_"  
				+ otherBot.getName().substring(0,otherBotNameLength) + "-" 
				+ thisBot.getRace().substring(0, 1).toUpperCase() + "v" + otherBot.getRace().substring(0, 1).toUpperCase() 
				+ ".rep" + newLine + newLine;

		BWINI += "; wait_for_min_players = #" + newLine;
		BWINI += "; # of players to wait for in a network game before starting." + newLine;
		BWINI += "; This includes the BWAPI player. The game will start immediately when it is full." + newLine;
		BWINI += "wait_for_min_players = " + bwapi.wait_for_min_players + newLine + newLine;

		BWINI += "; wait_for_max_players = #" + newLine;
		BWINI += "; Start immediately when the game has reached # players." + newLine;
		BWINI += "; This includes the BWAPI player. The game will start immediately when it is full." + newLine;
		BWINI += "wait_for_max_players = " + bwapi.wait_for_max_players + newLine + newLine;

		BWINI += "; wait_for_time = #" + newLine;
		BWINI += "; The time in milliseconds (ms) to wait after the game has met the min_players requirement." + newLine;
		BWINI += "; The game will start immediately when it is full." + newLine;
		BWINI += "wait_for_time = " + bwapi.wait_for_time + newLine + newLine;

		BWINI += "[config]" + newLine;
		BWINI += "; holiday = ON | OFF" + newLine;
		BWINI += "; This will apply special easter eggs to the game when it comes time for a holiday." + newLine;
		BWINI += "holiday = " + bwapi.holiday + newLine + newLine;

		BWINI += "; show_warnings = YES | NO" + newLine;
		BWINI += "; Setting this to NO will disable startup Message Boxes, but also disable options that" + newLine;
		BWINI += "; assist in revision choice decisions." + newLine;
		BWINI += "show_warnings = " + bwapi.show_warnings + newLine + newLine;

		BWINI += "; shared_memory = ON | OFF" + newLine;
		BWINI += "; This is specifically used to disable shared memory (BWAPI Server) in the Windows Emulator \"WINE\"" + newLine;
		BWINI += "; Setting this to OFF will disable the BWAPI Server, default is ON" + newLine;
		BWINI += "shared_memory = " + bwapi.shared_memory + newLine + newLine;

		BWINI += "[window]" + newLine;
		BWINI += "; These values are saved automatically when you move, resize, or toggle windowed mode" + newLine;

		BWINI += "; windowed = ON | OFF" + newLine;
		BWINI += "; This causes BWAPI to enter windowed mode when it is injected." + newLine;
		BWINI += "windowed = " + bwapi.windowed + newLine + newLine;

		BWINI += "; left, top" + newLine;
		BWINI += "; Determines the position of the window" + newLine;
		BWINI += "left = " + bwapi.left + newLine + newLine;
		BWINI += "top  = " + bwapi.top + newLine + newLine;

		BWINI += "; width, height" + newLine;
		BWINI += "; Determines the width and height of the client area and not the window itself" + newLine;
		BWINI += "width  = " + bwapi.width + newLine + newLine;
		BWINI += "height = " + bwapi.height + newLine + newLine;

		BWINI += "[starcraft]" + newLine;
		BWINI += "; Game sound engine = ON | OFF" + newLine;
		BWINI += "sound = " + bwapi.sound + "" + newLine;
		BWINI += "; Screenshot format = gif | pcx | tga | bmp" + newLine;
		BWINI += "screenshots = " + bwapi.screenshots + newLine + newLine;

		BWINI += "[paths]" + newLine;
		BWINI += "log_path = " + bwapi.log_path + "" + newLine;

		try 
		{
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(bwapiDest)));
			out.write(BWINI);
			out.close();
		} 
		catch (Exception e) 
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	static boolean checkIsStarcraftDirectoryCorrupted()
	{
		String requiredFile = ClientSettings.Instance().ClientStarcraftDir + "storm.dll";
		File loaderExeFile = new File(requiredFile);

		return !loaderExeFile.exists() || loaderExeFile.isDirectory();
	}
}
