package haslab.eo.msgs;

import haslab.eo.NodeId;

public class TokenMsg extends NetMsg {
	public final long s, r;
	public byte[] payload;

	public TokenMsg(NodeId node, long s, long r, byte[] payload) {
		super(node);
		this.s = s;
		this.r = r;
		this.payload = payload;
	}
}