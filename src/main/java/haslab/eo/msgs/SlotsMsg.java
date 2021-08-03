package haslab.eo.msgs;

import haslab.eo.NodeId;

public class SlotsMsg extends NetMsg {
	public final long s, r, n;

	public SlotsMsg(NodeId node, long s, long r, long n) {
		super(node);
		this.s = s;
		this.r = r;
		this.n = n;
	}
}