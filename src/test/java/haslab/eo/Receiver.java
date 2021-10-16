package haslab.eo;

import java.net.UnknownHostException;

public class Receiver {
	// 1: one-way, 2: bidirectional not RPC
	private static final int COMTYPE2 = 2;
	private static final int PORT = 3456;
	private static int dstPort, iterations, len, warmUp = 1000, comType;
	private EOMiddleware eom;
	private NodeId dstNode;

	public Receiver(EOMiddleware eom, String dstHost, int dstPort) throws UnknownHostException {
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
		Receiver r1 = new Receiver(eom, dstHost, dstPort);
		r1.new Receiving().start();
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
			boolean started = false;
			try {
				// Warming up
				for (int i = 0; i < warmUp; i++) {
					eom.receive();
					if ((!started) && (comType == COMTYPE2)) {
						started = true;
						new Sending().start();
					}
				}
				long start = System.currentTimeMillis();
				for (int i = 0; i < iterations; i++) {
					eom.receive();
					// System.out.println(i);
				}
				long duration = System.currentTimeMillis() - start;
				double mps = iterations / (duration / 1000.0f);
				System.out.println("msg/s: " + mps + " throughput (B/s): " + mps * 1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
