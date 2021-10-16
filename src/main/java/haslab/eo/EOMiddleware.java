package haslab.eo;

import java.util.concurrent.*;

import haslab.eo.events.*;
import haslab.eo.msgs.*;
import java.util.HashMap;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.nio.ByteBuffer;

public class EOMiddleware {
	private BlockingQueue<AQMsg> algoQueue = new ArrayBlockingQueue<AQMsg>(1000000);
	private BlockingQueue<DQMsg> deliveryQueue = new ArrayBlockingQueue<DQMsg>(1000000);
	private ConcurrentHashMap<NodeId, SendRecord> sr = new ConcurrentHashMap<NodeId, SendRecord>();
	private DatagramSocket sk;
	int N, P;
	private final int maxAcks = 1;
	private ByteBuffer bb;
	private final int MTUSize = 1400;
	private final int REQSLOT = 1, SLOT = 2, TOKEN = 3, ACK = 4;
	private byte[] outData;
	private boolean sendFirstTime = true, receiveFirstTime = true;
	// for P calculations
	private int tcpPort = 12121;
	private int bandwidthIterations = 1000000;
	private int rttIterations = 10000;
	private int leng = 1024;
	private int N_Multiplier = 4;

	private EOMiddleware(int port) throws SocketException {
		sk = new DatagramSocket(port);
		sk.setReceiveBufferSize(2000000000);
		System.out.println("UDP DatagramSocket Created: " + port);
		bb = ByteBuffer.allocate(MTUSize);
		outData = new byte[MTUSize];
	}

	public static EOMiddleware start(int port) throws SocketException {
		EOMiddleware eo = new EOMiddleware(port);
		eo.new AlgoThread().start();
		eo.new ReaderThread().start();
		return eo;
	}

	public MsgId send(NodeId node, byte[] msg) throws InterruptedException, IOException {
		if (sendFirstTime) {
			sendFirstTime = false;
			P = calculatePSender(node);
			// P = 120;
			N = P * N_Multiplier;
			System.out.println("P= " + P + ", N=" + N);
			System.out.println("----------------------------------- \n");
		}
		SendRecord c = sr.get(node);
		if (c != null)
			c.sem.acquire();

		AQMsg aqm = new AQMsg(node, new ClientMsg(node, msg));
		algoQueue.put(aqm);
		return null; // so far
	}

	private boolean netSend(NodeId node, NetMsg m) throws IOException, InterruptedException {

		bb = ByteBuffer.wrap(outData);
		if (m instanceof ReqSlotsMsg) {
			ReqSlotsMsg rsm = (ReqSlotsMsg) m;
			bb.putInt(REQSLOT).putLong(rsm.s).putLong(rsm.n).putLong(rsm.l).putDouble(rsm.RTT);
		} else if (m instanceof SlotsMsg) {
			SlotsMsg sm = (SlotsMsg) m;
			bb.putInt(SLOT).putLong(sm.s).putLong(sm.r).putLong(sm.n);
		} else if (m instanceof TokenMsg) {
			TokenMsg tm = (TokenMsg) m;
			bb.putInt(TOKEN).putLong(tm.s).putLong(tm.r).put(tm.payload);
		} else if (m instanceof AcksMsg) {
			AcksMsg am = (AcksMsg) m;
			bb.putInt(ACK);
			bb.putLong(am.r);
			for (int i = 0; i < am.acks.size(); i++)
				bb.putLong(am.acks.get(i));
		}
		DatagramPacket sendPacket = new DatagramPacket(outData, bb.position(), node.addr, node.port);
		sk.send(sendPacket);
		return true;
	}

	public ClientMsg receive() throws InterruptedException {
		if (receiveFirstTime) {
			receiveFirstTime = false;
			try {
				P = calculatePReceiver();
				N = P * N_Multiplier;
				System.out.println("P= " + P + ", N=" + N);
				System.out.println("----------------------------------- \n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		DQMsg m = deliveryQueue.take();
		return new ClientMsg(m.node, m.msg);
	}

	// public Msg receive(long timeout){
	// return null;
	// }

	class AlgoThread extends Thread {
		private long ck = 0;
		private NodeId j;
		private HashMap<NodeId, ReceiveRecord> rr = new HashMap<NodeId, ReceiveRecord>();
		private PriorityQueue<Event> pq = new PriorityQueue<Event>(100000, new TimeComparator());
		private long timeout, currentTime;
		private int slotsTimeout = 50000;
		final double ALPHA = 0.2, BETA = 2;
		final double LBOUND = 100, UBOUND = 1000;
		int retransmit = 0;
		double receiverRTT;
		final double reqSlotsMultiplier = 1.5;
		final double tokenMultiplier = 2;
		final double acksMultiplier = 0.25;

		public void run() {
			try {
				currentTime = System.currentTimeMillis();
				while (true) {
					Event eve = pq.peek();
					if (eve == null)
						timeout = Long.MAX_VALUE;
					else
						timeout = eve.time - currentTime;

					AQMsg m = (AQMsg) algoQueue.poll(timeout, TimeUnit.MILLISECONDS);
					currentTime = System.currentTimeMillis();

					if (m != null) {
						j = m.node;

						if (m.msg instanceof ClientMsg) {// Client message received
							ClientMsg eom = (ClientMsg) m.msg;
							byte[] msg = eom.msg;
							SendRecord c = sr.get(j);
							if (c == null) {
								SendRecord s = new SendRecord(ck, 0, msg);
								s.sem = new Semaphore(P);
								sr.put(j, s);
								requestSlots(j);
							} else {
								if (c.envelopes.size() != 0) {
									long e = c.envelopes.dequeue();
									if (c.envelopes.size() == (N - 1))
										requestSlots(j);
									TokenMsg tm = new TokenMsg(j, e, c.rck, msg);
									TokenRecord tr = new TokenRecord(j, e, c.rck, msg, currentTime);
									c.tok.put(e, tr);
									pq.add(new TokenEvent(j, tr, msgTimeout(currentTime, c.RTT, tokenMultiplier)));
									netSend(j, tm);
								} else
									c.msg.add(msg);
							}
						} else if (m.msg instanceof ReqSlotsMsg) {// ReqSlots Received
							ReqSlotsMsg rm = (ReqSlotsMsg) m.msg;
							long s = rm.s;
							long n = rm.n;
							long l = rm.l;
							receiverRTT = rm.RTT;

							ReceiveRecord c = rr.get(j);
							if (c == null) {
								c = new ReceiveRecord(s, ck);
								rr.put(j, c);
								ck += 1;
								pq.add(new SlotsEvent(j, currentTime + slotsTimeout, currentTime));
							}

							if (n > 0) {
								if ((s + n) > c.sck) {
									c.slt.extendTo(s + n);
									c.sck = s + n;
								}
								c.lastSlotsSendTime = currentTime;
								netSend(j, new SlotsMsg(j, s, c.rck, n));
							} else {
								c.slt.removeSmallerThan(l);
							}

							if (c.slt.size() == 0)
								rr.remove(j);

						} else if (m.msg instanceof SlotsMsg) {// Slots Received
							SlotsMsg rm = (SlotsMsg) m.msg;
							long s = rm.s;
							long r = rm.r;
							long n = rm.n;
							SendRecord c = sr.get(j);

							if (c == null) {
								netSend(j, new ReqSlotsMsg(j, ck, 0, ck, 0));
							} else if (s == c.sck) {
								// calculating RTT
								long newRTT = currentTime - c.reqSlotsTime;
								c.RTT = (ALPHA * c.RTT) + ((1 - ALPHA) * newRTT);
								c.rck = r;
								c.envelopes.append(s + n);
								c.sck = s + n;
								while ((c.envelopes.size() != 0) && (c.msg.size() != 0)) {
									long e = c.envelopes.dequeue();
									byte[] msg = c.msg.poll();
									TokenMsg tm = new TokenMsg(j, e, c.rck, msg);
									TokenRecord tr = new TokenRecord(j, e, c.rck, msg, currentTime);
									c.tok.put(e, tr);
									pq.add(new TokenEvent(j, tr, msgTimeout(currentTime, c.RTT, tokenMultiplier)));
									netSend(j, tm);
								}
								requestSlots(j);
							}
						} else if (m.msg instanceof TokenMsg) {// Token Received
							TokenMsg rm = (TokenMsg) m.msg;
							long s = rm.s;
							long r = rm.r;
							byte[] msg = rm.payload;
							ReceiveRecord c = rr.get(j);
							if ((c != null) && (r == c.rck)) {
								if (c.slt.contains(s)) {
									if (deliveryQueue.offer(new DQMsg(j, msg))) { // deliver(msg)
										c.slt.remove(s);
										sendAck(j, c, s, r, msgTimeout(currentTime, receiverRTT, acksMultiplier));
									}
								} else {
									sendAck(j, c, s, r, msgTimeout(currentTime, receiverRTT, acksMultiplier));
								}
							}
						} else if (m.msg instanceof AcksMsg) {// Ack Received
							AcksMsg rm = (AcksMsg) m.msg;
							ArrayList<Long> acks = rm.acks;
							long r = rm.r;
							SendRecord c = sr.get(j);
							if ((c != null) && (r == c.rck)) {
								for (int i = 0; i < acks.size(); i++) {
									TokenRecord tr = c.tok.get(acks.get(i));
									if (tr != null) {
										c.tok.remove(acks.get(i));
										c.sem.release();
										tr.acked = true;
									}
								}
							}
						}
					}
					// Periodically
					while (true) {
						eve = pq.peek();
						if (eve == null || eve.time > currentTime)
							break;

						pq.poll();
						NodeId j = eve.node;

						if (eve instanceof ReqSlotsEvent) {
							ReqSlotsEvent rse = (ReqSlotsEvent) eve;
							SendRecord c = sr.get(j);
							if (c != null)
								if (c.reqSlotsTime == rse.lastReqSlotsSendTime)
									requestSlots(j);
						} else if (eve instanceof SlotsEvent) {
							SlotsEvent se = (SlotsEvent) eve;
							ReceiveRecord c = rr.get(j);
							if (c != null) {
								if (se.lastSlotsSendTime != c.lastSlotsSendTime) {
									c.lastSlotsSendTime = currentTime;
									netSend(j, new SlotsMsg(j, c.sck, c.rck, 0));
								}
								pq.add(new SlotsEvent(j, currentTime + slotsTimeout, c.lastSlotsSendTime));
							}
						} else if (eve instanceof TokenEvent) {
							TokenEvent te = (TokenEvent) eve;
							TokenRecord tr = te.t;
							if (!tr.acked) {
								SendRecord c = sr.get(j);
								if (c != null) {
									if ((c.rck == tr.r) && (c.tok.containsKey(tr.s))) {
										System.out.println("Re-transmitting: " + retransmit++);
										pq.add(new TokenEvent(j, tr,
												msgTimeout(currentTime, c.RTT, tokenMultiplier * 3)));
										netSend(j, new TokenMsg(j, tr.s, tr.r, tr.m));
									}
								}
							}
						} else if (eve instanceof AcksEvent) {
							AcksEvent ae = (AcksEvent) eve;
							ReceiveRecord c = rr.get(j);
							if (c != null) {
								if ((c.oldestAck == ae.oldestAck) && (!c.acks.isEmpty())) {
									netSend(j, new AcksMsg(j, c.acks, c.rck));
									c.acks.clear();
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public void requestSlots(NodeId j) {
			SendRecord c = sr.get(j);
			long n = N + c.msg.size() - c.envelopes.size();

			try {
				if (n > 0) {
					long e;
					if (c.tok.size() != 0)
						e = c.tok.firstKey();
					else if (c.envelopes.size() != 0)
						e = c.envelopes.first();
					else
						e = c.sck;

					c.reqSlotsTime = currentTime;
					pq.add(new ReqSlotsEvent(j, msgTimeout(currentTime, c.RTT, reqSlotsMultiplier), currentTime));
					netSend(j, new ReqSlotsMsg(j, c.sck, n, e, c.RTT));
				} else if (c.tok.size() == 0 && c.msg.size() == 0) {
					netSend(j, new ReqSlotsMsg(j, c.sck, 0, c.sck, c.RTT));
					ck = Math.max(ck, c.sck);
					sr.remove(j);
				}
			} catch (Exception ee) {
				ee.printStackTrace();
			}
		}

		public long msgTimeout(long currentTime, double RTT, double multiplier) {
			return (long) Math.min(UBOUND, Math.max(LBOUND, (multiplier * RTT))) + currentTime;
		}

		public void sendAck(NodeId j, ReceiveRecord c, long s, long r, long acksTimeout)
				throws IOException, InterruptedException {
			if (c != null) {
				if (c.acks.isEmpty()) {
					pq.add(new AcksEvent(j, currentTime, acksTimeout));
					c.oldestAck = currentTime;
				}
				c.acks.add(s);
				if (c.acks.size() == maxAcks) {
					netSend(j, new AcksMsg(j, c.acks, r));
					c.acks.clear();
				}
			} else {
				ArrayList<Long> ack = new ArrayList<Long>();
				ack.add(s);
				netSend(j, new AcksMsg(j, ack, r));
			}
		}
	}

	class ReaderThread extends Thread {
		public void run() {
			ByteBuffer b = ByteBuffer.allocate(MTUSize);
			byte[] incomingData = new byte[MTUSize];
			try {
				while (true) {
					Msg m = null;
					DatagramPacket in_pkt = new DatagramPacket(incomingData, incomingData.length);
					sk.receive(in_pkt);
					b = ByteBuffer.wrap(incomingData, 0, in_pkt.getLength());
					int msgType = b.getInt();
					NodeId node = new NodeId(in_pkt.getAddress().getHostAddress(), in_pkt.getPort());

					if (msgType == REQSLOT)
						m = new ReqSlotsMsg(node, b.getLong(), b.getLong(), b.getLong(), b.getDouble());
					else if (msgType == SLOT)
						m = new SlotsMsg(node, b.getLong(), b.getLong(), b.getLong());
					else if (msgType == TOKEN) {
						long s = b.getLong();
						long r = b.getLong();
						byte[] payload = new byte[b.remaining()];
						b.get(payload);
						m = new TokenMsg(node, s, r, payload);
					} else if (msgType == ACK) {
						ArrayList<Long> acks = new ArrayList<Long>();
						long r = b.getLong();
						int numAcks = b.remaining() / 8;
						for (int i = 0; i < numAcks; i++)
							acks.add(b.getLong());
						m = new AcksMsg(node, acks, r);
					}
					AQMsg aqm = new AQMsg(node, m);
					algoQueue.put(aqm);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public int calculatePReceiver() throws IOException, InterruptedException {
		int p = 0;
		try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
			System.out.println("Testing the network...");
			Socket socket = serverSocket.accept();
			InputStream input = socket.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output, true);

			// Calculating RTT
			for (int i = 0; i < rttIterations; i++) {
				String m = reader.readLine();
				writer.println(m);
			}
			double TCP_RTT = Double.parseDouble(reader.readLine());// receive TCP_RTT

			// Calculating bandwidth, and then P
			long start = System.currentTimeMillis();
			for (int i = 0; i < bandwidthIterations; i++) {
				reader.readLine();
			}
			long duration = System.currentTimeMillis() - start;
			double mps = bandwidthIterations / (duration / 1000.0f);
			double bandwidth = mps * leng * 8 / 1000000;
			System.out.println("Bandwidth: " + bandwidth + ", mps: " + mps + ", TCP_RTT: " + TCP_RTT);
			p = (int) ((bandwidth * 1000000 / 8) * (TCP_RTT / 1000)) / leng;
			writer.println(p);
		} catch (IOException ex) {
			System.out.println("Server exception: " + ex.getMessage());
			ex.printStackTrace();
		}
		return p;
	}

	public int calculatePSender(NodeId node) {
		String m = new String(new char[leng]).replace('\0', ' ');
		int p = 0;

		try (Socket socket = new Socket(node.addr.getHostAddress(), tcpPort)) {
			System.out.println("Testing the network...");
			OutputStream output = socket.getOutputStream();
			PrintWriter writer = new PrintWriter(output, true);
			InputStream input = socket.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));

			// Calculating RTT
			long start = System.currentTimeMillis();
			for (int i = 0; i < rttIterations; i++) {
				writer.println(" ");
				reader.readLine();
			}
			long duration = System.currentTimeMillis() - start;
			double tcpRTT = ((double) duration) / ((double) rttIterations);
			writer.println(tcpRTT); // send TCP_RTT

			// Calculating bandwidth, and then P
			for (int i = 0; i < bandwidthIterations; i++) {
				writer.println(m);
			}
			p = Integer.parseInt(reader.readLine());
		} catch (UnknownHostException ex) {
			System.out.println("Server not found: " + ex.getMessage());
		} catch (IOException ex) {
			System.out.println("I/O error: " + ex.getMessage());
		}
		return p;
	}
}

class TimeComparator implements Comparator<Event> {
	public int compare(Event e1, Event e2) {
		return (int) (e1.time - e2.time);
	}
}
