package haslab.eo.events;

import haslab.eo.NodeId;

public class ReqSlotsEvent extends Event {

	public long lastReqSlotsSendTime;

	public ReqSlotsEvent(NodeId node, long time, long currentTime) {
		super(node, time);
		lastReqSlotsSendTime = currentTime;
	}
}