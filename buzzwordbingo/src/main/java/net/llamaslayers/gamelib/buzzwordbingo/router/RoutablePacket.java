package net.llamaslayers.gamelib.buzzwordbingo.router;

import java.io.Serializable;

class RoutablePacket implements Serializable {
	private static final long serialVersionUID = 1L;
	public final String label;
	public final Serializable packet;

	public RoutablePacket(String label, Serializable packet) {
		this.label = label;
		this.packet = packet;
	}
}
