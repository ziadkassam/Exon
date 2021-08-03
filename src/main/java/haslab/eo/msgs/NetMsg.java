package haslab.eo.msgs;

import java.io.Serializable;

import haslab.eo.NodeId;

public class NetMsg extends Msg implements Serializable {
	NodeId node;
	public NetMsg(NodeId node) {
		this.node = node;
	}

}
