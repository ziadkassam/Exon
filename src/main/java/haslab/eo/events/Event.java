package haslab.eo.events;

import haslab.eo.NodeId;

public abstract class Event {
	public long time;
	public NodeId node;

	public Event(NodeId node, long time) {
		this.node = node;
		this.time = time;
	}
}