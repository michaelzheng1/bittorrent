package main;

import java.util.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.util.Queue;

public class TransferManager {
    //public static Queue<Piece> pieces;
        public static LinkedList<Piece> pieces;
        public static LinkedList<Integer> avail; // This would keep track which is avaiible

	public static int piece_num;
	public static int downloaded_num = 0;
	public static byte[] donwload_pieces = null;
	public static TrackerInfo tracker;
    
    	static void initial(LinkedList<Piece> qpieces, TrackerInfo ttracker) {

	    //	static void initial(Queue<Piece> qpieces, TrackerInfo ttracker) {
		tracker = ttracker;
		pieces = qpieces;
		piece_num =pieces.size();
		donwload_pieces = new byte[Long.valueOf(tracker.file_length).intValue()];
		avail = new LinkedList<Integer>(); 
		//Arrays.fill(avail, Boolean.TRUE);
		for (int i = 0; i < piece_num; i++) {
		    avail.add(i);
		}
	}
	
	static void nextPieces(Peer peer) {
		synchronized(TransferManager.class) {
			if(downloaded_num < piece_num) {
 
			    if(peer.cur_piece == null) {
				if (avail.size() >= 4) {
				    int index = RarestFirst.getRarestPiece();
				    //peer.cur_piece = pieces.poll();
				    //if (!pieces.isEmpty() && pieces.get(index) != null) {
				    if (!avail.isEmpty() && avail.get(index) != null) {

				    	peer.cur_piece = pieces.get(index);
					avail.remove(index);
					RarestFirst.removePiece(index);
				    }
				} else {
				    if (!avail.isEmpty()) {
					Random random = new Random();
					Integer randomIndex = avail.get(random.nextInt(avail.size()));

				    	peer.cur_piece = pieces.get(randomIndex);
				    	RarestFirst.removePiece(randomIndex);
				    }
				}
			    } else {
				 
				    int pidx = peer.cur_piece.idx;

				    for(int i = 0; i < peer.cur_piece.size; i ++) {
					donwload_pieces[pidx * Settings.PIECE_SIZE + i] = peer.cur_piece.piece_data[i];
				    }
					//peer.cur_piece = pieces.poll();
					//if (pieces.size() >= 4) {
					int index = RarestFirst.getRarestPiece();
					System.out.println("I am looking for index: " + index);					
					try {
					    peer.cur_piece = pieces.get(index);
					} catch ( IndexOutOfBoundsException e ) {
					    if (!avail.isEmpty()) {
						Random random = new Random();
						Integer randomIndex = avail.get(random.nextInt(avail.size()));
						peer.cur_piece = pieces.get(randomIndex);
						RarestFirst.removePiece(randomIndex);
					
					    }
					}
					downloaded_num ++;
				}
			}
		}
	}
	
	static boolean fileDonwloaded() {
		return piece_num == downloaded_num;
	}
	
	static void writeToFile() {
		try {
			if(!tracker.mutifile_mode) {
				FileOutputStream out = new FileOutputStream(tracker.file_name);
				out.write(donwload_pieces);
				out.close();
			}else {
				File f = new File(tracker.file_name);
				if(f.exists()) {
					f.delete();
				}
				f.mkdir();
				int begin = 0;
				for(FileInfo fileInfo : tracker.fileinfoLs) {
					String dir = tracker.file_name;
					if(fileInfo.file_path.size() > 1) {
						String tmp = String.join("/", fileInfo.file_path.subList(0, fileInfo.file_path.size() - 1));
						dir = String.join("/", dir, tmp);
					}
					String path_to_file = String.join("/", dir, fileInfo.file_path.get(fileInfo.file_path.size() - 1));
					File dir_f = new File(dir);
					if(!dir_f.exists()) {
						dir_f.mkdirs();
					}
					int file_len = Long.valueOf(fileInfo.file_length).intValue();
					FileOutputStream out = new FileOutputStream(path_to_file);
					out.write(donwload_pieces, begin, file_len);
					out.close();
					begin += file_len;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
