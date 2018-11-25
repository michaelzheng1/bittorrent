package main;

import java.net.InetAddress;

public class IPAndPort {
	InetAddress ip;
	int port;
	
	public IPAndPort(InetAddress ip, short port) {
		this.ip = ip;
		this.port = port < 0 ? Short.MAX_VALUE + port : port;
	}
	
	public String toString() {
		return String.format("%s:%d", this.ip.getHostAddress(), this.port);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IPAndPort other = (IPAndPort) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
}
