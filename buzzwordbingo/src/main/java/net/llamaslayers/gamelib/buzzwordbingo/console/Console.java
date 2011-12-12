package net.llamaslayers.gamelib.buzzwordbingo.console;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.llamaslayers.gamelib.networkedlua.LuaUtil;
import net.llamaslayers.gamelib.networking.AbstractClient;
import net.llamaslayers.gamelib.scripting.Lua;
import se.krka.kahlua.integration.annotations.*;

@LuaClass
public final class Console {
	private final AbstractClient client;
	private final Lua lua;
	private final Map<String, ConsoleCommand> commands = new ConcurrentHashMap<String, ConsoleCommand>();
	private volatile ConsoleCommand currentCommand;
	private final StringBuilder log = new StringBuilder();
	private final List<ConsoleListener> listeners = new LinkedList<ConsoleListener>();
	public Console(AbstractClient client) {
		this.client = client;
		lua = new Lua();
		LuaUtil.install(lua, client);
		lua.exposer.exposeClass(Console.class);
		lua.state.getEnvironment().rawset("console", this);
	}

	public AbstractClient getClient() {
		return client;
	}

	public Lua getLua() {
		return lua;
	}

	public boolean register(ConsoleCommand command) {
		if (command.getName().contains(" "))
			return false;
		commands.put(command.getName().toLowerCase(), command);
		return true;
	}

	/**
	 * Unregister a console command
	 * @param command the command to unregister
	 * @return true if the command was unregistered, false if it was not registered initially
	 */
	public boolean unregister(ConsoleCommand command) {
		return commands.remove(command.getName().toLowerCase()) != null;
	}

	public ConsoleCommand findCommand(String line) {
		if (line.length() < 2)
			return null;
		char firstChar = line.charAt(0);
		if (firstChar != '/')
			return null;
		String name = line.substring(1);
		if (name.contains(" "))
			name = name.substring(0, name.indexOf(' '));
		return commands.get(name.toLowerCase());
	}

	public boolean execute(String line) {
		ConsoleCommand command = findCommand(line);
		if (command == null)
			return false;

		String[] args = line.split(" ");
		args = Arrays.copyOfRange(args, 1, args.length);
		currentCommand = command;
		command.run(this, args);
		currentCommand = null;
		return true;
	}

	@LuaMethod
	public void log(String text) {
		if (currentCommand != null)
			text = "[" + currentCommand.getName() + "] " + text;

		log.append(text).append('\n');
		for (ConsoleListener listener : listeners) {
			listener.log(text);
		}
	}

	public String getLog() {
		return log.toString();
	}

	public void clearLog() {
		log.setLength(0);
	}
}
