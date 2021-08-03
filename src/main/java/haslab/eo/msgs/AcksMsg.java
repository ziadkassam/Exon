package haslab.eo.msgs;

import java.util.ArrayList;

import haslab.eo.NodeId;

public class AcksMsg extends NetMsg {
	public final long r;
	public final ArrayList<Long> acks; 
	
	public AcksMsg(NodeId node, ArrayList<Long> acks, long r) {
		super(node);
		this.acks = acks;
		this.r = r;
	}
}