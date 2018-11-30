package main;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UDPManager {
	
	List<Thread> threadls;
	List<UDPResponse> udp_data_ls;
	
	public UDPManager() {
		threadls = new ArrayList<>();
		udp_data_ls = new ArrayList<>();
	}
	
	void addUdpPlan(byte[] send_data, String host, int port, int check_action, int check_trans_id, int check_length) {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				byte[] rd;
				String log_head = host + ":" + port + "==>";
				try {
					InetAddress address = InetAddress.getByName(host);
					System.out.println(log_head + "success query ip address: " + address.getHostAddress());
					System.out.println(log_head + "starting connect use upd");
					rd = handleUdpSend(send_data, address, port, check_action, check_trans_id, check_length, 1, log_head);
					if(rd != null) {
						synchronized(UDPManager.class) {
							udp_data_ls.add(new UDPResponse(rd, host, port));
							System.out.println(log_head + "success fetch data");
						}
					}else {
						System.out.println(log_head + "fail to fetch data");
					}
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
				}
			}
		});
		this.threadls.add(t);
		t.start();
	}
	
	List<UDPResponse> excutePlan() {
		for(Thread t : this.threadls) {
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return this.udp_data_ls;
	}
	
	List<UDPResponse> getUDPReturnData(){
		return this.udp_data_ls;
	}
	
	public byte[] handleUdpSend(byte[] send_data, InetAddress address, int port, int check_action, int check_trans_id, int check_length, int count, String log_head) {
		DatagramPacket rp = null;
		// send data
		DatagramSocket socket = null;
		try {
			socket = new DatagramSocket();
			socket.setSoTimeout(Settings.UDP_TIME_OUT);
			DatagramPacket packet = new DatagramPacket(send_data, send_data.length, address, port);
			socket.send(packet);
			System.out.println(log_head + "start send data");
			
			// receive data
			byte[] rdata = new byte[1024];
			rp = new DatagramPacket(rdata, 0, 1024);
			socket.receive(rp);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(log_head + "request " + count + " times==>" + " ERROR:timeout:" + e.getMessage());
			if(count < Settings.UDP_RESEND_LIMIT) {
				return handleUdpSend(send_data, address, port, check_action, check_trans_id, check_length, ++ count, log_head);
			}else {
				return null;
			}
		} finally {
			if(socket != null) {
				socket.close();
			}
		}
		if(rp != null && rp.getLength() >= check_length) {
			byte[] rdata = rp.getData();
			int action = ByteBuffer.wrap(Arrays.copyOf(rdata, 4)).getInt();
			int trans_id = ByteBuffer.wrap(Arrays.copyOfRange(rdata, 4, 8)).getInt();
			if(action != check_action || trans_id != check_trans_id) {
				System.out.println(log_head + "request " + count + " times==>" + " ERROR:get response but invalid action status and trans_id==>" 
			+ String.format("s_action:%d-s_trans_id:%d r_action:%d-r_trans_id:%d", check_action, check_trans_id, action, trans_id));
				if(count < Settings.UDP_RESEND_LIMIT) {
					return handleUdpSend(send_data, address, port, check_action, check_trans_id, check_length, ++ count, log_head);
				}else {
					return null;
				}
			}
			return Arrays.copyOfRange(rdata, 0, rp.getLength());
		}else {
			if(count < Settings.UDP_RESEND_LIMIT) {
				System.out.println(log_head + "request " + count + " times==>" + " ERROR:response length is invalid or got not response");
				return handleUdpSend(send_data, address, port, check_action, check_trans_id, check_length, ++ count, log_head);
			}else {
				return null;
			}
		}
	}
	
	void reset() {
		this.threadls.clear();
		this.udp_data_ls.clear();
	}
}
