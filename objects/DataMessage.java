package objects;

import server.EmailUtil;
import utility.*;

public class DataMessage implements Message
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8193083217816970522L;
	public DataType type;		// the type of data
	public byte[] data;			// the data
	public String botName;		// associated bot name (if necessary)
	
	public DataMessage(DataType type, String src)
	{
		this.type = type;
		
		// check if this is a zip file already
		if (src.contains(".zip"))
		{
			readZipFile(src);
		}
		else
		{
			// otherwise we need to zip the directory
			read(src);
		}
		
	}
	
	public DataMessage(DataType type, String botName, String src)
	{
		this.type = type;
		this.botName = botName;
		read(src);
	}
	
	public String toString()
	{
		return "" + type + (botName == null ? "" : (" " + botName)) + " " + (data.length/1000) + " kb";
	}
	
	public void read(String src)
	{
		try
		{
			data = ZipTools.ZipDirToByteArray(src);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e);
			System.exit(-1);
		}
	}
	
	public void readZipFile(String src)
	{
		try
		{
			data = ZipTools.LoadZipFileToByteArray(src);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e);
			System.exit(-1);
		}
	}
	
	public void write(String dest)
	{
		//final File d = new File(dest);
		//if (!d.mkdirs()) {
		//   System.err.println("Could not create directories of the destination path.");
		//}
		
		try
		{
			ZipTools.UnzipByteArrayToDir(data, dest);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			EmailUtil.sendExceptionViaGmail(e);
		}
	}
}