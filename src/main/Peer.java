package main;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

public class Peer {
/**
 * Follow the specification, define the below class variable.
 */
	boolean choked, interested;
	InetAddress ip;
	int port;
	TrackerInfo tracker;
	InputStream in;
	OutputStream out;
	Socket client;
	String log_head;
	
	public Peer(InetAddress ip, int port, TrackerInfo tracker) {
		this.ip = ip;
		this.port = port;
		this.tracker = tracker;
		this.choked = false;
		this.interested = false;
		this.log_head = String.format("TCP==>%s:%d==>", ip, port);
	}
	
	boolean initialTCP() {
		System.out.println(this.log_head + "start establish tcp");
		try {
			this.client = new Socket(ip, port);
			this.in = this.client.getInputStream();
			this.out = this.client.getOutputStream();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(log_head + "ERROR: can't get the socket use the ip and port==>" + e.getMessage());
			return false;
		}
		System.out.println(this.log_head + "success establish tcp");
		return true;
	}
	
	void handShake() {
		// make handshake for out
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
		DataOutputStream dout = new DataOutputStream(buffer);
		try {
			dout.writeByte(19);
			dout.writeBytes("BitTorrent protocol");
			dout.writeLong(0);
			dout.write(tracker.info_sha1);
			dout.write(tracker.peer_id);
			out.write(buffer.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		Peer other = (Peer) obj;
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
