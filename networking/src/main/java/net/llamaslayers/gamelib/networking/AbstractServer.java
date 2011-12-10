package net.llamaslayers.gamelib.networking;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractServer implements Runnable {
	private final ThreadGroup clientGroup = new ThreadGroup("Client connections");
	private final ConcurrentHashMap<Integer, ServerClient> clients = new ConcurrentHashMap<Integer, ServerClient>();
	private final ServerSocket socket;
	private int nextClientID = 1;

	public AbstractServer(int port) throws IOException {
		socket = new ServerSocket(port);
	}

	@Override
	public final void run() {
		while (!socket.isClosed()) {
			try {
				ServerClient client = new ServerClient(socket.accept(), nextClientID, clientGroup, this);
				clients.put(nextClientID, client);
				client.start();
				handleNewConnection(client);
				nextClientID++;
			} catch (IOException ex) {
				if (!socket.isClosed())
					Logger.getLogger(AbstractServer.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public final void stop() {
		try {
			for (ServerClient client : clients.values()) {
				client.interrupt();
			}
			clients.clear();
			socket.close();
		} catch (IOException ex) {
			Logger.getLogger(AbstractServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public ServerClient getClient(int id) throws NoSuchElementException {
		if (clients.containsKey(id))
			return clients.get(id);
		throw new NoSuchElementException("No client with ID " + id);
	}

	public abstract void handleNewConnection(ServerClient client);

	void clientIsFinished(ServerClient client) {
		clients.remove(client.getClientId());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (ServerClient client : clients.values()) {
			sb.append(client).append('\n');
		}
		return sb.toString();
	}
}
