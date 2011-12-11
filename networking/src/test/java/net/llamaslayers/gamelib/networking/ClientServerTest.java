package net.llamaslayers.gamelib.networking;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;
import junit.framework.TestCase;

public class ClientServerTest extends TestCase {
	public ClientServerTest(String testName) {
		super(testName);
	}
	private AbstractServer server;
	private static final int SERVER_PORT = 4000;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		server = new Server(SERVER_PORT);
		new Thread(server).start();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		server.stop();
	}

	private static class Server extends AbstractServer {
		public Server(int port) throws IOException {
			super(port);
		}

		@Override
		public void handleNewConnection(final ServerClient client) {
			new Thread() {
				@Override
				public void run() {
					while (!client.finished()) {
						try {
							Serializable packet = client.waitForPacket();
							client.write(packet);
						} catch (InterruptedException ex) {
							client.interrupt();
							break;
						}
					}
				}
			}.start();
		}
	}

	private static class PacketFinished implements Serializable {
		private static final long serialVersionUID = 1L;
	}

	private static class Packet1 implements Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean equals(Object o) {
			return o instanceof Packet1;
		}

		@Override
		public int hashCode() {
			int hash = 3;
			return hash;
		}
	}

	private static class Packet2 implements Serializable {
		private static final long serialVersionUID = 1L;

		@Override
		public boolean equals(Object o) {
			return o instanceof Packet2;
		}

		@Override
		public int hashCode() {
			int hash = 7;
			return hash;
		}
	}

	private static class Packet3 implements Serializable {
		private static final long serialVersionUID = 1L;
		public final ArrayList<String> data = new ArrayList<String>();

		public Packet3 add(String text) {
			data.add(text);
			return this;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Packet3 other = (Packet3) obj;
			if (this.data != other.data && (this.data == null || !this.data.equals(other.data))) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			int hash = 3;
			hash = 67 * hash + (this.data != null ? this.data.hashCode() : 0);
			return hash;
		}
	}

	private static class Client extends AbstractClient {
		private final ArrayList<Serializable> packetsRecieved;

		public Client(InetAddress ip, int port, ArrayList<Serializable> packetsRecieved) throws IOException {
			super(ip, port);
			this.packetsRecieved = packetsRecieved;
			new ListenThread().start();
		}

		private class ListenThread extends Thread {
			@Override
			public void run() {
				while (!Client.this.finished() && !interrupted()) {
					Serializable packet;
					try {
						packet = Client.this.waitForPacket();
					} catch (InterruptedException ex) {
						break;
					}
					if (packet instanceof PacketFinished) {
						Client.this.write(DisconnectPacket.DISCONNECT);
						synchronized (Client.this) {
							Client.this.notify();
						}
						Client.this.interrupt();
						return;
					}
					packetsRecieved.add(packet);
				}
			}
		}
	}

	public void testDataOrder() throws IOException, InterruptedException {
		ArrayList<Serializable> packetsSent = new ArrayList<Serializable>();
		ArrayList<Serializable> packetsRecieved = new ArrayList<Serializable>();
		AbstractClient client = new Client(InetAddress.getLocalHost(), SERVER_PORT, packetsRecieved);
		client.start();

		packetsSent.add(new Packet1());
		packetsSent.add(new Packet2());
		for (Serializable packet : packetsSent) {
			client.write(packet);
		}
		client.write(new PacketFinished());
		synchronized (client) {
			if (!client.finished()) {
				client.wait();
			}
		}

		assertEquals("packets should not differ", packetsRecieved, packetsSent);
	}

	public void testComplexSerialization() throws IOException, InterruptedException {
		ArrayList<Serializable> packetsSent = new ArrayList<Serializable>();
		ArrayList<Serializable> packetsRecieved = new ArrayList<Serializable>();
		AbstractClient client = new Client(InetAddress.getLocalHost(), SERVER_PORT, packetsRecieved);
		client.start();

		packetsSent.add(new Packet3().add("This").add("is").add(null).add("a").add("test"));
		for (Serializable packet : packetsSent) {
			client.write(packet);
		}
		client.write(new PacketFinished());
		synchronized (client) {
			if (!client.finished()) {
				client.wait();
			}
		}

		assertEquals("packets should not differ", packetsRecieved, packetsSent);
	}
}
