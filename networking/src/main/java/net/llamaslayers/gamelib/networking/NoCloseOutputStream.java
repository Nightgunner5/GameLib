package net.llamaslayers.gamelib.networking;

import java.io.IOException;
import java.io.OutputStream;

class NoCloseOutputStream extends OutputStream {
	private final OutputStream out;

	public NoCloseOutputStream(OutputStream out) {
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}

	@Override
	public void close() throws IOException {
		// NOPE
	}

}
