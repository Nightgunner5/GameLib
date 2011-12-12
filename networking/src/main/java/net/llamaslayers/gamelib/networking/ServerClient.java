package net.llamaslayers.gamelib.networking;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServerClient extends Thread {
	private final int id;
	private final AbstractServer server;
	private final Socket socket;
	private final AtomicReference<ObjectInputStream> in;
	private final AtomicReference<ObjectOutputStream> out;
	private final Deque<Serializable> queue = new LinkedList<Serializable>();

	ServerClient(Socket socket, int id, ThreadGroup group, AbstractServer server) throws IOException {
		super(group, "CLIENT#" + id + ": " + socket.getInetAddress().toString() + " on port " + socket.getPort());
		this.id = id;
		this.server = server;
		this.socket = socket;
		socket.setSoTimeout(30000);
		socket.getOutputStream().write('S');
		out = new AtomicReference<ObjectOutputStream>(new ObjectOutputStream(socket.getOutputStream()));
		int clientInit = socket.getInputStream().read();
		if (clientInit != 'C')
			throw new IOException("Client INIT " + (char) clientInit + " != C");
		in = new AtomicReference<ObjectInputStream>(new ObjectInputStream(socket.getInputStream()));
	}

	@Override
	public void run() {
		while (!interrupted()) {
			try {
				Serializable s = (Serializable) in.get().readObject();
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
					in.get().close();
					in.set(new ObjectInputStream(socket.getInputStream()));
				}
			} catch (EOFException ex) {
				interrupt();
			} catch (IOException ex) {
				if (!interrupted())
					Logger.getLogger(ServerClient.class.getName()).log(Level.SEVERE, null, ex);
				interrupt();
			} catch (ClassNotFoundException ex) {
				Logger.getLogger(ServerClient.class.getName()).log(Level.SEVERE, null, ex);
				interrupt();
			}
		}
		try {
			in.get().close();
			out.get().close();
			socket.close();
		} catch (IOException ex) {
			Logger.getLogger(ServerClient.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			if (finished()) {
				server.clientIsFinished(this);
			}
		}
	}

	public void write(Serializable packet) {
		if (packet == null) { // Don't send null packets.
			return;
		}
		synchronized (out) {
			try {
				out.get().writeObject(packet);
				out.get().flush();
				out.get().close();
				out.set(new ObjectOutputStream(socket.getOutputStream()));
			} catch (IOException ex) {
				Logger.getLogger(ServerClient.class.getName()).log(Level.SEVERE, null, ex);
				interrupt();
			}
		}
	}

	public Serializable queryPacket() {
		try {
			synchronized (queue) {
				return queue.poll();
			}
		} finally {
			if (finished()) {
				server.clientIsFinished(this);
			}
		}
	}

	public Serializable waitForPacket() throws InterruptedException {
		try {
			synchronized (queue) {
				if (!queue.isEmpty()) {
					return queue.remove();
				}
				queue.wait();
				return queue.remove();
			}
		} finally {
			if (finished()) {
				server.clientIsFinished(this);
			}
		}
	}

	public Serializable waitForPacket(long maxWait) throws InterruptedException, NoSuchElementException {
		try {
			synchronized (queue) {
				if (!queue.isEmpty()) {
					return queue.remove();
				}
				queue.wait(maxWait);
				return queue.remove();
			}
		} finally {
			if (finished()) {
				server.clientIsFinished(this);
			}
		}
	}

	public boolean finished() {
		synchronized (queue) {
			return socket.isClosed() && queue.isEmpty();
		}
	}

	public int getClientId() {
		return id;
	}
}
