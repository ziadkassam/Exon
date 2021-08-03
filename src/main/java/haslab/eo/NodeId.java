package haslab.eo;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class NodeId implements Serializable {
	public final InetAddress addr;
	public final int port;

	public NodeId(String addr, int port) throws UnknownHostException {
		this.addr = InetAddress.getByName(addr);
		this.port = port;
	}

	@Override
	public int hashCode() {
		return Objects.hash(addr, port);
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (!(o instanceof NodeId))
			return false;
		NodeId other = (NodeId) o;
		return this.port == other.port && this.addr.equals(other.addr);
	}

}