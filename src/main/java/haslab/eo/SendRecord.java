package haslab.eo;

import java.util.*;
import java.util.concurrent.*;

class SendRecord {
	Semaphore sem;
	long sck, rck;
	Queue<byte[]> msg = new LinkedList<byte[]>();
	Interval envelopes;
	final TreeMap<Long, TokenRecord> tok = new TreeMap<Long, TokenRecord>();
	long reqSlotsTime;
	double RTT = 10;

	public SendRecord(long sck, long rck, byte[] m) throws Exception {
		this.sck = sck;
		this.rck = rck;
		msg.add(m);
		envelopes = new Interval(0, 0);
	}

	public String toString() {
		return "sck: " + sck + ", rck: " + rck + ", msg: " + new String(msg.peek());
	}
}