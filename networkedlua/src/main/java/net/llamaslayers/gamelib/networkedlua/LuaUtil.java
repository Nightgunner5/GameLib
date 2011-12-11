package net.llamaslayers.gamelib.networkedlua;

import java.io.Serializable;
import net.llamaslayers.gamelib.networking.AbstractClient;
import net.llamaslayers.gamelib.networking.DisconnectPacket;
import net.llamaslayers.gamelib.networking.ServerClient;
import net.llamaslayers.gamelib.scripting.Lua;
import se.krka.kahlua.integration.annotations.Desc;
import se.krka.kahlua.integration.annotations.LuaMethod;

public class LuaUtil {
	public synchronized static void install(AbstractClient client) {
		Lua.GLOBAL_CONTEXT.exposer.exposeGlobalFunctions(new LuaUtil(client));
	}
	public synchronized static void install(Lua lua, AbstractClient client) {
		lua.exposer.exposeGlobalFunctions(new LuaUtil(client));
	}
	public synchronized static void install(Lua lua, ServerClient client) {
		lua.exposer.exposeGlobalFunctions(new LuaUtil(client));
	}
	private final AbstractClient client;

	private LuaUtil(AbstractClient client) {
		this.client = client;
		this.server = null;
	}
	private final ServerClient server;

	private LuaUtil(ServerClient client) {
		this.client = null;
		this.server = client;
	}

	@LuaMethod(global = true)
	@Desc("Send an object to the other side, where it can be retrieved with recievePacket() or queryPacket()")
	public void sendPacket(@Desc("The object to send") Serializable object) {
		if (client == null) {
			server.write(object);
		} else {
			client.write(object);
		}
	}

	@LuaMethod(global = true)
	@Desc("Waits for an object from the other side, sent using sendPacket(). This function will not return until a packet has been recieved")
	public Serializable recievePacket() throws InterruptedException {
		if (client == null) {
			return server.waitForPacket();
		} else {
			return client.waitForPacket();
		}
	}

	@LuaMethod(global = true)
	@Desc("Grabs an object from the other side, sent using sendPacket(), but doesn't wait if there isn't one already waiting. Instead, this function will return nil")
	public Serializable queryPacket() throws InterruptedException {
		if (client == null) {
			return server.queryPacket();
		} else {
			return client.queryPacket();
		}
	}

	@LuaMethod(global = true)
	@Desc("Tells the other side that you will send no more packets and that it should stop disconnect after it recieves this one")
	public void disconnect() {
		sendPacket(DisconnectPacket.DISCONNECT);
	}
}
