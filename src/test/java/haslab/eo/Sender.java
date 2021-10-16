package haslab.eo;

import java.net.UnknownHostException;

public class Sender {
	// 1: one-way, 2: bidirectional not RPC
	private static final int COMTYPE2 = 2;
	private static final int PORT = 1234;
	private static int dstPort, iterations, len, warmUp = 1000, comType;
	private EOMiddleware eom;
	private NodeId dstNode;

	public Sender(EOMiddleware eom, String dstHost, int dstPort) throws UnknownHostException {
		this.eom = eom;
		this.dstNode = new NodeId(dstHost, dstPort);
	}

	public static void main(String[] args) throws Exception {

		String dstHost = args[0];
		dstPort = Integer.parseInt(args[1]);
		iterations = Integer.parseInt(args[2]);
		len = Integer.parseInt(args[3]);
		comType = Integer.parseInt(args[4]);

		EOMiddleware eom = EOMiddleware.start(PORT);
		Sender s1 = new Sender(eom, dstHost, dstPort);
		s1.new Sending().start();
		if (comType == COMTYPE2)
			s1.new Receiving().start();

	}

	class Sending extends Thread {
		public void run() {
			try {
				String m = new String(new char[len]).replace('\0', ' ');
				// Warming up
				for (int i = 0; i < warmUp; i++) {
					eom.send(dstNode, m.getBytes());
				}
				for (int i = 0; i < iterations; i++) {
					eom.send(dstNode, m.getBytes());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	class Receiving extends Thread {
		public void run() {
			try {
				// Warming up
				for (int i = 0; i < warmUp; i++) {
					eom.receive();
				}
				long start = System.currentTimeMillis();
				for (int i = 0; i < iterations; i++) {
					eom.receive();
				}
				long duration = System.currentTimeMillis() - start;
				float mps = iterations / (duration / 1000.0f);
				System.out.println("msg/s: " + mps + " throughput (B/s): " + mps * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
