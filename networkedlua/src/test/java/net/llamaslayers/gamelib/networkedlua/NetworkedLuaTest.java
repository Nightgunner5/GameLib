package net.llamaslayers.gamelib.networkedlua;

import java.io.Serializable;
import java.io.IOException;
import java.net.InetAddress;
import net.llamaslayers.gamelib.networking.AbstractClient;
import net.llamaslayers.gamelib.networking.AbstractServer;
import net.llamaslayers.gamelib.networking.DisconnectPacket;
import net.llamaslayers.gamelib.networking.ServerClient;
import net.llamaslayers.gamelib.scripting.Lua;
import net.llamaslayers.gamelib.scripting.LuaException;
import net.llamaslayers.gamelib.scripting.LuaRandom;
import org.junit.*;
import static org.junit.Assert.*;

public class NetworkedLuaTest {
	private static final int SERVER_PORT = 4000;

	@Test
	public void testClient() throws IOException, LuaException, InterruptedException {
		AbstractServer server = new AbstractServer(SERVER_PORT) {
			@Override
			public void handleNewConnection(final ServerClient client) {
				try {
					Serializable packet = client.waitForPacket();
					assertTrue(packet instanceof LuaRandom);
					client.write(packet);
				} catch (InterruptedException ex) {
				}
				client.interrupt();
			}
		};
		Thread serverThread = new Thread(server);
		serverThread.start();
		AbstractClient client = new AbstractClient(InetAddress.getLocalHost(), SERVER_PORT) {
		};
		client.start();
		LuaUtil.install(client);
		assertEquals(((Double) Lua.GLOBAL_CONTEXT.run("assert(queryPacket() == nil, 'queryPacket() == nil')"
				+ "sendPacket(Random())"
				+ "local n = recievePacket():integerBetween(10, 11)"
				+ "disconnect()"
				+ "return n")[0]).intValue(), 10);
		server.stop();
		client.interrupt();
		serverThread.join();
		client.join();
	}

	@Test
	public void testServer() throws IOException, LuaException, InterruptedException {
		AbstractServer server = new AbstractServer(SERVER_PORT) {
			@Override
			public void handleNewConnection(ServerClient client) {
				Lua lua = new Lua();
				LuaUtil.install(lua, client);
				try {
					lua.run("sendPacket(recievePacket():integerBetween(10, 11))");
				} catch (LuaException ex) {
					fail(ex.getMessage());
				}
				client.interrupt();
			}
		};
		Thread serverThread = new Thread(server);
		serverThread.start();
		AbstractClient client = new AbstractClient(InetAddress.getLocalHost(), SERVER_PORT) {
		};
		client.start();

		client.write(new LuaRandom());
		assertEquals(((Double) client.waitForPacket()).intValue(), 10);
		client.write(DisconnectPacket.DISCONNECT);

		server.stop();
		serverThread.interrupt();
		client.interrupt();
		serverThread.join();
		client.join();
	}
}
