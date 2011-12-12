package net.llamaslayers.gamelib.networking;

import java.io.IOException;
import java.io.InputStream;

class NoCloseInputStream extends InputStream {
	private final InputStream in;

	public NoCloseInputStream(InputStream in) {
		this.in = in;
	}

	@Override
	public int read() throws IOException {
		return in.read();
	}

	@Override
	public void close() throws IOException {
		// NOPE
	}

}
