package main;

import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class Torrent {
	TrackerInfo tracker;
        //Queue<Piece> pieces;d
        LinkedList<Piece> pieces;
	List<Peer> peers;
    /***********************************Begin of editing**************************/


    //Map<String, Integer> m = new HashMap<String, Integer>();
    RarestFirst peer_inital_bitfield;
    
    /***********************************End of editing**************************/

    public Torrent(String torrentFile) {
		tracker = new TrackerInfo(torrentFile);
		peer_inital_bitfield = new RarestFirst(tracker);
	}
	
	void connectToTracker() {
		System.out.println("================>>>>>Start Connect To Tracker================>>>>>");
		pieces = HelpFunction.getPiecesFromTrackerInfo(tracker);
		TransferManager.initial(pieces, this.tracker);
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
		List<Thread> threadls = new ArrayList<>();
		for(Peer peer: peers) {
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
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
					peer.closeTCP();
				}
			    });
			t.start();
			threadls.add(t);
		}
		for(Thread t: threadls) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if(TransferManager.fileDonwloaded()) {
			TransferManager.writeToFile();
			System.out.println("Success Recieve The File: " + this.tracker.file_name);
			
		}else {
			System.out.println("Unable To Recieve The File: " + this.tracker.file_name);
			
		}
	}
   
}

