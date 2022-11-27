package objects;
import java.util.Vector;

public class TournamentModuleSettingsMessage implements Message 
{
	private static final long serialVersionUID = 9062722131163042773L;
	
	// default local speed & frameskip 
	public int 				LocalSpeed				= 0;
	public int 				FrameSkip				= 0;
	
	// total game time limit in frames
	public int					GameFrameLimit			= 85714;
	
	// time limit for period with no units killed (in real-world seconds) 
	public int 				NoKillsTimeoutRealSecs	= 300;

	// camera movement speeds
	public int 				CameraMoveTime			= 200;
	public int 				CameraMoveTimeMin		= 50;
	
	// screen size & debug messages
	public int					ScreenWidth 			= 640;
	public int 				ScreenHeight 			= 480;

	// bot LAG prevention limits
	public Vector<Integer>		TimeoutLimits			= new Vector<Integer>();
	public Vector<Integer>		TimeoutBounds			= new Vector<Integer>();

	// speedups: speed = 0 (for better viewing experience)
	public int 				InitMaxSpeedTime 		= 1440;		// speed=0 for first N frames of the game		
	public int 				ZeroSpeedTime			= 43200;	// game speeds up on frame N
	public int 				NoCombatSpeedUpTime 	= 9600; 	// previous setting activates after N frames
	public int					NoCombatSpeedUpDelay 	= 480;		// if no combat happens for N frames, game speeds up
	
	public TournamentModuleSettingsMessage()
	{
		
	}
	
	public String toString()
	{
		return "(" + LocalSpeed + "," + FrameSkip + "," + GameFrameLimit + ")";
	}
	
	public String getSettingsFileString()
	{
		String newLine = System.getProperty("line.separator");
		
		String tm = "";
		
		tm += "LocalSpeed "         + LocalSpeed     + newLine;
		tm += "FrameSkip "          + FrameSkip      + newLine;
		tm += "GameFrameLimit "     + GameFrameLimit + newLine;
		tm += "NoKillsRealSecondsLimit "     + NoKillsTimeoutRealSecs + newLine;
		tm += "ZeroSpeedTime " 		+ ZeroSpeedTime  + newLine;
		tm += "CameraMoveTime "     + CameraMoveTime + newLine;
		tm += "CameraMoveTimeMin "     + CameraMoveTimeMin + newLine; 
		tm += "InitMaxSpeedTime "     + InitMaxSpeedTime + newLine; 
		tm += "NoCombatSpeedUpTime "     + NoCombatSpeedUpTime + newLine; 
		tm += "NoCombatSpeedUpDelay "     + NoCombatSpeedUpDelay + newLine; 
		tm += "ScreenWidth "     + ScreenWidth + newLine;
		tm += "ScreenHeight "     + ScreenHeight + newLine;
		
		for (int i = 0; i < TimeoutLimits.size(); ++i)
		{
			tm += "Timeout " + TimeoutLimits.get(i) + " " + TimeoutBounds.get(i) + newLine;	
		}
		
		return tm;
	}
}
