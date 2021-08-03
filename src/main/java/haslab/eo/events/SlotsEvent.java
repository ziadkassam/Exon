package haslab.eo.events;

import haslab.eo.NodeId;

public class SlotsEvent extends Event {
	public long lastSlotsSendTime;
	
	public SlotsEvent(NodeId node, long time, long lastSlotsSendTime) {
		super(node, time);
		this.lastSlotsSendTime = lastSlotsSendTime;
	}
}