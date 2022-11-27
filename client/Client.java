package client;

import java.util.*;
import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.text.*;

import objects.*;
import utility.*;

public class Client extends Thread
{
	protected ClientStatus status;

	public ClientSettings settings;

	private long startTime;
	private long endTime;

	public InstructionMessage previousInstructions;

	private Vector<Integer> startingproc;

	private ClientListenerThread listener;

	private long 		lastGameStateUpdate 	= 0;
	private int 		lastGameStateFrame 		= 0;
	private int 		monitorLoopTimer 		= 1000;
	private int 		gameStartingTimeout 	= 70000;
	private int 		gameStartingBroodwarTimeout = 5000;
	private int 		gameStartAttempts 	    = 0;
	private int 		gameStartMaxAttempts 	= 10;
	private int 		gameStartRunBroodwarAttempts 	    = 0;
	private int 		gameStartRunBroodwarMaxAttempts 	= 30;
	private int			timeOutThreshold		= 78000; // increased to 78 = 60 (drop screen time) + 15 (since last update) + 3 (just in case)
	private int 		gameStateReadAttempts 	= 0;
	private boolean 	haveGameStateFile		= false;
	private boolean 	starcraftIsRunning		= false;

	public boolean 	shutDown = false;

	private DataMessage	requiredFiles			= null;
	private DataMessage	botFiles				= null;

	private TournamentModuleSettingsMessage	tmSettings = null;

	private long		lastMovedMouseAt		= 0;

	public Client()
	{
		settings 				= ClientSettings.Instance();
		startTime 				= 0;
		endTime 				= 0;
		startingproc 			= WindowsCommandTools.GetRunningProcesses();
		status 					= ClientStatus.READY;
		previousInstructions 	= null;

		ClientCommands.Client_InitialSetup();

		// update the list of "tolerated" running processes, so that it includes Chromium GUI
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) { e.printStackTrace();	}
		startingproc = WindowsCommandTools.GetRunningProcesses();

	}


	public static String getTimeStamp()
	{
		return new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
	}

	public void setListener(ClientListenerThread l)
	{
		listener = l;
		listener.start();
	}

	public static void log(String s) {
		String msg = getTimeStamp() + " " + s.replaceAll("\n","").replaceAll("\\s+$","");

		// print the message into the console
		System.out.println(msg);

		// send the message to the server via HTTP
		try {
			URL url = new URL("http://sscaitournament.com/client_console_listener.php?address="+InetAddress.getLocalHost()+"&message="+URLEncoder.encode(msg, "UTF-8"));
			url.openStream().close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public synchronized void setStatus(ClientStatus s, Game g)
	{
		log("\n\nNEW STATUS: " + s + "\n\n");
		this.status = s;
		log("Status: " + status + "\n");
		listener.sendMessageToServer(new ClientStatusMessage(this.status, g));
	}

	public synchronized void sendRunningUpdate(TournamentModuleState gameState)
	{
		listener.sendMessageToServer(new ClientStatusMessage(this.status, null, gameState, previousInstructions.isHost, 0));
	}

	public synchronized void sendStartingUpdate(int startingDuration)
	{
		listener.sendMessageToServer(new ClientStatusMessage(this.status, null, null, previousInstructions.isHost, startingDuration));
	}

	public synchronized void setStatus(ClientStatus s)
	{
		setStatus(s, null);
	}

	// move the mouse cursor to the bottom-right corner (but not more often than once every 10 seconds)
	public void moveMouseCursor() {
		if (!this.starcraftIsRunning) return;
		long currentUnixTime = System.currentTimeMillis();
		if (currentUnixTime - 10000 > this.lastMovedMouseAt) {
			try {
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			    Robot robot = new Robot();
			    robot.mouseMove(10, Double.valueOf(screenSize.getHeight()-10).intValue());

			    this.lastMovedMouseAt = currentUnixTime;

			} catch (AWTException e) {
				e.printStackTrace();
			}
		}
	}

	public void run()
	{
		while (true)
		{
			TournamentModuleState gameState = new TournamentModuleState();

			// run this loop every so often
			try
			{
				Thread.sleep(monitorLoopTimer);
			}	catch (Exception e) {}

			// don't do anything if we haven't connected to the server yet
			if (!listener.connected)
			{
				continue;
			}

			if (shutDown) {
				continue;
			}

			// try to read in the game state file
			haveGameStateFile = gameState.readData(settings.ClientStarcraftDir + "gameState.txt");
			starcraftIsRunning = WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe");

			// move the mouse cursor to the corner
			moveMouseCursor();

			//System.out.println("Client Main Loop: " + status + " " + haveGameStateFile + " " + starcraftIsRunning);

			if (status == ClientStatus.READY)
			{
				if (haveGameStateFile)
				{
					log("MainLoop: Error, have state file while in main loop\n");
				}
			}
			// if the game is starting, check for game state file
			else if (status == ClientStatus.STARTING)
			{
				// if we we don't have a game state file yet
				if (!haveGameStateFile) {
					//System.out.println("MONITOR: No Game State File Yet " );
					gameStateReadAttempts++;
					sendStartingUpdate((gameStateReadAttempts * monitorLoopTimer)/1000);

					// if the game didn't start within our threshold time
                    boolean isAGameStartTimeout = gameStateReadAttempts > (gameStartingTimeout / monitorLoopTimer);
                    if (isAGameStartTimeout
                        || (gameStateReadAttempts > (gameStartingBroodwarTimeout / monitorLoopTimer)
                            && !WindowsCommandTools.IsWindowsProcessRunning("StarCraft.exe"))
                    ) {
						log("MainLoop: Game didn't start for " + gameStartingTimeout + "ms\n");

						// try to run it again for a few times
						if (gameStartAttempts < gameStartMaxAttempts
                            && gameStartRunBroodwarAttempts < gameStartRunBroodwarMaxAttempts
                        ) {
							log("Trying to run SC again...");
							this.status = ClientStatus.READY;
							if (canStartStarCraft()) startStarCraft(this.previousInstructions, true);

                            if (isAGameStartTimeout) {
                                gameStartAttempts++;
                            } else {
                                gameStartRunBroodwarAttempts++;
                            }
						} else {
							// and restart the machine if it fails to start repeatedly
							log("Couldn't start the game for too many times...");
							WindowsCommandTools.RestartMachine();
						}
					}
				} else {
					// the game is now running
					setStatus(ClientStatus.RUNNING);
					gameStartAttempts = 0;
                    gameStartRunBroodwarAttempts = 0;
				}
			}
			// if the game is currently running
			else if (status == ClientStatus.RUNNING)
			{
				sendRunningUpdate(gameState);

				// update the last frame we read from the gameState file
				if(lastGameStateUpdate == 0 || gameState.frameCount > lastGameStateFrame)
				{
					lastGameStateUpdate = System.currentTimeMillis();
					lastGameStateFrame = gameState.frameCount;
				}

				// if the game ended gracefully
				if (gameState.gameEnded == 1)
				{
					log("MainLoop: Game ended normally, prepping reply\n");
					setEndTime();
					prepReply(gameState);
				}
				// check for a crash
				else
				{
					boolean crash = ((System.currentTimeMillis() - lastGameStateUpdate) > timeOutThreshold)	|| !starcraftIsRunning;

					if (crash)
					{
						log("MainLoop: We crashed, prepping crash\n");
						log("	starcraftIsRunning = "+starcraftIsRunning+"\n");
						log("	system.currentTime = "+System.currentTimeMillis()+"\n");
						log("	lastGameUpdate     = "+lastGameStateUpdate+"\n");
						log("	currentTime-lastUp = "+(System.currentTimeMillis()-lastGameStateUpdate)+"\n");
						log("	timeOutThreshold   = "+timeOutThreshold+"\n");
						log("MONITOR: Crash detected, shutting down game");
						setEndTime();
						prepCrash(gameState);
					}
				}
			}
		}
	}

	public synchronized void receiveMessage(Message m)
	{
		if (m instanceof InstructionMessage)
		{
			receiveInstructions((InstructionMessage)m);
		}
		else if (m instanceof DataMessage)
		{
			DataMessage dm = (DataMessage)m;

			if (dm.type == DataType.REQUIRED_DIR)
			{
				requiredFiles = dm;
				log("Client: Required files received\n");
			}
			else if (dm.type == DataType.BOT_DIR)
			{
				botFiles = dm;
				log("Client: Bot files received\n");
			}
		}
		else if (m instanceof StartGameMessage)
		{
			if (canStartStarCraft())
			{
				startStarCraft(previousInstructions, false);
			}
			else
			{
				log("Error: StartGameMessage while StarCraft not ready to start\n");
			}
		}
		else if (m instanceof TournamentModuleSettingsMessage)
		{
			tmSettings = (TournamentModuleSettingsMessage)m;
		}
		else if (m instanceof ServerShutdownMessage)
		{
			shutDown();
		}
		else if (m instanceof AbortGameMessage)
		{
            if (status == ClientStatus.STARTING) {
                log("Asked to abort game. Cleaning up.\n");

                abortGame();

                setStatus(ClientStatus.READY);
            }
		}
	}

	public boolean canStartStarCraft()
	{
		return (previousInstructions != null) && (requiredFiles != null) && (botFiles != null);
	}

	public void receiveInstructions(InstructionMessage instructions)
	{
		log("Recieved Instructions");
		log("Game id -> " + instructions.game_id);
		log(instructions.hostBot.getName() + " vs. " + instructions.awayBot.getName());

		previousInstructions = instructions;
	}

	private boolean isProxyBot(InstructionMessage instructions)
	{
		if (instructions == null)
		{
			return false;
		}

		if (instructions.isHost)
		{
			return instructions.hostBot.isProxyBot();
		}
		else
		{
			return instructions.awayBot.isProxyBot();
		}
	}

	private void startStarCraft(InstructionMessage instructions, boolean onlyRestarting)
	{
		try 
		{
			if (status == ClientStatus.READY)
			{
				setStatus(ClientStatus.STARTING);
				gameStateReadAttempts = 0;
				lastGameStateUpdate = 0;
	
	
				// Prepare the machine for Starcraft launching
				ClientCommands.Client_KillStarcraft();
				ClientCommands.Client_KillExcessWindowsProccess(startingproc);
				if (onlyRestarting == false) {
					ClientCommands.Client_CleanStarcraftDirectory();
					Thread.sleep(3000); 
				}
	
				log("	StartStarcraft: Storing Current Processes\n");
				startingproc = WindowsCommandTools.GetRunningProcesses();
	
				log("	StartStarcraft: Launching StarCraft\n");
	
				if (onlyRestarting == false) {
					// Write the required starcraft files to the client machine
					requiredFiles.write(ClientSettings.Instance().ClientStarcraftDir);
	
					// Write the bot files to the client machine
					botFiles.write(ClientSettings.Instance().ClientStarcraftDir + "bwapi-data");
	
					// Rename the character files to match the bot names
					ClientCommands.Client_RenameCharacterFile(instructions);
	
					// Write out the BWAPI and TournamentModule settings files
					ClientCommands.Client_WriteBWAPISettings(instructions);
					ClientCommands.Client_WriteTournamentModuleSettings(tmSettings);
	
					if (ClientCommands.checkIsStarcraftDirectoryCorrupted()){
						log("Starcraft directory corrupted. Restarting VM...");
						WindowsCommandTools.RestartMachine();
	
						shutDown = true;
						return;
					}
				}
	
				// If this is a proxy bot, start the proxy bot script before StarCraft starts
				if (isProxyBot(previousInstructions))
				{
					ClientCommands.Client_RunProxyScript(previousInstructions);
				}
	
				// Start Starcraft using the InsectLoader.exe
				ClientCommands.Client_StartWithAppropriateLauncher();
	
	
				// Record the time that we tried to start the game
				startTime = System.currentTimeMillis();
	
				// Reset the files for next game
				requiredFiles = null;
				botFiles = null;
			}
			else
			{
				log("Tried to start StarCraft when not ready");
			}
		} 
		catch (Exception e) 
		{
			log("Exception in startStartCraft(): " + e.getMessage());
		}
	}

	public String getServer()
	{
		return settings.ServerAddress;
	}

	void prepReply(TournamentModuleState gameState)
	{
		Game retGame = new Game(	previousInstructions.game_id,
									previousInstructions.round_id,
									previousInstructions.hostBot,
									previousInstructions.awayBot,
									null
									);

		retGame.setWasDraw(gameState.gameHourUp > 0);
		retGame.setFinalFrame(gameState.frameCount);

		if (previousInstructions.isHost)
		{
			retGame.setHostTime(getElapsedTime());
			retGame.setHostTimers(gameState.timeOutExceeded);
			retGame.setTimeout(gameState.gameHourUp == 1);
			retGame.setHostwon(gameState.selfWin == 1);
			retGame.setHostScore(gameState.selfScore);
		}
		else
		{
			retGame.setGuestTime(getElapsedTime());
			retGame.setAwayTimers(gameState.timeOutExceeded);
			retGame.setTimeout(gameState.gameHourUp == 1);
			retGame.setHostwon(gameState.selfWin == 0);
			retGame.setAwayScore(gameState.selfScore);
		}

		log("Game ended normally. Sending results and cleaning the machine\n");
		setStatus(ClientStatus.SENDING, retGame);
		gameOver();
		setStatus(ClientStatus.READY);
	}

	void prepCrash(TournamentModuleState gameState)
	{
		Game retGame = new Game(	previousInstructions.game_id,
									previousInstructions.round_id,
									previousInstructions.hostBot,
									previousInstructions.awayBot,
									null);

		retGame.setFinalFrame(gameState.frameCount);
		if (previousInstructions.isHost)
		{
			retGame.setHostcrash(true);
			retGame.setHostTime(getElapsedTime());
			retGame.setHostTimers(gameState.timeOutExceeded);
			retGame.setHostScore(gameState.selfScore);
		}
		else
		{
			retGame.setAwaycrash(true);
			retGame.setGuestTime(getElapsedTime());
			retGame.setAwayTimers(gameState.timeOutExceeded);
			retGame.setAwayScore(gameState.selfScore);
		}

		log("Game ended in crash. Sending results and cleaning the machine\n");
		ClientCommands.Client_KillStarcraft();
		ClientCommands.Client_KillExcessWindowsProccess(startingproc);
		setStatus(ClientStatus.SENDING, retGame);
		sendFilesToServer(false);
		ClientCommands.Client_CleanStarcraftDirectory();
		setStatus(ClientStatus.READY);

	}

	private void gameOver()
	{
		sendFilesToServer(true);
		ClientCommands.Client_KillStarcraft();
		ClientCommands.Client_KillExcessWindowsProccess(startingproc);
		ClientCommands.Client_CleanStarcraftDirectory();
	}

	public void shutDown()
	{
		abortGame();
		System.exit(0);
	}

	public void abortGame()
	{
	    ClientCommands.Client_KillStarcraft();
        ClientCommands.Client_KillExcessWindowsProccess(startingproc);
        ClientCommands.Client_KillStarcraft();
        ClientCommands.Client_CleanStarcraftDirectory();
	}

	private void sendFilesToServer(boolean retryWait)
	{
		// sleep 5 seconds to make sure starcraft wrote the replay file correctly
		int attempt=0;
 		java.io.File dir;
		boolean fileExists=false;
 		do
 		{
 			try { Thread.sleep(5000); } catch (Exception e) {}
 			try {
	 			dir = new java.io.File(ClientSettings.Instance().ClientStarcraftDir + "maps\\replays");
				if (dir.list().length>0)
				{
					java.io.File subDir=dir.listFiles()[0];
					if(subDir.list().length>0)
					{
						fileExists=true;
					}
				}
	 			attempt++;
 			} catch (Exception e) {
 				e.printStackTrace();
 				attempt++;
 			}
		}while(attempt<10 && !fileExists && retryWait);


		// copy all the error logs to replays folder, so they can be zipped and sent along with the rep
		String srcDir1 = ClientSettings.Instance().ClientStarcraftDir + "Errors";
		String srcDir2 = ClientSettings.Instance().ClientStarcraftDir + "bwapi-data\\logs";
		String trgDir = ClientSettings.Instance().ClientStarcraftDir + "maps\\replays\\"+previousBotName().toUpperCase()+"\\Logs";
		FileUtils.CopyDirectory(srcDir1, trgDir);
		FileUtils.CopyDirectory(srcDir2, trgDir);
		Process p;
		try {
			//log("Moving "+System.getProperty("user.dir")+"\\hs_err_*"+" to "+"\""+ClientSettings.Instance().ClientStarcraftDir+"maps\\replays\\"+previousBotName().toUpperCase()+"\"");
			p = Runtime.getRuntime().exec(new String[] {"cmd.exe", "/c", "move", System.getProperty("user.dir")+"\\hs_err_*","\""+ClientSettings.Instance().ClientStarcraftDir+"maps\\replays\\"+previousBotName().toUpperCase()+"\""});
			p.waitFor();
		} catch (Exception e) {e.printStackTrace();}

		// send the replay data to the server
		DataMessage replayMessage = new DataMessage(DataType.REPLAY, ClientSettings.Instance().ClientStarcraftDir + "maps\\replays");
		log("Sending Data to Sever: " + replayMessage.toString() + "\n");
		listener.sendMessageToServer(replayMessage);

		// send the write folder back to the server
		if (previousInstructions != null)
		{
			DataMessage writeDirMessage = new DataMessage(DataType.WRITE_DIR, previousBotName(), ClientSettings.Instance().ClientStarcraftDir + "bwapi-data\\write");
			log("Sending Data to Sever: " + writeDirMessage.toString() + "\n");
			listener.sendMessageToServer(writeDirMessage);
		}
	}

	public String previousBotName()
	{
		return previousInstructions.isHost ? previousInstructions.hostBot.getName() : previousInstructions.awayBot.getName();
	}

	public void setFinalFrame(String substring)
	{
		Integer.parseInt(substring.trim());
	}

	public void setEndTime()
	{
		this.endTime = System.currentTimeMillis();
		log("Game lasted " + (this.endTime - this.startTime) + " ms");
	}

	public long getElapsedTime()
	{
		return (this.endTime - this.startTime);
	}
}
