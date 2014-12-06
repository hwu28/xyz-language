//This is some of the worst code I have written...
public class Token implements Cloneable
{
	public final int type;
	public final double value;

	public Token(int type, double value)
	{
		this.type = type;
		this.value = value;
	}

	@Override public String toString()
	{
		return "["+type+","+value+"]";
	}

	public Token clone() throws CloneNotSupportedException
	{
		return (Token) super.clone();
	}
}
