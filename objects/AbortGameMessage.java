package objects;

public class AbortGameMessage implements Message
{
	private static final long serialVersionUID = -347052767995307052L;

	public AbortGameMessage()
	{
	
	}
	
	public String toString()
	{
		return "Abort the Game!";
	}
}