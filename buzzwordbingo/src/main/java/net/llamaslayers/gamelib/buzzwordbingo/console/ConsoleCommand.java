package net.llamaslayers.gamelib.buzzwordbingo.console;

public interface ConsoleCommand {
	public String getName();
	public void run(Console console, String... args);
}
