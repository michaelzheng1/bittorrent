package main;

public class UDPResponse {
	byte[] data;
	String ip;
	int port;
	
	public UDPResponse(byte[] data, String ip, int port) {
		this.data = data;
		this.ip = ip;
		this.port = port;
	}
}
