package haslab.eo.events;

import haslab.eo.NodeId;

public class AcksEvent extends Event {
	public long oldestAck;

	public AcksEvent(NodeId node, long oldestAck, long time) {
		super(node, time);
		this.oldestAck = oldestAck;
	}
}
