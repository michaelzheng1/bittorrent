package main;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TransferManager {
	public static Deque<Piece> pieces;
	public static Piece cur_piece;
	public static int piece_num;
	public static int downloaded_num = 0;
	public static List<Piece> donwload_pieces = new ArrayList<>();
	
	static void initial() {
		piece_num =pieces.size();
		cur_piece = pieces.poll();
	}
	
	static void nextPieces() {
		if(downloaded_num < piece_num) {
			donwload_pieces.add(cur_piece);
			cur_piece = pieces.poll();
			downloaded_num ++;
		}
	}
	
	static boolean fileDonwloaded() {
		return piece_num == downloaded_num;
	}
	
	static void writeToFile(String filename) {
		try {
			FileOutputStream out = new FileOutputStream(filename);
			for(Piece p : donwload_pieces) {
				out.write(p.piece_data);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
