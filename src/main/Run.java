package main;

public class Run {
	
	public static void main(String[] args) {
		if(args.length > 0) {
			Torrent torrent = new Torrent(args[0]);
			torrent.connectToTracker();
			torrent.tcpEstablish();
		}else {
			System.out.println("Please place a torrent file as agrument");
		}
	}
	
}
