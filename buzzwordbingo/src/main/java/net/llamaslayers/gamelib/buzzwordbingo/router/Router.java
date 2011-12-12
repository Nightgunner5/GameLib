package net.llamaslayers.gamelib.buzzwordbingo.router;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.llamaslayers.gamelib.networking.AbstractClient;
import net.llamaslayers.gamelib.networking.ServerClient;
import net.llamaslayers.gamelib.scripting.Lua;
import se.krka.kahlua.integration.annotations.*;
import se.krka.kahlua.vm.LuaClosure;

@LuaClass
public class Router extends Thread {
	private final Map<String, List<LuaClosure>> luaRoutes = new HashMap<String, List<LuaClosure>>();
	private final Map<String, List<PacketRoute>> javaRoutes = new HashMap<String, List<PacketRoute>>();
	private final Lua lua;
	private final AbstractClient client;
	private final ServerClient server;

	public Router(Lua lua, AbstractClient client) {
		this.lua = lua;
		lua.exposer.exposeClass(Router.class);
		lua.state.getEnvironment().rawset("router", this);
		this.client = client;
		this.server = null;
		start();
	}

	public Router(Lua lua, ServerClient client) {
		this.lua = lua;
		lua.exposer.exposeClass(Router.class);
		lua.state.getEnvironment().rawset("router", this);
		this.client = null;
		this.server = client;
		start();
	}

	@Override
	public void run() {
		if (client == null) {
			while (!server.finished()) {
				try {
					route(server.waitForPacket());
				} catch (InterruptedException ex) {
					break;
				}
			}
		} else {
			while (!client.finished()) {
				try {
					route(client.waitForPacket());
				} catch (InterruptedException ex) {
					break;
				}
			}
		}
	}

	private void route(Serializable packet) {
		if (packet instanceof RoutablePacket) {
			RoutablePacket rp = (RoutablePacket) packet;
			boolean found = false;
			if (luaRoutes.containsKey(rp.label)) {
				for (LuaClosure route : luaRoutes.get(rp.label))
					lua.state.pcall(route, new Object[] {rp.packet});
				found = true;
			}
			if (javaRoutes.containsKey(rp.label)) {
				for (PacketRoute route : javaRoutes.get(rp.label))
					route.route(rp.packet);
				found = true;
			}
			Logger.getLogger(Router.class.getCanonicalName()).log(Level.WARNING, "Route not found: {0}", rp.label);
		} else {
			Logger.getLogger(Router.class.getCanonicalName()).log(Level.SEVERE, "Non-Router packet detected: {0}", packet.getClass().getCanonicalName());
		}
	}

	@LuaMethod
	public void send(String label, Serializable packet) {
		if (client == null)
			server.write(new RoutablePacket(label, packet));
		else
			client.write(new RoutablePacket(label, packet));
	}

	@LuaMethod
	public void addRoute(String label, LuaClosure func) {
		add(luaRoutes, label, func);
	}

	public void addRoute(String label, PacketRoute func) {
		add(javaRoutes, label, func);
	}

	@LuaMethod
	public void removeRoute(String label, LuaClosure func) {
		remove(luaRoutes, label, func);
	}

	public void removeRoute(String label, PacketRoute func) {
		remove(javaRoutes, label, func);
	}

	private static <L> void add(Map<String, List<L>> list, String name, L listener) {
		synchronized (list) {
			if (!list.containsKey(name)) {
				list.put(name, new LinkedList<L>());
			}
			list.get(name).add(listener);
		}
	}

	private static <L> void remove(Map<String, List<L>> list, String name, L listener) {
		synchronized (list) {
			if (list.containsKey(name)) {
				list.get(name).remove(listener);
				if (list.get(name).isEmpty()) {
					list.remove(name);
				}
			}
		}
	}
}
