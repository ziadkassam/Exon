package haslab.eo;

import haslab.eo.msgs.ClientMsg;

public class Server {
	private static final int PORT = 3456;

	public static void main(String[] args) throws Exception {
		String dstHost = args[0];
		int dstPort = Integer.parseInt(args[1]);
		int P = Integer.parseInt(args[2]);

		EOMiddleware eom = EOMiddleware.start(PORT, P);
		NodeId dstNode = new NodeId(dstHost, dstPort);

		while (true) {
			ClientMsg request = eom.receive();
			eom.send(dstNode, request.msg);
		}
	}
}