package net.llamaslayers.gamelib.scripting;

import junit.framework.TestCase;

public class LuaTest extends TestCase {
	public LuaTest(String testName) {
		super(testName);
	}

	public void testReturn() throws LuaException {
		assertEquals((Double) Lua.run("return 1 + 2")[0], 3, 0.00001);
	}

	public void testRandomInteger() throws LuaException {
		assertEquals((Double) Lua.run("return Random():integerBetween(10, 11)")[0], 10, 0.00001);
	}

	public void testSyntax() {
		try {
			Lua.run("obviously invalid syntax");
			fail();
		} catch (LuaException ex) {

		}
	}
}
