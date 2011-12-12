package net.llamaslayers.gamelib.networking;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractClient extends Thread {
	private final Socket socket;
	private final AtomicReference<ObjectInputStream> in;
	private final AtomicReference<ObjectOutputStream> out;
	private final NoCloseInputStream _in;
	private final NoCloseOutputStream _out;
	private final Deque<Serializable> queue = new LinkedList<Serializable>();

	public AbstractClient(InetAddress ip, int port) throws IOException {
		socket = new Socket(ip, port);
		socket.getOutputStream().write('C');
		_out = new NoCloseOutputStream(socket.getOutputStream());
		out = new AtomicReference<ObjectOutputStream>(new ObjectOutputStream(_out));
		int serverInit = socket.getInputStream().read();
		if (serverInit != 'S') {
			throw new IOException("Server INIT " + (char) serverInit + " != S");
		}
		_in = new NoCloseInputStream(socket.getInputStream());
		in = new AtomicReference<ObjectInputStream>(new ObjectInputStream(_in));
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
					in.set(new ObjectInputStream(_in));
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
			in.get().close();
			out.get().close();
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
				out.get().writeObject(packet);
				out.get().flush();
				out.get().close();
				out.set(new ObjectOutputStream(_out));
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
