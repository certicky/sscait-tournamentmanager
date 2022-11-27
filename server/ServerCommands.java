package server;

import java.sql.SQLException;
import objects.*;
import utility.FileUtils;
import utility.WindowsCommandTools;

public class ServerCommands
{
	public static void Server_InitialSetup()
	{
		if (System.getProperty("os.name").contains("Windows"))
		{
			WindowsCommandTools.RunWindowsCommand("netsh firewall add allowedprogram program = " + ServerSettings.Instance().ServerDir + "server.jar name = AIIDEServer mode = ENABLE scope = ALL", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening TCP 12345 \"Open Port 12345TCP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening UDP 12345 \"Open Port 12345UDP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening TCP 1337 \"Open Port 1337TCP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening UDP 1337 \"Open Port 1337UDP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening TCP 11337 \"Open Port 11337TCP\"", true, false);
			WindowsCommandTools.RunWindowsCommand("netsh firewall add portopening UDP 11337 \"Open Port 11337UDP\"", true, false);
		}
	}
	
	public static void Server_MoveWriteToRead() throws SQLException 
	{
		System.out.println("Copying write to read directories for enabled bots.");
    	for (Bot bot : ServerSettings.Instance().database.getAllEnabledBots())
		{
    		System.out.println("    " + ServerSettings.Instance().ServerBotDir + bot.getId() + "/write/" + " to " + ServerSettings.Instance().ServerBotDir + bot.getId() + "/read/");
			FileUtils.CopyDirectory(ServerSettings.Instance().ServerBotDir + bot.getId() + "/write/", ServerSettings.Instance().ServerBotDir + bot.getId() + "/read/");
    	}
	}

	public static void Server_MoveWriteToRead(Bot bot1, Bot bot2) 
	{
		System.out.println("Copy directory " +ServerSettings.Instance().ServerBotDir + bot1.getId() + "/write/"+ " to " +ServerSettings.Instance().ServerBotDir + bot1.getId() + "/read/");
		FileUtils.CopyDirectory(ServerSettings.Instance().ServerBotDir + bot1.getId() + "/write/", ServerSettings.Instance().ServerBotDir + bot1.getId() + "/read/");
		System.out.println("Copy directory " +ServerSettings.Instance().ServerBotDir + bot2.getId() + "/write/"+ " to " +ServerSettings.Instance().ServerBotDir + bot2.getId() + "/read/");
		FileUtils.CopyDirectory(ServerSettings.Instance().ServerBotDir + bot2.getId() + "/write/", ServerSettings.Instance().ServerBotDir + bot2.getId() + "/read/");
	}

}