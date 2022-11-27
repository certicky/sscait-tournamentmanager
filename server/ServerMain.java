package server;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.ResultSet;
import java.sql.Statement;

import objects.Bot;

public class ServerMain 
{

	public static String serverSettingsFile;
	
	public static void main(String[] args) throws Exception
	{
		
		// load and parse the settings file
		if (args.length == 1)
		{
			ServerSettings.Instance().parseSettingsFile(args[0]);
			ServerSettings.Instance().connectToDB();
		}
		else
		{
			System.err.println("\n\nPlease provide server settings file as command line argument.\n");
			System.exit(-1);
		}
		
		// redirect errors to the log file
		System.setErr(new PrintStream(new FileOutputStream(ServerSettings.Instance().ServerLogFilePath, true), true));
		
		// start the server
		Server manager = new Server();
		manager.start();
		
		while (true)
		{
			Thread.sleep(1000);
		}
	}

}
