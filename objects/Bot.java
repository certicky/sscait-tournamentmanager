package objects;

import java.io.Serializable;

import server.ServerSettings;

public class Bot implements Serializable
{
	private static final long serialVersionUID = 2690734629985126222L;
	private String name;
	private String email;
	private String race;
	private String type;
	private String botPath;
	private int id;

	public Bot(int botId, String name, String race, String type, String botPath, String email)
	{
		this.id = botId;
		this.name = name;
		this.email = email;
		this.race = race;
		this.type = type;
		this.botPath = botPath;
		if (this.type.equalsIgnoreCase("AI_MODULE")) this.type = "dll";
		if (this.type.equalsIgnoreCase("JAVA_JNI")) this.type = "proxy";
		if (this.type.equalsIgnoreCase("JAVA_MIRROR")) this.type = "proxy";
		if (this.type.equalsIgnoreCase("EXE")) this.type = "proxy";
	}
	
	
	public String getEmail() 
	{
		return email;
	}
	
	public String getName() 
	{
		return name;
	}

	public String getRace() 
	{
		return race;
	}
	
	public String getType()
	{
		return type;
	}
	
	public String getServerDir()
	{
		return ServerSettings.Instance().ServerBotDir + String.valueOf(this.id) + "/";
	}
	
	public boolean isProxyBot()
	{
		return type.equalsIgnoreCase("proxy");
	}

	public String getId() {
		return String.valueOf(this.id);
	}
	
	public String getBotPath() 
	{
		return botPath;
	}
}
