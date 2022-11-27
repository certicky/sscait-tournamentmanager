package server;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import objects.*;
import utility.FileUtils;

public class ServerClientThread extends Thread implements Comparable<ServerClientThread>
{

	private InetAddress 		address;
	public  InstructionMessage 	lastInstructionSent = null;
	private Socket 				con;
	private ClientStatus 		status;
	private Server 				server;

	private ObjectInputStream 	ois = null;
	private ObjectOutputStream 	oos = null;
	private boolean	 			run = true;

	public ServerClientThread(Socket con, Server man) throws SocketException
	{
		address = con.getInetAddress();
		this.con = con;
		con.setKeepAlive(true);
		server = man;
		status = ClientStatus.READY;
	}

	public void run()
	{
		setupConnectionStreams();

		while (run)
		{
			try
			{
				Message m = (Message) ois.readObject();
				handleClientMessage(m);
			}
			catch (Exception e)
			{
				// this exception happens when client shuts down, for example due to VM restart (no need to print it out)
				server.log("Exception in ManagerClientThread, removing client\n");
				server.removeClient(this);

				server.abortAnyStartingGameIfNecessary();

				run = false;
			}
		}
	}

	private void setupConnectionStreams()
	{
		try
		{
			oos = new ObjectOutputStream(con.getOutputStream());
			ois = new ObjectInputStream(con.getInputStream());
		}
		catch (Exception e)
		{
			server.log("ManagerClientThread Object Streams could not initialize\n");
			EmailUtil.sendExceptionViaGmail(e);
			e.printStackTrace();
		}
	}

	private void handleClientMessage(Message m) throws Exception
	{
		if (m != null)
		{
			if (m instanceof ClientStatusMessage)
			{
				updateClientStatus((ClientStatusMessage)m);
			}
			else if (m instanceof DataMessage)
			{
				DataMessage dm = (DataMessage)m;

				// save replay file to disk
				if (dm.type == DataType.REPLAY)
				{
					server.log("Message from Client " + server.getClientIndex(this) + " : " + m.toString() + "\n");
					dm.write(ServerSettings.Instance().ServerReplayDir);
				}

				// save "write" folder to disk
				else if (dm.type == DataType.WRITE_DIR)
				{
					dm.write(ServerSettings.Instance().ServerBotDir + String.valueOf(ServerSettings.Instance().database.getBotIdFromName(dm.botName)) + "//write");
				}
			}
		}
	}

	private void updateClientStatus(ClientStatusMessage m)
	{
		status = m.status;
		server.updateClient(this);

		// if the status is READY, check if we should abort any starting games from before
		if (status == ClientStatus.READY)
		{
			server.abortAnyStartingGameIfNecessary();
		}

		// if the status is sending, grab the replay
		if (status == ClientStatus.SENDING)
		{
			server.receiveGameResults(m.game);
		}

		if (m.status == ClientStatus.RUNNING && m.gameState != null)
		{
			server.updateRunningStats(this.toString(), m.gameState, m.isHost);
		}

		if (m.status == ClientStatus.STARTING && m.startingTime > 0)
		{
			server.updateStartingStats(this.toString(), m.startingTime);
		}
	}

	public synchronized void sendMessage(Message m) throws Exception
	{
		server.log("Sending Message to Client " + server.getClientIndex(this) + " : " + m.toString() + "\n");

		oos.writeObject(m);
		oos.flush();
		oos.reset();

		if (m instanceof InstructionMessage)
		{
			lastInstructionSent = (InstructionMessage)m;
		}
	}

	public void sendTournamentModuleSettings()
	{
		try
		{
			sendMessage(ServerSettings.Instance().tmSettings);
		}
		catch (Exception e)
		{
			EmailUtil.sendExceptionViaGmail(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void sendBotFiles(String botId)
	{
		try
		{
			Message m = new DataMessage(DataType.BOT_DIR, botId, ServerSettings.Instance().ServerBotDir + botId);
			sendMessage(m);
		}
		catch (Exception e)
		{
			EmailUtil.sendExceptionViaGmail(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void sendRequiredFiles(String botId)
	{

		// Get the MD5 checksum of the BWAPI.dll file attached to the bot
		// default: 3.4.7. (MD5 6e940dc6acc76b6e459b39a9cdd466ae)
		String md5checksum = "6e940dc6acc76b6e459b39a9cdd466ae";
		String attachedBWAPIdllPath = ServerSettings.Instance().ServerBotDir+botId+File.separator+"AI"+File.separator+"BWAPI.dll";
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			md.update(Files.readAllBytes(Paths.get(attachedBWAPIdllPath)));
			byte[] digest = md.digest();
			md5checksum = DatatypeConverter.printHexBinary(digest).toLowerCase();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e1);
		} catch (IOException e) {}
		// Make a copy of Starcraft folder specific for this bot (it should contain correct versions of BWAPI.dll and TournamentModule.dll)
		String requirementsForThisBotPath = ServerSettings.Instance().ServerRequiredDir + "temp"+File.separator + botId+"_Starcraft";
		FileUtils.CopyDirectory(ServerSettings.Instance().ServerRequiredDir+"Starcraft", requirementsForThisBotPath); // copy the standard Starcraft folder to the temp folder
		FileUtils.CopyDirectory(ServerSettings.Instance().ServerRequiredDir+"BWAPI_versions"+File.separator+md5checksum, requirementsForThisBotPath); // add specific BWAPI.dll and TournamentModule.dll that this bot requires from BWAPI_versions folder

		// Send the prepared requirements ZIP file to the client and delete it
		try
		{
			Message m = new DataMessage(DataType.REQUIRED_DIR, requirementsForThisBotPath);
			sendMessage(m);
			FileUtils.DeleteDirectory(new File(requirementsForThisBotPath));
		}
		catch (Exception e)
		{
			EmailUtil.sendExceptionViaGmail(e);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public String toString()
	{
		return "" + this.getAddress();
	}

	public synchronized InetAddress getAddress()
	{
		return address;
	}

	public synchronized ClientStatus getStatus()
	{
		return status;
	}

	public void stopThread()
	{
		this.interrupt();
		try
		{
			this.con.close();
		}
		catch
		(IOException e)
		{
			EmailUtil.sendExceptionViaGmail(e);
			e.printStackTrace();
		}

		run = false;
	}

	public synchronized void setStatus(ClientStatus status)
	{
		this.status = status;
	}

	@Override
	public int compareTo(ServerClientThread arg0)
	{
		return this.address.equals((arg0).getAddress()) ? 0 : 1;
	}
}
