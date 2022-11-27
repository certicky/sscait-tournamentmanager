package utility;

import java.io.*;
import java.util.*;

import client.Client;

public class WindowsCommandTools
{
	public static void DeleteDirectory(String dir) 
	{
		try
		{
			WindowsCommandTools.RunWindowsCommand("rmdir /S /Q " + dir, true, false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void RestartMachine() 
	{
		Client.log("Restarting the machine now...");
		Runtime r = Runtime.getRuntime();
		try {
			r.exec("shutdown -r");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static long GetFreeMemory() {
		long allocatedMemory      = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		long presumableFreeMemory = Runtime.getRuntime().maxMemory() - allocatedMemory;
		return presumableFreeMemory;
	}
	
	public static void RunWindowsExeLocal(String dir, String exe, boolean waitFor, boolean exitIfException)
	{
		String windowsCommandPrefix = "CMD /C ";
		String completeCommand = windowsCommandPrefix + "\"cd /D " + dir + " & start " + exe+"\"";
		Process proc;
		Client.log("	Running: "+completeCommand);
		
		try {
			proc = Runtime.getRuntime().exec(completeCommand);
			if (waitFor){
				try {
					proc.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
					if (exitIfException){
							Client.log("The client is exiting due to the exception in EXE.");
							System.exit(-1);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (exitIfException){
				Client.log("The client is exiting due to the exception in EXE.");
				System.exit(-1);
			}
		}
	}
	
	public static void RunWindowsExeLocalAsAdmin(String dir, String exe, boolean waitFor, boolean exitIfException)
	{
		String windowsCommandPrefix = "CMD /C runas /user:Administrator ";
		String completeCommand = windowsCommandPrefix + "\"cd /D " + dir + " & start " + exe+"\"";
		Process proc;
		Client.log("	Running: "+completeCommand);
		
		try {
			proc = Runtime.getRuntime().exec(completeCommand);
			if (waitFor){
				try {
					proc.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
					if (exitIfException){
							Client.log("The client is exiting due to the exception in EXE.");
							System.exit(-1);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			if (exitIfException){
				Client.log("The client is exiting due to the exception in EXE.");
				System.exit(-1);
			}
		}
	}	
	
	public static void KillWindowsProcessID(int pid)
	{
		WindowsCommandTools.RunWindowsCommand("taskkill /F /PID " + pid, true, false);
	}
	
	public static void KillExcessWindowsProccess(Vector<Integer> doNotKill) 
	{
		if (doNotKill == null)
		{
			return;
		}
	
		Vector<Integer> running = GetRunningProcesses();
		for (int i = 0; i < running.size(); i++) 
		{
			if (!(doNotKill.contains(running.get(i)))) 
			{
				KillWindowsProcessID(running.get(i));
			}
		}
	}
	
	public static Vector<Integer> GetRunningProcesses() 
	{
		Vector<Integer> running = new Vector<Integer>();
		Process p;
		try 
		{
			p = Runtime.getRuntime().exec("tasklist.exe /FO LIST");

			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			
			String[] splitout;
			while ((line = stdInput.readLine()) != null) 
			{
				if (line.startsWith("PID:")) 
				{
					splitout = line.split(":");
					if (splitout.length > 1) 
					{
						running.add(Integer.parseInt(splitout[1].trim()));
					}
				}
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		return running;
	}
	
	public static boolean IsWindowsProcessRunning(String process) 
	{
		try 
		{
			Process p = Runtime.getRuntime().exec("tasklist.exe");
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line;
			while ((line = stdInput.readLine()) != null) 
			{
				if (line.contains(process)) 
				{
					return true;
				}
			}
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		
		return false;
	}
	
	public static void RunWindowsCommand(String command, boolean waitFor, boolean exitIfException)
	{
		try
		{
			String windowsCommandPrefix = "CMD /C ";
			Process proc = Runtime.getRuntime().exec(windowsCommandPrefix + command);
			if (waitFor)
			{
				proc.waitFor();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			if (exitIfException)
			{
				System.exit(-1);
			}
		}
	}
	
	public static void RunWindowsCommandAsAdmin(String command, boolean waitFor, boolean exitIfException)
	{
		try
		{
			String windowsCommandPrefix = "CMD /C runas /user:Administrator ";
			Process proc = Runtime.getRuntime().exec(windowsCommandPrefix + command);
			if (waitFor)
			{
				proc.waitFor();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("\n\nCommandTools: Could not run command:\n" + command + "\n\n");
			
			if (exitIfException)
			{
				System.exit(-1);
			}
		}
	}
	
	public static void RegEdit(String keyName, String valueName, String type, String data)
	{
		String q = "\"";
	
		// make sure there are no quotations in the input strings
		keyName.replaceAll(q, "");
		valueName.replaceAll(q, "");
		type.replaceAll(q, "");
		data.replaceAll(q, "");
		
		// wrap quotations around the values to be sure
		keyName = q + keyName + q;
		valueName = q + valueName + q;
		data = q + data + q;
		
		String cmd = "reg add " + keyName + " /f /v " + valueName + " /t " + type + " /d " + data;
		RunWindowsCommand(cmd, true, false);
	}
	
	public static void main(String[] args)
	{
		RunWindowsCommandAsAdmin("notepad", true, false);	
	}
}