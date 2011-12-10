package net.llamaslayers.gamelib.networking;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractClient extends Thread {
	private final Socket socket;
	private final ObjectInputStream in;
	private final ObjectOutputStream out;

	public AbstractClient(InetAddress ip, int port) throws IOException {
		socket = new Socket(ip, port);
		socket.getOutputStream().write('C');
		out = new ObjectOutputStream(socket.getOutputStream());
		int serverInit = socket.getInputStream().read();
		if (serverInit != 'S')
			throw new IOException("Server INIT " + (char) serverInit + " != S");
		in = new ObjectInputStream(socket.getInputStream());
	}

	@Override
	public void run() {
		while (!interrupted()) {
			try {
				Serializable s = (Serializable) in.readObject();
				process(s);
			} catch (EOFException ex) {
				interrupt();
			} catch (IOException ex) {
				Logger.getLogger(AbstractClient.class.getName()).log(Level.SEVERE, null, ex);
				interrupt();
			} catch (ClassNotFoundException ex) {
				Logger.getLogger(AbstractClient.class.getName()).log(Level.SEVERE, null, ex);
				interrupt();
			}
		}
		try {
			socket.close();
		} catch (IOException ex) {
			Logger.getLogger(AbstractClient.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void write(Serializable packet) {
		if (!isAlive())
			throw new IllegalStateException("Client thread has not been started before write.");
		if (packet == null) { // Don't send null packets.
			return;
		}
		synchronized (out) {
			try {
				out.writeObject(packet);
				out.flush();
			} catch (IOException ex) {
				Logger.getLogger(AbstractClient.class.getName()).log(Level.SEVERE, null, ex);
				interrupt();
			}
		}
	}

	protected abstract void process(Serializable packet);

	public boolean finished() {
		return socket.isClosed();
	}
}
