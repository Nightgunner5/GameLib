package net.llamaslayers.gamelib.networking;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractClient extends Thread {
	private final Socket socket;
	private final ObjectInputStream in;
	private final ObjectOutputStream out;
	private final Deque<Serializable> queue = new LinkedList<Serializable>();

	public AbstractClient(InetAddress ip, int port) throws IOException {
		socket = new Socket(ip, port);
		socket.getOutputStream().write('C');
		out = new ObjectOutputStream(socket.getOutputStream());
		int serverInit = socket.getInputStream().read();
		if (serverInit != 'S') {
			throw new IOException("Server INIT " + (char) serverInit + " != S");
		}
		in = new ObjectInputStream(socket.getInputStream());
	}

	@Override
	public void run() {
		while (!interrupted()) {
			try {
				Serializable s = (Serializable) in.readObject();
				if (s instanceof DisconnectPacket) {
					if (!((DisconnectPacket) s).response) {
						write(DisconnectPacket.DISCONNECT_ACK);
					}
					interrupt();
				} else {
					synchronized (queue) {
						queue.add(s);
						queue.notify();
					}
				}
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
		if (!isAlive()) {
			throw new IllegalStateException("Client thread has not been started before write.");
		}
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

	public Serializable queryPacket() {
		synchronized (queue) {
			return queue.poll();
		}
	}

	public Serializable waitForPacket() throws InterruptedException {
		synchronized (queue) {
			if (!queue.isEmpty()) {
				return queue.remove();
			}
			queue.wait();
			return queue.remove();
		}
	}

	public Serializable waitForPacket(long maxWait) throws InterruptedException, NoSuchElementException {
		synchronized (queue) {
			if (!queue.isEmpty()) {
				return queue.remove();
			}
			queue.wait(maxWait);
			return queue.remove();
		}
	}

	public boolean finished() {
		synchronized (queue) {
			return socket.isClosed() && queue.isEmpty();
		}
	}
}
