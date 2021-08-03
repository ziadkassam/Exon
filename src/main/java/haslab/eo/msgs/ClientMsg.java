package haslab.eo.msgs;

import haslab.eo.NodeId;

public class ClientMsg extends Msg {
	public final NodeId node;
	public final byte[] msg;
	//final MsgId id;
	
	public ClientMsg(NodeId node, byte[] msg) {
		this.node = node;
		this.msg = msg;
	}
}