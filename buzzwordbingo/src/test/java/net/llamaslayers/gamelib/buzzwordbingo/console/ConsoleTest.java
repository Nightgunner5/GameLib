/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.llamaslayers.gamelib.buzzwordbingo.console;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author Nightgunner5
 */
public class ConsoleTest {
	public ConsoleTest() {
	}
	private Console console;

	@Before
	public void setUp() {
		console = new Console(null);
	}

	@After
	public void tearDown() {
		console = null;
	}

	@Test
	public void testJavaCommand() {
		final AtomicBoolean called = new AtomicBoolean(false);
		ConsoleCommand command = new ConsoleCommand() {
			@Override
			public String getName() {
				return "java";
			}

			@Override
			public void run(Console console, String... args) {
				assertNull(console.getClient());
				assertNotNull(console.getLua());
				assertArrayEquals(args, new String[] {"arg1", "arg2", "arg3"});
				called.set(true);
			}
		};
		assertTrue(console.register(command));
		assertTrue(console.execute("/java arg1 arg2 arg3"));
		assertTrue(called.get());
	}

	@Test
	public void testLuaCommand() {
		console.getLua().state.getEnvironment().rawset("called", false);
		ConsoleCommand command = new LuaConsoleCommand("lua",
				"assert(args[1] == 'arg1', 'arg1')"
				+ "assert(args[2] == 'arg2', 'arg2')"
				+ "assert(args[3] == 'arg3', 'arg3')"
				+ "called = true");
		assertTrue(console.register(command));
		assertTrue(console.execute("/lua arg1 arg2 arg3"));
		assertTrue((Boolean) console.getLua().state.getEnvironment().rawget("called"));
	}

	@Test
	public void testNonCommand() {
		assertFalse(console.execute("/what"));
	}

	@Test
	public void testInvalidCommand() {
		assertTrue(console.register(new LuaConsoleCommand("what", "")));
		assertFalse(console.execute("what"));
		assertFalse(console.execute(":what"));
	}

	@Test
	public void testRegisterInvalidCommand() {
		assertFalse(console.register(new LuaConsoleCommand("sp ace", "")));
	}

	@Test
	public void testUnregisterCommand() {
		ConsoleCommand what = new LuaConsoleCommand("what", "");
		assertTrue(console.register(what));
		assertTrue(console.execute("/what"));
		assertTrue(console.unregister(what));
		assertFalse(console.execute("/what"));
		assertFalse(console.unregister(what));
	}
}
