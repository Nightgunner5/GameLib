package net.llamaslayers.gamelib.buzzwordbingo.router;

import java.io.Serializable;

public interface PacketRoute {
	public void route(Serializable packet);
}
