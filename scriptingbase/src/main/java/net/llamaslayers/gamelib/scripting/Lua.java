package net.llamaslayers.gamelib.scripting;

import java.io.IOException;
import java.util.Arrays;
import se.krka.kahlua.converter.*;
import se.krka.kahlua.integration.expose.LuaJavaClassExposer;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.threading.ThreadSafeLuaState;
import se.krka.kahlua.vm.LuaClosure;

public class Lua {
	public static final ThreadSafeLuaState state;
	public static final LuaConverterManager manager;
	public static final LuaJavaClassExposer exposer;
	static {
		state = new ThreadSafeLuaState(System.out);
		manager = new LuaConverterManager();
		LuaNumberConverter.install(manager);
		LuaTableConverter.install(manager);
		exposer = new LuaJavaClassExposer(state, manager);

		exposer.exposeClass(LuaRandom.class);
		exposer.exposeClass(LuaSimplex.class);
	}

	public static Object[] run(String source) throws LuaException {
		try {
			LuaClosure closure = LuaCompiler.loadstring(source, "", state.getEnvironment());
			Object[] result = state.pcall(closure);
			if ((Boolean) result[0])
				return Arrays.copyOfRange(result, 1, result.length);
			String message = concatStrings(result);
			throw new LuaException(message + source.split("\n")[Integer.parseInt(message.substring(message.lastIndexOf("@:") + 2, message.lastIndexOf('\n'))) - 1], getThrowable(result));
		} catch (IOException ex) {
			throw new RuntimeException("The universe seems to have imploded.", ex);
		}
	}

	private static Throwable getThrowable(Object[] candidates) {
		for (Object candidate : candidates)
			if (candidate instanceof Throwable)
				return (Throwable) candidate;
		return null;
	}

	private static String concatStrings(Object[] candidates) {
		boolean first = true;
		StringBuilder sb = new StringBuilder();
		for (Object candidate : candidates) {
			if (candidate instanceof String) {
				if (!first)
					sb.append(' ');
				first = false;
				sb.append((String) candidate);
			}
		}
		return sb.toString();
	}
}
