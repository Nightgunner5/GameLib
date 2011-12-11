package net.llamaslayers.gamelib.scripting;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import se.krka.kahlua.integration.annotations.*;

@LuaClass
public class LuaRandom implements Serializable {
	private static final long serialVersionUID = 1L;
	private static final Random base = new Random();
	private final Random internal;

	@LuaConstructor(name = "Random")
	@Desc("Create a new random number generator")
	public LuaRandom() {
		internal = new Random(base.nextLong());
	}

	private LuaRandom(Random internal) {
		this.internal = internal;
	}

	@LuaMethod(name = "integer")
	@Desc("Get a random integer greater than or equal to zero and less than the given number")
	public int nextInt(@Desc("The result will be less than this number") int max) {
		return internal.nextInt(max);
	}

	@LuaMethod(name = "integerBetween")
	@Desc(
	"Get a random integer greater than or equal to a given number and less than another given number")
	public int nextIntBetween(@Desc("The result will be greater than or equal to this number") int min,
							  @Desc("The result will be less than this number") int max) {
		return internal.nextInt(max - min) + min;
	}

	@LuaMethod(name = "real")
	@Desc("Get a random real number greater than or equal to zero and less than one")
	public double nextReal() {
		return internal.nextDouble();
	}

	@LuaMethod(name = "realBetween")
	@Desc("Get a random real number greater than or equal to a given number and less than another given number")
	public double nextIntBetween(@Desc("The result will be greater than or equal to this number") double min,
								 @Desc("The result will be less than this number") double max) {
		return internal.nextDouble() * (max - min) + min;
	}

	private static final Field SEED;
	static {
		try {
			SEED = Random.class.getDeclaredField("seed");
			SEED.setAccessible(true);
		} catch (NoSuchFieldException ex) {
			throw new RuntimeException(ex);
		} catch (SecurityException ex) {
			throw new RuntimeException(ex);
		}
	}
	@LuaMethod(name = "clone")
	public LuaRandom getClone() {
		try {
			return new LuaRandom(new Random(((AtomicLong) SEED.get(internal)).get() ^ 0x5DEECE66DL));
		} catch (IllegalArgumentException ex) {
			throw new RuntimeException(ex);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} catch (SecurityException ex) {
			throw new RuntimeException(ex);
		}
	}
}
