package haslab.eo.events;

import haslab.eo.NodeId;
import haslab.eo.TokenRecord;

public final class TokenEvent extends Event {
	public final TokenRecord t;

	public TokenEvent(NodeId node, TokenRecord t, long time) {
		super(node, time);
		this.t = t;
	}

	public TokenRecord getT() {
		return t;
	}
	
}