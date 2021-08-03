package haslab.eo;

import haslab.eo.msgs.ClientMsg;

public class Client {
	private static final int PORT = 1234;

	public static void main(String[] args) throws Exception {
		String dstHost = args[0];
		int dstPort = Integer.parseInt(args[1]);
		int P = Integer.parseInt(args[2]);
		int iterations = Integer.parseInt(args[3]);
		int len = Integer.parseInt(args[4]);
		int actors = Integer.parseInt(args[5]);
		
		int reqSnt[] = new int[actors];
		int repRcv[] = new int[actors];
		int finished = 0;

		String m = new String(new char[len]).replace('\0', ' ');

		for (int i = 0; i < actors; i++) {
			reqSnt[i] = 0;
			repRcv[i] = 0;
		}

		EOMiddleware eom = EOMiddleware.start(PORT, P);
		NodeId dstNode = new NodeId(dstHost, dstPort);

		long start = System.nanoTime();
		for (int i = 0; i < actors; i++) {
			String request = i + "-" + m;
			eom.send(dstNode, request.getBytes());
			reqSnt[i]++;
		}

		for (int i = 0; i < iterations; i++) {
			ClientMsg reply = eom.receive();
			String[] arrOfStr = new String(reply.msg).split("-");
			int id = Integer.parseInt(arrOfStr[0]);
   
			repRcv[id]++;

			if (repRcv[id] == (iterations / actors)) {
				finished++;
				if (finished == actors)
					break;
			} else {
				String request = id + "-" + m;
				eom.send(dstNode, request.getBytes());
				reqSnt[id]++;
			}
		}
		long duration = System.nanoTime() - start;
		float rps = iterations / (duration / 1000000000.0f);
		//float micros = (duration / 1000) / (iterations / actors);
		float micros = (actors * (duration / 1000)) / iterations;
		System.out.println("msg size: " + len + " rps: " + rps + " latency (microsec): " + micros);
	}
}
