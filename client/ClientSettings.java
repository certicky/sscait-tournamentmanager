package client;

import objects.BWAPISettings;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

public class ClientSettings
{	
	public String			ClientStarcraftDir;
	public String			ClientRequirementsDir;
	public String			TournamentModuleFilename;
	
	public String			ServerAddress;
	public String 			DefaultBWAPISettingsFileName;
	
	public BWAPISettings	bwapi = new BWAPISettings();

	private static final ClientSettings INSTANCE = new ClientSettings();
	
	
	private ClientSettings()
	{
		
	}
	
	public static ClientSettings Instance() 
	{
        return INSTANCE;
    }
	
	public void parseSettingsFile(String filename)
	{
		try
		{
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			
			while ((line = br.readLine()) != null)
			{
				line = line.trim();
				
				if (line.startsWith("#") || line.length() == 0)
				{
					continue;
				}
				
				parseLine(line);
			}
			
			br.close();
		}
		catch (Exception e)
		{
			System.err.println("Error parsing settings file, exiting\n");
			e.printStackTrace();
			System.exit(-1);
		}
		
		bwapi.loadFromFile(DefaultBWAPISettingsFileName);
	}
	
	private void parseLine(String line) throws Exception
	{
		StringTokenizer st = new StringTokenizer(line, " +");	
		String type = st.nextToken();
		
		if (type.equalsIgnoreCase("ClientStarcraftDir"))
		{
			ClientStarcraftDir = ensureSlash(st.nextToken());
		}
		else if (type.equalsIgnoreCase("ServerAddress"))
		{
			ServerAddress = st.nextToken();
		}
		else if (type.equalsIgnoreCase("TournamentModule"))
		{
			TournamentModuleFilename = st.nextToken();
		}
		else if (type.equalsIgnoreCase("DefaultBWAPISettings"))
		{
			DefaultBWAPISettingsFileName = st.nextToken();
		}
		else if (type.equalsIgnoreCase("ClientRequirementsDir"))
		{
			ClientRequirementsDir = st.nextToken();
		}
		else
		{
			System.err.println("Ignoring incorrect setting type in settings file:    " + type);
		}
	}
	
	public String ensureSlash(String dir)
	{
		if (!dir.endsWith("\\") && !dir.endsWith("/"))
		{
			dir += "\\";
		}
		
		return dir;
	}
}