import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

/*

	Main server class. It creates sockets and threads.
	Server is run in separate thread, and then for each connected
	client new thread is created.

*/
public class Server
{
	private int id = 0; // also represents the amount of clients connected
	private ArrayList<TheServer.ClientThread> clients = null;
	private final Consumer<Serializable> callback;

	Server (Consumer<Serializable> callback)
	{
		clients = new ArrayList<>();
		this.callback = callback;
		TheServer server = new TheServer();
		server.start();
	}

	// Server is also separate thread from the UI
	public class TheServer extends Thread
	{
		public void run ()
		{
			try
			{
				ServerSocket serverSocket = new ServerSocket(StartUI.port);
				callback.accept("Server started.$$green");

				while (true)
				{
					ClientThread c = new ClientThread(serverSocket.accept(), id);
					clients.add(c);
					c.start();

					callback.accept("New client connected with ID " + id + ".");

					id++;
				}
			}
			catch (BindException e)
			{
				callback.accept("Port already in use.$$red");
				callback.accept("Server is not running.$$red");
				callback.accept("(uptime counter is lying)$$grey");
				callback.accept("You can close this window now.");

				throw new RuntimeException(e);
			}
			catch (Exception e)
			{
				callback.accept("Server socket did not launch$$red");
				throw new RuntimeException(e);
			}
		}

		/*

			Each connected client gets his own thread.

		 */
		class ClientThread extends Thread
		{
			Socket socket;
			private ObjectInputStream in;
			private ObjectOutputStream out;
			private final int id;
			private GameLogic game;


			ClientThread (Socket s, int id)
			{
				this.socket = s;
				this.id = id;
				game = new GameLogic(callback, ServerController.categories, id);
			}

			public void run ()
			{
				try
				{
					in = new ObjectInputStream(socket.getInputStream());
					out = new ObjectOutputStream(socket.getOutputStream());
					socket.setTcpNoDelay(true);
				}
				catch (IOException e)
				{
					callback.accept("Couldn't open streams$$red");
					throw new RuntimeException(e);
				}

				// main server loop
				while (true)
				{
					try
					{
						Object response = game.processClientRequest(in.readObject().toString());

						if (response != null)
							out.writeObject(response);
					}
					catch (Exception e)
					{
						callback.accept("Client ID " + id + " disconnected.$$brown");
						clients.remove(this);
						return;
					}
				}

			}
		}
	}
}
