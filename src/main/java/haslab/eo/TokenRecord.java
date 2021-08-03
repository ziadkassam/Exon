package haslab.eo;

public class TokenRecord {
	long s, r, time;
	byte[] m;
	NodeId node;
	boolean acked;

	public TokenRecord(NodeId node, long s, long r, byte[] m, long time) {
		this.node = node;
		this.s = s;
		this.r = r;
		this.m = m;
		this.time = time;
		acked = false;
	}
}