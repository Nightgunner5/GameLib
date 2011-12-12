package net.llamaslayers.gamelib.buzzwordbingo.console;

import java.util.Arrays;
import net.llamaslayers.gamelib.scripting.LuaException;
import se.krka.kahlua.converter.LuaConversionError;

public final class LuaConsoleCommand implements ConsoleCommand {
	private final String name;
	private final String source;

	public LuaConsoleCommand(String name, String source) {
		this.name = name;
		this.source = source;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void run(Console console, String... args) {
		try {
			console.getLua().state.getEnvironment().rawset("args", console.getLua().manager.fromJavaToLua(Arrays.asList(args)));
		} catch (LuaConversionError ex) {
			throw new RuntimeException(ex);
		}
		try {
			console.getLua().run(source);
		} catch (LuaException ex) {
			throw new RuntimeException(ex);
		}
	}
}
