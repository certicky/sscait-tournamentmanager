package server;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

public class WatchdogUtils {
	
	private static int lastActionGameId = 0;
	
	// log the restart into a logfile
	public static void logRestart(String logMessage) {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // get the datetime for the log msg
		Date now = new Date();
	    String strDate = sdf.format(now);
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(ServerSettings.Instance().ServerLogFilePath, true)))) {
		    out.println(strDate+" "+logMessage);
		    out.close();
		}catch (IOException e) {
			e.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e);
		}
	}
	
	// restart the VMs
	public static synchronized boolean restartManagerAndVMs(int gameId, boolean restartWasScheduled, String logMessage) {
		// don't do anything for one game twice (this function is called twice after 
		// every game -- when we receive result from both VMs. It's enough to check the
		// stream once)
		if (lastActionGameId == gameId) return false;
		lastActionGameId = gameId;
		
		try {
			
			// run pre-restart command if this is scheduled restart
			if (restartWasScheduled) { 
				System.out.println("Running pre-restart command: "+ Arrays.toString(ServerSettings.Instance().PreRestartCommand));
				Runtime.getRuntime().exec(ServerSettings.Instance().PreRestartCommand).waitFor();
			}
			
			// restart VMs using the script 
			Runtime.getRuntime().exec(ServerSettings.Instance().RestartAllCommand).waitFor();
			logRestart("restartall.sh called by the server. MSG: "+logMessage); //+" CMD: "+Arrays.toString(ServerSettings.Instance().RestartAllCommand));
			Thread.sleep(1000);
			System.exit(1);

		} catch (InterruptedException e) {
			e.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e);
		} catch (IOException e) {
			e.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e);
		}
		
		return true;
	}
	
	// returns "ok" if the stream is running and it's not streaming black screen (hitbox.tv specific)
	// returns other status messages otherwise
	public static String streamIsRunningHitbox() {
		
		// is stream running? (use hitbox API)
    	String content;
		try {
			content = WatchdogUtils.getTextFromUrl("http://api.hitbox.tv/media/live/sscaitournament");
			// return false if hitbox API says we're offline
			if (content.contains("\"media_is_live\":\"0\"")) return "streamOffline";
		} catch (Exception e) {
			// notify the admin if we can't contact hitbox API
			e.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e);
		}
		
		/* THIS IS DISABLED NOW (CURRENTLY PERFORMED BY PYTHON SCRIPT CALLED BY CRON) 
		// do we stream black screen?
		URL url;
		try {
			url = new URL("http://edge.sf.hitbox.tv/static/img/media/live/sscaitournament_large_000.jpg");
			BufferedImage image = ImageIO.read(url);
			boolean imageBlack = true;
			for (int x = 1; x < image.getWidth(); x += 20) {
				for (int y = 1; y < image.getHeight(); y += 20) {
					// if the pixel is not black, the image is not black
					if (new Color(image.getRGB(x, y)).equals(Color.black) == false) {
						imageBlack = false;
						break;
					}
				}
				if (imageBlack == false) break;
			}
			if (imageBlack) return "streamingBlackScreen";
		} catch (IOException e) {
			// notify the admin if we can't retrieve the thumbnail image from hitbox
			e.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e);
		}
		*/
		
		return "ok";
	}
    
	// helper function -- reads the URL to string
	public static String getTextFromUrl(String url){
        String response = "";
        String line;
        try {
	        URL website = new URL(url);
	        HttpURLConnection huc;
			huc = (HttpURLConnection) website.openConnection();
	        HttpURLConnection.setFollowRedirects(false);
	        // timeouts
	        huc.setConnectTimeout(10 * 1000);
	        huc.setReadTimeout(15 * 1000);
	        huc.setRequestMethod("GET");
	        huc.setRequestProperty("User-Agent", "SSCAITServerBot (+http://sscaitournament.com/)");
	        huc.connect();
	        InputStream is = huc.getInputStream();
	        BufferedReader br = new BufferedReader(new InputStreamReader(is));
	        while ( (line = br.readLine()) != null) response += line+"\n";
	        br.close();
	        is.close();
		}  catch (SocketTimeoutException e) {
			// do nothing on timeout. this happens quite often
			System.out.println("Request to "+url+" timed out.");
		} catch (Exception e) {
			EmailUtil.sendExceptionViaGmail(e);
			e.printStackTrace();
		}
        return response;
	}
}