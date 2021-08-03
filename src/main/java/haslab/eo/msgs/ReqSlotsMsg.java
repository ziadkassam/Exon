package haslab.eo.msgs;

import haslab.eo.NodeId;

public class ReqSlotsMsg extends NetMsg {
	public final long s, n, l;
	public double RTT;

	public ReqSlotsMsg(NodeId node, long s, long n, long l, double RTT) {
		super(node);
		this.s = s;
		this.n = n;
		this.l = l;
		this.RTT = RTT;
	}
}