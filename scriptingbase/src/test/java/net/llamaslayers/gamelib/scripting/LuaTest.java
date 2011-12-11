package net.llamaslayers.gamelib.scripting;

import org.junit.*;
import static org.junit.Assert.*;

public class LuaTest {
	@Test
	public void testReturn() throws LuaException {
		assertEquals((Double) new Lua().run("return 1 + 2")[0], 3, 0.00001);
	}

	@Test
	public void testRandomInteger() throws LuaException {
		assertEquals((Double) new Lua().run("return Random():integerBetween(10, 11)")[0], 10, 0.00001);
	}

	@Test
	public void testSyntax() {
		try {
			new Lua().run("obviously invalid syntax");
			fail();
		} catch (LuaException ex) {

		}
	}
}
