package main;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import bencoding.Bencode;
import bencoding.Type;


public class HelpFunction {
	
	public static Bencode bencode = new Bencode();
	static UDPManager udpConnectManager = new UDPManager();
	static UDPManager udpAnnouceManager = new UDPManager();
	static TrackerInfo tracker;
    public static LinkedList<Piece> getPiecesFromTrackerInfo(TrackerInfo trackerinfo){

    //public static Queue<Piece> getPiecesFromTrackerInfo(TrackerInfo trackerinfo){
		tracker = trackerinfo;
		byte[] pieces_sha1 = trackerinfo.pieces_sha1;
		//Queue<Piece> result = new LinkedList<>();
		LinkedList<Piece> result = new LinkedList<Piece>();

		int bound = trackerinfo.pieces_num;
		for(int i = 0; i < bound; i ++) {
			byte[] sha1 = Arrays.copyOfRange(pieces_sha1, i * 20, (i + 1) * 20);
			if(i < bound - 1) {
			    //result.offer(new Piece(i, sha1, trackerinfo.piece_length));
			    result.add(new Piece(i, sha1, trackerinfo.piece_length));

			}else {
			    //	result.offer(new Piece(i, sha1, (int)(trackerinfo.file_length - trackerinfo.piece_length * i)));
			    result.add(new Piece(i, sha1, (int)(trackerinfo.file_length - trackerinfo.piece_length * i)));

			}
		}
		return result;
	}
	
	static Set<IPAndPort> ipAndPortLs = new HashSet<>();
	public static List<Peer> getPeersFromTrackerInfo(TrackerInfo trackerinfo) {
		tracker = trackerinfo;
		List<Peer> peerls = new LinkedList<Peer>();
		List<Thread> http_threadls = new ArrayList<>();
		System.out.println("================>>>>>Start http connect server================>>>>>");
		for(String url : trackerinfo.announce_list) {
			String urltype = url.substring(0, url.indexOf(":"));
			if(urltype.equals("udp")) {
				handleUdpConnect(url, trackerinfo.get_info_hash(), Settings.PEER_DEFAULT_ID);
			}else if(urltype.equals("http")) {
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						// TODO Auto-generated method stub
						handlHttpRequest(url, trackerinfo.get_info_hash(), Settings.PEER_DEFAULT_ID);
					}
					
				});
				t.start();
				http_threadls.add(t);
			}
		}
		for(Thread t : http_threadls) {
			try {
				t.join();
			} catch (InterruptedException e) {
				System.out.println("================>>>>>Http connect error");
			}
		}
		System.out.println("================>>>>>End http connect server================>>>>>");
		System.out.println("================>>>>>Start udp connect server================>>>>>");
		List<UDPResponse> udpConnectResponse = udpConnectManager.excutePlan();
		System.out.println(String.format("================>>>>>End udp connect server find %d valid server================>>>>>", udpConnectResponse.size()));
		if(udpConnectResponse.size() > 0) {
			System.out.println("================>>>>>Start udp announce server================>>>>>");
			for(UDPResponse udpres : udpConnectResponse) {
				handleUdpAnnounce(trackerinfo.get_info_hash(), Settings.PEER_DEFAULT_ID, udpres);
			}
		}else {
			System.out.println("================>>>>>No server connected. Maybe you can retry the program or get another torrent file instead================>>>>>");
		}
		List<UDPResponse> udpAnnouceResponse = udpAnnouceManager.excutePlan();
		if(udpAnnouceResponse.size() > 0) {
			System.out.println(String.format("================>>>>>End udp announce server find %d valid result================>>>>>", udpAnnouceResponse.size()));
			System.out.println("================>>>>>Start parse announce response to peer ip================>>>>>");
			for(UDPResponse udpres : udpAnnouceResponse) {
				byte[] rdata = udpres.data;
				if(rdata.length > 20) {
					for(int i = 20; i < rdata.length; i += 6) {
						try {
							ipAndPortLs.add(new IPAndPort(
									InetAddress.getByAddress(Arrays.copyOfRange(rdata, i, i + 4)), 
									ByteBuffer.wrap(rdata, i + 4, 2).getShort()));
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							System.out.println(e.getMessage());
						}
					}
				}
			}
		}else {
			System.out.println("================>>>>>No announce response founded. Maybe you can retry the program or get another torrent file instead================>>>>>");
		}
		if(ipAndPortLs.size() > 0) {
			System.out.println("================>>>>>End parse announce response to peer ip and got below peer================>>>>>");
			for(IPAndPort ipp : ipAndPortLs) {
				System.out.println("================>>>>>" + ipp.toString());
				peerls.add(new Peer(ipp.ip, ipp.port, tracker));
			}
		}else {
			System.out.println("================>>>>>No peer ip founded. Maybe you can retry the program or get another torrent file instead================>>>>>");
		}
		
		return peerls;
	}
	
	private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static void handlHttpRequest(String url, String info_hash, String peer_id) {
		String info_head = url + "==>";
		String charset = "ISO-8859-1";
		try {
			String query = String.format("info_hash=%s&peer_id=%s",
					URLEncoder.encode(new String(tracker.info_sha1, charset), charset),
					URLEncoder.encode(new String(tracker.peer_id, charset), charset));
			URLConnection con = new URL(url + "?" + query).openConnection();
//			System.out.println(con.getURL().toString());
			con.setConnectTimeout(1000);
			con.setReadTimeout(5000);
			
			InputStream response = con.getInputStream();
			HttpURLConnection httpcon = (HttpURLConnection) con;
			int status = httpcon.getResponseCode();
			System.out.println(info_head + "Status Code: " + status);
			// It means the request need redirect
			if(status == 302) {
				String rurl = httpcon.getHeaderField("Location");
				System.out.println("Redirect===>" + rurl);
				URLConnection rcon = new URL(rurl + "/announce?" + query).openConnection();
				rcon.setConnectTimeout(1000);
				rcon.setReadTimeout(5000);
				response = rcon.getInputStream();
				httpcon = (HttpURLConnection) rcon;
			}
			
			Map<String, Object> bres = bencode.decode(getByteArrayFromResponse(response), Type.DICTIONARY);
			Object peersobj = bres.get("peers");
			if(peersobj instanceof List) {
				List<Map<String, Object>> http_peers = (List<Map<String, Object>>) peersobj;
				for(Map<String, Object> http_p : http_peers) {
					synchronized(HelpFunction.class) {
						ipAndPortLs.add(new IPAndPort(InetAddress.getByName((String)http_p.get("ip")), (short)http_p.get("port")));
					}
				}
			}else if(peersobj != null){
				byte[] bpeers = ((String)peersobj).getBytes(bencode.getCharset());
				for(int i = 0; i < bpeers.length; i += 6) {
					synchronized(HelpFunction.class) {
						ipAndPortLs.add(new IPAndPort(
								InetAddress.getByAddress(Arrays.copyOfRange(bpeers, i, i + 4)), 
								ByteBuffer.wrap(bpeers, i + 4, 2).getShort()));
					}
				}
			}
		}catch(IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	static byte[] getByteArrayFromResponse(InputStream response) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int n;
		byte[] buffer = new byte[4096]; // 4kb per read
		while((n = response.read(buffer, 0, buffer.length)) != -1) {
			baos.write(buffer, 0, n);
		}
		return baos.toByteArray();
	}
	
	public static void handleUdpConnect(String url, String info_hash, String peer_id){
		try {
			String host = url.split("://")[1];
			int port_start = host.indexOf(":") + 1;
			int port_end = host.indexOf("/");
			String port_str = port_end > 0 ? host.substring(port_start, port_end) : host.substring(port_start);
			int port = Integer.valueOf(port_str);
			host = host.split(":")[0];
			
			// Initialize parameter
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
			DataOutputStream out = new DataOutputStream(buffer);
			out.writeLong(Settings.UDP_PROTOCOL_ID);
			out.writeInt(Settings.ACTION_CONNECT);
			int trans_id = new Random().nextInt();
			out.writeInt(trans_id);
			
			// Add to plan
			udpConnectManager.addUdpPlan(buffer.toByteArray(), host, port, Settings.ACTION_CONNECT, trans_id, 16);
			
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void handleUdpAnnounce(String info_hash, String peer_id, UDPResponse udpr) {
		byte[] rdata = udpr.data;
		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
			DataOutputStream out = new DataOutputStream(buffer);
			ByteBuffer bbf = ByteBuffer.wrap(Arrays.copyOfRange(rdata, 8, 16));
			out.writeLong(bbf.getLong()); // connection_id
			out.writeInt(Settings.ACTION_ANNOUNCE); //action
			int trans_id = new Random().nextInt();
			out.writeInt(trans_id); // transaction_id
			out.write(tracker.info_sha1);
			out.write(tracker.peer_id); //peer_id
			out.writeLong(0); //downloaded
			out.writeLong(tracker.file_length.longValue()); //left
			out.writeLong(0); //uploaded
			out.writeInt(Settings.EVENT_NONE); // event
			out.writeInt(0); //IP address
			out.writeInt(new Random().nextInt()); //Key
			out.writeInt(-1); //num_want
			out.writeShort(Settings.DOWNLOADED_PORT); //port
			
			// Add to plan
			byte[] pdata = buffer.toByteArray();
			udpAnnouceManager.addUdpPlan(pdata, udpr.ip, udpr.port, Settings.ACTION_ANNOUNCE, trans_id, 20);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	static void startClient(String tracker_file, int port) throws UnknownHostException {
		// Do the first handshake
		TrackerInfo ctracker = new TrackerInfo(tracker_file);
		TransferManager.initial(getPiecesFromTrackerInfo(ctracker), ctracker);
		Peer peer = new Peer(InetAddress.getByName("127.0.0.1"), port, ctracker);
		peer.initialTCP();
		peer.handShake();
		while(!TransferManager.fileDonwloaded()) {
			if(peer.out_buf.size() > 0) {
				if(!peer.send()) {
					break;
				}
			}
			if(!peer.recieve()) {
				if(peer.out_buf.size() == 0) {
					break;
				}
			}else {
				peer.recieveHandle();
			}
		}
		if(TransferManager.fileDonwloaded()) {
			TransferManager.writeToFile();
			System.out.println("Success Recieve The File: " + ctracker.file_name);
		}else {
			System.out.println("Unable To Recieve The File: " + ctracker.file_name);
		}
		peer.closeTCP();
	}
	
	static void startServer(String tracker_file, int port, String file_location) throws IOException {
		// Do the first handshake
		TrackerInfo ctracker = new TrackerInfo(tracker_file);
		Peer peer = new Peer(InetAddress.getByName("127.0.0.1"), port, ctracker, file_location);
		peer.waitingForClient();
		while(true) {
			if(peer.out_buf.size() > 0) {
				if(!peer.send()) {
					break;
				}
			}
			if(!peer.recieve()) {
				if(peer.out_buf.size() == 0) {
					break;
				}
			}else {
				peer.recieveHandle();
			}
		}
		peer.closeTCP();
	}
	
}
