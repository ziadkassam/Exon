package haslab.eo.msgs;

import haslab.eo.NodeId;

public class MsgId {
	public final NodeId node;
	public final long clock;

	public MsgId(NodeId node, long clock) {
		this.node = node;
		this.clock = clock;
	}
}