package haslab.eo.msgs;

import haslab.eo.NodeId;

public class DQMsg {
	public NodeId node;
	public byte[] msg;

	public DQMsg(NodeId node, byte[] msg) {
		this.node = node;
		this.msg = msg;
	}
}