package main;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Torrent {
	TrackerInfo tracker;
	List<Piece> pieces;
	List<Peer> peers;
	
	public Torrent(String torrentFile) {
		tracker = new TrackerInfo(torrentFile);
	}
	
	void connectToTracker() {
		System.out.println("================>>>>>Start Connect To Tracker================>>>>>");
		pieces = Collections.synchronizedList(HelpFunction.getPiecesFromTrackerInfo(tracker));
		peers = Collections.synchronizedList(HelpFunction.getPeersFromTrackerInfo(tracker));
		System.out.println("================>>>>>End Connect To Tracker================>>>>>");
	}
	
	void tcpEstablish() {
		System.out.println("================>>>>>Start TCP To Peers================>>>>>");
		List<Thread> threadls = new ArrayList<>();
		List<Peer> peers_timeout = Collections.synchronizedList(new ArrayList<>());
		for(Peer p : peers) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					if(!p.initialTCP()) {
						peers_timeout.add(p);
					}
				}
			});
			t.start();
			threadls.add(t);
		}
		for(Thread t : threadls) {
			try {
				t.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Remove timeout peer
		for(Peer p : peers_timeout) {
			peers.remove(p);
		}
		System.out.println("================>>>>>After TCP and first handshake below peer are working fine");
		for(Peer p : peers) {
			System.out.println(p.ip.getHostAddress() + ":" + p.port);
		}
		System.out.println("================>>>>>End TCP To Peers================>>>>>");
	}
	
	void startServer(int port, String filePath) throws IOException {
		byte[] filedata = Files.readAllBytes(Paths.get(filePath));
		ServerSocket serverSocket = new ServerSocket(port);
		System.out.println("Server Listening on " + port);
		Socket client = serverSocket.accept();
		System.out.println("Got Client request");
		DataInputStream in = new DataInputStream(client.getInputStream());
		OutputStream out = client.getOutputStream();
		while(true) {
			
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
			DataOutputStream bout = new DataOutputStream(buffer);
		}
	}
	
	void startClient(int port, String tracker_file) throws UnknownHostException {
		TrackerInfo tracker = new TrackerInfo(tracker_file);
		pieces = Collections.synchronizedList(HelpFunction.getPiecesFromTrackerInfo(tracker));
		Peer client = new Peer(InetAddress.getByName("127.0.0.1"), port, tracker);
		client.initialTCP();
	}
	
}

