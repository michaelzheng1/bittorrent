package main;

import java.io.IOException;

public class Run {
	
	public static void main(String[] args) throws IOException {
		System.out.println("For downloading from bittorrent client: java  -jar bittorrent-client.jar test.torrent");
		System.out.println("For downloading from our bittorrent instance, first need to start a sever, then start a client. Use below command");;
		System.out.println("Start a server: java -jar bittorrent-client.jar test.torrent server serverport download_file_location");
		System.out.println("Start a client: java -jar bittorrent-client.jar test.torrent client serverport");
		System.out.println("Pay attention: serverport shounld be the same usable port!!");
		System.out.println("You can look the report.pdf for detail effect after excute below command");
		// Added a timer
		Timer time  = new Timer();
		time.start();
		if(args.length > 0) {
			if(args.length == 1) {
				Torrent torrent = new Torrent(args[0]);
				torrent.connectToTracker();
				torrent.tcpEstablish();
				torrent.download();
				time.end();
				time.getTotalTime();
			}else {
				String torrent_file = args[0];
				String type = args[1];
				int port = Integer.valueOf(args[2]);
				if(type.equals("server")) {
					String file_location = args[3];
					HelpFunction.startServer(torrent_file, port, file_location);
				}else if(type.equals("client")) {
					HelpFunction.startClient(torrent_file, port);
				}else {
					System.out.println("Only 'server' or 'client' can be the type parameter");
				}
			}
		}else {
			System.out.println("Please place a agrument menthioned above");
		}
	}
	
}
