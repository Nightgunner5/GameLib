package net.llamaslayers.gamelib.scripting;

public class LuaException extends Exception {
	private static final long serialVersionUID = 1L;

	public LuaException(String message, Throwable cause) {
		super(message, cause);
	}
}
