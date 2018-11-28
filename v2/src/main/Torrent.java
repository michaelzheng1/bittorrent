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
import java.util.Deque;
import java.util.List;

public class Torrent {
	TrackerInfo tracker;
	Deque<Piece> pieces;
	List<Peer> peers;
	
	public Torrent(String torrentFile) {
		tracker = new TrackerInfo(torrentFile);
	}
	
	void connectToTracker() {
		System.out.println("================>>>>>Start Connect To Tracker================>>>>>");
		pieces = HelpFunction.getPiecesFromTrackerInfo(tracker);
		TransferManager.pieces = pieces;
		TransferManager.initial();
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
	
	void download() {
		// Do the first handshake
		for(Peer peer: peers) {
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
				peer.closeTCP();
				break;
			}
		}
		System.out.println("Success Recieve The File: " + this.tracker.file_name);
		TransferManager.writeToFile(this.tracker.file_name);
	}
	
}

