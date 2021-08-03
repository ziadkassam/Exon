package haslab.eo;

import java.util.ArrayList;
import io.github.pssalmeida.slidingbitmap.SlidingBitmap;

class ReceiveRecord {
	long sck, rck;
	long lastSlotsSendTime, oldestAck;
	ArrayList<Long> acks = new ArrayList<Long>();
	SlidingBitmap slt = new SlidingBitmap(1000000);

	public ReceiveRecord(long sck, long rck) {
		this.sck = sck;
		this.rck = rck;
	}

	public String toString() {
		return "Receive Record: sck: " + sck + ", rck: " + rck;
	}
}