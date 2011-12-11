package net.llamaslayers.gamelib.networking;

import java.io.Serializable;

public final class DisconnectPacket implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final DisconnectPacket DISCONNECT = new DisconnectPacket(false);
	static final DisconnectPacket DISCONNECT_ACK = new DisconnectPacket(true);

	final boolean response;
	private DisconnectPacket(boolean response) {this.response = response;}
}
