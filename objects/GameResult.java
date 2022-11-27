package objects;

import java.util.Vector;

import server.ServerSettings;

public class GameResult implements Comparable<Object>
{
	public int gameID				= -1;
	public int roundID				= -1;
	
	public String hostName			= "Error";
	public String awayName			= "Error";
	public String mapName			= "Error";
	
	public String firstReport		= "Error";
	
	public String winName			= "Error";
	public String crashName			= "n/a";
	public String timeOutName		= "n/a";
	
	public boolean hostWon			= false;
	public boolean hostCrash		= false;
	public boolean awayCrash		= false;
	
	public Vector<Integer> hostTimers   = new Vector<Integer>();
	public Vector<Integer> awayTimers   = new Vector<Integer>();

	public boolean hourTimeout		= false;
	
	public int hostScore			= 0;
	public int awayScore			= 0;
	public int finalFrame			= -2;
	public int hostTime				= 0;
	public int awayTime				= 0;

	public GameResult() {}

	public GameResult (String data) 
	{
		setResult(data);

		int numTimers = ServerSettings.Instance().tmSettings.TimeoutLimits.size();
	
		for (int i=0; i<numTimers; ++i)
		{
			hostTimers.add(0);
			awayTimers.add(0);
		}
	}
	
	public void setResult (String dataLine)
	{
		String[] data = dataLine.split(" +");
		
		gameID 				= Integer.parseInt(data[1]);
		roundID 			= Integer.parseInt(data[2]);
		
		hostName			= data[3];
		awayName			= data[4];
		mapName				= data[5];
		
		if (data[6].equals("true"))
		{
			hostWon = true;
		}
		
		if (data[7].equals("true"))
		{
			hostCrash = true;
		}
		
		if (data[8].equals("true"))
		{
			awayCrash = true;
		}
		
		hourTimeout			= Boolean.getBoolean(data[9]);
		
		//hostScore			= Integer.parseInt(data[10]) != 0 ? Integer.parseInt(data[10]) : hostScore;
		//awayScore			= Integer.parseInt(data[11]) != 0 ? Integer.parseInt(data[11]) : awayScore;
		
		finalFrame			= Integer.parseInt(data[12]) > finalFrame ? Integer.parseInt(data[12]) : finalFrame;
		
		hostTime			= Integer.parseInt(data[13]) != 0 ? Integer.parseInt(data[13]) : hostTime;
		awayTime			= Integer.parseInt(data[14]) != 0 ? Integer.parseInt(data[14]) : awayTime;
		
		int numTimers = ServerSettings.Instance().tmSettings.TimeoutLimits.size();
		for (int i=0; i<numTimers; ++i)
		{
			hostTimers.add(Integer.parseInt(data[15 + i]));
			awayTimers.add(Integer.parseInt(data[15 + numTimers + i]));
		}
		
		winName 			= hostWon ? hostName : awayName;
		
		if (finalFrame > 0 && hostCrash && !awayCrash)
		{
			crashName = hostName;
		}
		else if (finalFrame > 0 && awayCrash && !hostCrash)
		{
			crashName = awayName;
		}
			
		int tempFinalFrame 	= Integer.parseInt(data[12]);
		if (firstReport.equals("Error"))
		{
			if (hostTime == 0)
			{
				firstReport = awayName;
				if (tempFinalFrame == -1)
				{
					awayCrash = true;
					hostCrash = false;
					crashName = awayName;
					winName = hostName;
				}
			}
			else
			{
				firstReport = hostName;
				if (tempFinalFrame == -1)
				{
					hostCrash = true;
					awayCrash = false;
					crashName = hostName;
					winName = awayName;
				}
			}
		}
		else if (tempFinalFrame == -1)
		{
			if (firstReport.equals(hostName))
			{
				hostCrash = false;
				awayCrash = true;
				crashName = awayName;
				winName = hostName;
			}	
			else
			{
				awayCrash = false;
				hostCrash = true;
				crashName = hostName;
				winName = awayName;
			}
		}
		
		for (int i=0; i<numTimers; ++i)
		{
			if (hostTimers.get(i) > ServerSettings.Instance().tmSettings.TimeoutBounds.get(i))
			{
				timeOutName = hostName;
				break;
			}
			
			if (awayTimers.get(i) > ServerSettings.Instance().tmSettings.TimeoutBounds.get(i))
			{
				timeOutName = awayName;
				break;
			}
		}
		
		if (finalFrame >= ServerSettings.Instance().tmSettings.GameFrameLimit)
		{
			hourTimeout = true;
		}
	}
	
	public String toString()
	{
		String s = String.format("%7d %5d %15s %15s %15s %8d %15s %15s %25s %6b %6b%6b %8d %8d %10d %10d\n",
				this.gameID, this.roundID, this.hostName, 
				this.awayName, this.winName, this.finalFrame, this.crashName, this.timeOutName, this.mapName, 
				this.hostCrash, this.awayCrash, 
				this.hourTimeout, this.hostScore, this.awayScore, 
				this.hostTime, this.awayTime);
				
		for (int i=0; i<hostTimers.size(); ++i)
		{
			String t = String.format(" %5d", hostTimers.get(i));
			s += t;
		}
		
		for (int i=0; i<awayTimers.size(); ++i)
		{
			String t = String.format(" %5d", awayTimers.get(i));
			s += t;
		}
		
		return s;
	}
	
	public int compareTo(Object other)
	{
		return this.gameID - ((GameResult)other).gameID;
	}

}
