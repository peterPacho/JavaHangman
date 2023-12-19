import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.function.Consumer;

public class ClientConnection extends Thread
{
	private Consumer<Serializable> callback;
	private Socket socket = null;
	private ObjectOutputStream out = null;
	private ObjectInputStream in = null;

	public ClientConnection (Consumer<Serializable> callback, String ip, int port)
	{
		this.callback = callback;

		try
		{
			socket = new Socket(ip, port);
		}
		catch (Exception e)
		{
			callback.accept("err:socket");
		}

		try
		{
			out = new ObjectOutputStream(socket.getOutputStream());
			in = new ObjectInputStream(socket.getInputStream());
			socket.setTcpNoDelay(true);
		}
		catch (Exception e)
		{
			callback.accept("err:streams");
		}
	}

	public void run ()
	{
		if (socket != null && in != null && out != null)
		{
			callback.accept("ok:connected");
		}
	}

	public void sendMsg (String msg)
	{
		try
		{
			out.writeObject(msg);
		}
		catch (Exception ignored)
		{

		}
	}

	public void close ()
	{
		try
		{
			socket.close();
			socket = null;
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public Object getObject ()
	{
		try
		{
			return in.readObject();
		}
		catch (Exception ignored)
		{
		}
		return null;
	}

	public String getMsg ()
	{
		Object returned = getObject();
		if (returned == null)
			return null;
		return returned.toString();
	}
}
