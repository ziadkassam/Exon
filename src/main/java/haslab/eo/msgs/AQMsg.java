package haslab.eo.msgs;

import haslab.eo.NodeId;

public class AQMsg {
	public final NodeId node;
	public final Msg msg;

	public AQMsg(NodeId node, Msg msg) {
		this.node = node;
		this.msg = msg;
	}
}