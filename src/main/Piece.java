package main;

public class Piece {
/**
 * Mainly for getting more specified piece data when query tracker.
 * And maintain data when connect to piece. 
 */
	// Basic infor got from .torrent file
	int idx; // position in the pieces list got from trackerinfo
	byte[] sha1; // sha1 hash value
	int size; // piece size
	
	// Below are use in file transferring
	boolean is_done; // true if all data of this piece are transfered successly
	boolean[] block_done; // true if a block's download is finished.
	byte[][] blocks;
	int block_num;
	int block_downloaded;
	
	public Piece(int idx, byte[] sha1, int size) {
		this.idx = idx;
		this.sha1 = sha1;
		this.size = size;
		
		this.is_done = false;
		this.block_downloaded = 0;
		this.block_num = (int) Math.ceil((double)size / Settings.BLOCK_SIZE);
		this.block_done = new boolean[block_num];
		this.blocks = new byte[block_num][Settings.BLOCK_SIZE];
	}
	
}
