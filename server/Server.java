
package server;

import java.io.*;
import java.util.*;
import java.sql.SQLException;
import java.text.*;

import utility.*;
import objects.*;

import java.nio.channels.FileChannel;

import com.sun.xml.internal.ws.api.config.management.policy.ManagementAssertion.Setting;
import com.sun.xml.internal.ws.util.StreamUtils;

public class Server  extends Thread
{
    private Vector<ServerClientThread> 		clients;
    private Vector<ServerClientThread> 		free;

    private ServerListenerThread			listener;
	private int								gameRescheduleTimer = 2000;
	private int 							lastVMRestartId = -1;

	public ServerGUI 						gui;
	private Game							previousScheduledGame = null;

	private long 							atLeastTwoClientsConnectedLastTime = System.currentTimeMillis();
	private long 							atLeastTwoClientsConnectedTimeoutSeconds = 180;

    Server() throws SQLException
	{
    	gui = new ServerGUI(this);
		//boolean resumed = gui.handleTournamentResume();
		gui.handleFileDialogues();

        clients 	= new Vector<ServerClientThread>();
        free 		= new Vector<ServerClientThread>();

		setupServer();

		listener = new ServerListenerThread(this);
		listener.start();
    }

	public void run()
	{

		// get next game on schedule
		Game nextGame = null;
		try {
			nextGame = ServerSettings.Instance().database.getNextGame();
		} catch (SQLException e1) {
			System.err.println(e1.getMessage() +"\n"+ e1);
		}

		if (nextGame == null)
		{
			System.err.println("Server: There were 0 games on schedule. We tried to add some. Run the server again please.");
			System.exit(-1);
		}

		int neededClients = 2;

		// keep trying to schedule games
		while (true)
		{
			try
			{
				// schedule a game once every few seconds
				Thread.sleep(gameRescheduleTimer);

				nextGame = ServerSettings.Instance().database.getNextGame();
				if (nextGame == null)
				{
					log("No more games scheduled. Action required!");
				}

				// checks if fewer than 2 clients connected during the last X minutes
				if (clients.size() < 2) {
					if (System.currentTimeMillis() - atLeastTwoClientsConnectedLastTime >= atLeastTwoClientsConnectedTimeoutSeconds * 1000) {
						lastVMRestartId = nextGame.getGameID();
						String restartMessage = "Restarting due to not enough clients for X minutes (gameId=" + lastVMRestartId + ").";
						log(restartMessage);
						WatchdogUtils.restartManagerAndVMs(lastVMRestartId, true, restartMessage);
					}
				} else {
					atLeastTwoClientsConnectedLastTime = System.currentTimeMillis();
				}

				String gameString = "Game(id:" + nextGame.getGameID() + ", " + nextGame.getHomebot().getName() + " vs. "+nextGame.getAwaybot().getName()+")";

				// we can't start a game if we don't have enough clients
				if (free.size() < neededClients)
				{
					//log(gameString + " Can't start: Not Enough Clients\n");
					continue;
				}
				// also don't start a game if a game is currently in the lobby
				else if (isAnyGameStarting())
				{
					//log(gameString + " Can't start: Another Game Starting\n");
					continue;
				}

				// if this game is a higher round than the last game
				if (previousScheduledGame != null
                        && (nextGame.getRound() > previousScheduledGame.getRound()
                            || clients.size() <= 2) // or after each game in the case of just two clients
                ) {
					// put some polling code here to wait until all games from this round are free
					while (free.size() < clients.size())
					{
						log(gameString + " Can't start: Waiting for Previous Round to Finish\n");
						Thread.sleep(gameRescheduleTimer);
					}
					
					// disabled, because this copied folders for all the active bots on the server and I don't know why 
					//log("Moving Write Directory to Read Directory");
					// move the write dir to the read dir
					//ServerCommands.Server_MoveWriteToRead();
				}

				log(gameString + " SUCCESS: Starting Game\n");
				start1v1Game(nextGame);

				//if (games.hasMoreGames())
				//{
				//	nextGame = games.getNextGame();
				//}
				nextGame = ServerSettings.Instance().database.getNextGame();
			}
			catch (Exception e)
			{
				EmailUtil.sendExceptionViaGmail(e);
				e.printStackTrace();
				log(e.toString() + "\n");
				continue;
			}
		}
	}

	public synchronized void updateRunningStats(String client, TournamentModuleState state, boolean isHost)
	{
		int fpm = 24 * 60;
		int fps = 24;
		int minutes = state.frameCount / fpm;
		int seconds = (state.frameCount / fps) % 60;
		gui.UpdateRunningStats(	client,
								state.selfName,
								state.enemyName,
								state.mapName,
								"" + minutes + ":" + (seconds < 10 ? "0" + seconds : seconds),
								state.selfWin == 1 ? "Victory" : "");
	}

		public synchronized void updateStartingStats(String client, int startingTime)
	{
		gui.UpdateRunningStats(	client,
								"",
								"",
								"",
								"" + startingTime + "s",
								"");
	}

	public synchronized void updateStatusTable()
	{
		for (int i = 0; i < clients.size(); i++)
		{
			ServerClientThread c = clients.get(i);

			updateClientStatus(c);
        }
	}

	public synchronized void updateClientGUI(ServerClientThread c)
	{
		if (c != null)
		{

			String client = c.toString();
			String status = "" + c.getStatus();
			String gameNumber = "";
			String hostBotName = "";
			String awayBotName = "";

			InstructionMessage ins = c.lastInstructionSent;

			if (ins != null)
			{
				gameNumber = "" + ins.game_id + " / " + ins.round_id;
				hostBotName = ins.hostBot.getName();
				awayBotName = ins.awayBot.getName();
			}

			if (status.equals("READY"))
			{
				gameNumber = "";
				hostBotName = "";
				awayBotName = "";
			}

			gui.UpdateClient(client, status, gameNumber, hostBotName, awayBotName);
		}
	}

	public static String getTimeStamp()
	{
		return new SimpleDateFormat("[HH:mm:ss]").format(Calendar.getInstance().getTime());
	}

	public synchronized void log(String s)
	{
		gui.logText(getTimeStamp() + " " + Thread.currentThread().getName() + ":	" + s.trim()+"\n");
		System.out.println(getTimeStamp() + " " + Thread.currentThread().getName() + ":	" + s.trim());
	}

	private synchronized void removeNonFreeClientsFromFreeList()
	{
		for (int i = 0; i < free.size(); i++)
		{
            if (free.get(i).getStatus() != ClientStatus.READY)
			{
                free.remove(i);
                log("AddClient(): Non-Free Client in Free List\n");
            }
        }
	}

	public synchronized void updateClientStatus(ServerClientThread c)
	{
		if (c != null)
		{
            if (!clients.contains(c))
			{
                clients.add(c);
                log("New Client Added: " + c.toString() + "\n");
                // send t-module settings
                c.sendTournamentModuleSettings();
            }
            if (c.getStatus() == ClientStatus.READY && !free.contains(c))
			{
                free.add(c);
                log("Client Ready: " + c.toString() + "\n");
            }
        }
	}

    public synchronized boolean updateClient(ServerClientThread c)
	{
		// double check to make sure the free list is correct
        removeNonFreeClientsFromFreeList();

		// update this client's status in the list
		updateClientStatus(c);
		updateClientGUI(c);

        return true;
    }

    public synchronized int getNeededClients(Game game)
	{
		return 2;
	}

	public synchronized void abortAnyStartingGameIfNecessary()
	{
		// one of the 2 client instances has just been crashed
		// while the only other instance is still trying to start the game
		// abort the current start game attempt so that it can be retried properly
		if (getClientsCount() <= 2
			&& isAnyGameStarting()
		) {
			abortStartingGame();
		}
	}

	synchronized boolean isAnyGameStarting()
	{
		for (int i = 0; i < clients.size(); i++)
		{
			ServerClientThread c = clients.get(i);

			if (c.getStatus() == ClientStatus.STARTING)
			{
				return true;
			}
		}

		return false;
	}

	synchronized int getClientsCount()
	{
		return clients.size();
	}

    /**
     * Handles all of the code needed to start a 1v1 game
     */
    private synchronized void start1v1Game(Game game) throws Exception
	{
    	previousScheduledGame = game;

		// get the clients and their instructions
		ServerClientThread hostClient = free.get(0);
		ServerClientThread awayClient = free.get(1);
		InstructionMessage hostInstructions = new InstructionMessage(ServerSettings.Instance().bwapi, true, game);
		InstructionMessage awayInstructions = new InstructionMessage(ServerSettings.Instance().bwapi, false, game);

		log("Starting Game: (" + hostInstructions.game_id + " / " + hostInstructions.round_id + ") "
								  + hostInstructions.hostBot.getName() + " vs. " + hostInstructions.awayBot.getName() + "\n");

		// set the clients to starting
        hostClient.setStatus(ClientStatus.STARTING);
		awayClient.setStatus(ClientStatus.STARTING);

		// send instructions and files to the host machine
        hostClient.sendMessage(hostInstructions);
		hostClient.sendRequiredFiles(hostInstructions.hostBot.getId());
		hostClient.sendBotFiles(hostInstructions.hostBot.getId());

		// send instructions and files to the away machine
		awayClient.sendMessage(awayInstructions);
		awayClient.sendRequiredFiles(hostInstructions.awayBot.getId());
		awayClient.sendBotFiles(awayInstructions.awayBot.getId());

		// start games on those machines
		hostClient.sendMessage(new StartGameMessage());
		awayClient.sendMessage(new StartGameMessage());

		// set the game to running
        game.setStatus(GameStatus.RUNNING);
        game.startTime();

		// remove the clients from the free list
        free.remove(hostClient);
        free.remove(awayClient);

		updateClientGUI(hostClient);
		updateClientGUI(awayClient);

		// call garbage collector (just in case)
		System.gc();
    }

    void shutDown()
	{
    	try
    	{
	        for (int i = 0; i < clients.size(); i++)
			{
	            clients.get(i).sendMessage(new ServerShutdownMessage());
	        }
    	}
    	catch (Exception e)
    	{

    	}
    	finally
    	{
    		System.exit(0);
    	}
    }

    void abortStartingGame()
	{
    	try
    	{
	        for (int i = 0; i < clients.size(); i++)
			{
	            clients.get(i).sendMessage(new AbortGameMessage());
	        }
    	}
    	catch (Exception e)
    	{

    	}
    }

	public void setListener(ServerListenerThread l)
	{
        listener = l;
    }

    public synchronized void receiveGameResults(Game game)
	{
		try
		{
			log("Recieving Replay: (game id " + game.getGameID()+")\n");				// EXCEPTION HERE
			log("Recieving Replay: (game id " + game.getGameID() + ")");
			Game g = ServerSettings.Instance().database.getGameById(game.getGameID());

			g.updateWithGame(game);

			// write the results to database
			log("Saving results to DB.");
			g.saveResultToDatabase();

			// copy contents of bot's write folders to their read folders
			log("Copying contents of bot's 'write' folder to 'read' folder.");
			ServerCommands.Server_MoveWriteToRead(g.getHomebot(), g.getAwaybot());

			// check if the bots aren't inactive (not doing anything for 2 games in a row) and disables them (but only in uncompetitive phase)
			if (!ServerSettings.Instance().CompetitivePhase) {
				log("Checking if we should disable some bot.");
				ServerSettings.Instance().database.disableInactiveBots(g);
			}

		}
		catch (Exception e)
		{
			EmailUtil.sendExceptionViaGmail(e);
			e.printStackTrace();
			log("Error Receiving Game Results\n");
		}

		// check if we shouldn't restart VMs now
		int restartFrequency = ServerSettings.Instance().RestartAfterGames;
		if (lastVMRestartId == -1) lastVMRestartId = game.getGameID();
		if ((restartFrequency != 0) && (game.getGameID() == lastVMRestartId+restartFrequency-1)) {
			lastVMRestartId = game.getGameID();
			log("Restarting VMs after "+restartFrequency+" games (gameId="+game.getGameID()+").");
			WatchdogUtils.restartManagerAndVMs(game.getGameID(),true,"scheduled"+String.valueOf(ServerSettings.Instance().RestartAfterGames));
		} 

    }

    public int getClientIndex(ServerClientThread c)
    {
    	return clients.indexOf(c);
    }


    public int getPort()
	{
        return ServerSettings.Instance().ServerPort;
    }

    public void setupServer()
	{
		log("Server: Created, Running Setup...\n");
		ServerCommands.Server_InitialSetup();
		log("Server: Setup Successful. Ready!\n");
    }

    synchronized public void removeClient(ServerClientThread c)
	{
        this.clients.remove(c);
        this.free.remove(c);

		gui.RemoveClient(c.toString());
		updateStatusTable();
    }

    synchronized public void killClient(String ip)
	{
        log("Attempting to kill client: " + ip);
        for (int i = 0; i < clients.size(); i++)
		{
            if (clients.get(i).getAddress().toString().contentEquals(ip))
			{
                log("Client Found and Stopped\n");
                free.remove(clients.get(i));
                clients.get(i).stopThread();
                clients.remove(i);
                return;
            }
        }
    }
}
class FileCopyThread extends Thread
{
	String source;
	String dest;
	Server server;

	public FileCopyThread(Server m, String source, String dest)
	{
		this.source = source;
		this.dest = dest;
		server = m;

		server.log("File Copy Thread Initialized\n");
	}

	public void run()
	{
		server.log("File Copy Thread Started()\n");

		while(true)
		{
			try
			{
				Thread.sleep(5000);
				server.log("Trying to copy file to web_docs\n");
				copyFileWindows(source, dest);
				server.log("SUCCESS   : " + source + " copied to " + dest + "\n");
				copyFileWindows(dest, "y:\\web_docs\\index.html");
				server.log("SUCCESS   : Final Copy\n");
			}
			catch (Exception e)
			{
				server.log("FAIL   : " + source + " not copied to " + dest + "\n");
			}
		}
	}

	public void copyFileWindows(String s, String d) throws Exception
	{
		String[] args = { "CMD", "/C", "COPY", "/Y", s, d };
		Process p = Runtime.getRuntime().exec(args);
		p.waitFor();
	}

	public void copyFile() throws IOException
	{
		File sourceFile = new File(source);
		File destFile = new File(dest);

		if(!destFile.exists())
		{
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try
		{
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		}
		finally
		{
			if(source != null)
			{
				source.close();
			}
			if(destination != null)
			{
				destination.close();
			}
		}
	}
}
