package main;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Piece {
/**
 * Mainly for getting more specified piece data when query tracker.
 * And maintain data when connect to piece. 
 */
	// Basic infor got from .torrent file
	int idx; // position in the pieces list got from trackerinfo
	byte[] sha1; // sha1 hash value
	long size; // piece size
	int last_piece_size;
	
	// Below are use in file transferring
	boolean is_done; // true if all data of this piece are transfered successly
	boolean[] block_done; // true if a block's download is finished.
	byte[][] blocks;
	byte[] piece_data;
	int block_num;
	int block_downloaded_num;
	
	public Piece(int idx, byte[] sha1, long size) {
		this.idx = idx;
		this.sha1 = sha1;
		this.size = size;
		
		this.is_done = false;
		this.block_downloaded_num = 0;
		this.block_num = (int) Math.ceil((double)size / Settings.BLOCK_SIZE);
		this.block_done = new boolean[block_num];
		this.blocks = new byte[block_num][];
		this.last_piece_size = (int) (this.size - (Settings.BLOCK_SIZE * (this.block_num - 1)));
	}
	
	public void addBlockData(int offset, byte[] data) {
		int bidx = offset / Settings.BLOCK_SIZE;
		if(!this.block_done[bidx]) {
			this.block_done[bidx] = true;
			this.block_downloaded_num ++;
			this.blocks[bidx] = data;
		}
	}
	
	public boolean isPieceFullDownloaded() {
		for(boolean b : block_done) {
			if(!b) {
				return false;
			}
		}
	    return true;
	}
	
	public boolean checkPieceHash() {
		int piece_size = 0;
		for(byte[] block : this.blocks) {
			piece_size += block.length;
		}
		this.piece_data = new byte[piece_size];
		int c = 0;
		for(byte[] block : this.blocks) {
			for(byte b : block) {
				this.piece_data[c ++] = b;
			}
		}
		byte[] psha1 = null;
		try {
			psha1 = MessageDigest.getInstance("SHA-1").digest(piece_data);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(psha1 != null && Arrays.equals(this.sha1, psha1)){
			return true;
		}else {
			this.block_done = new boolean[block_num];
			this.block_downloaded_num = 0;
			return false;
		}
	}
	
	List<BlockInfo> getBlockInfoLs() {
		List<BlockInfo> r = new ArrayList<>();
		for(int i = 0; i < this.block_num; i ++) {
			if(!this.block_done[i]) {
				int size = 0;
				if(i == this.block_num - 1) {
					size = this.last_piece_size;
				}else {
					size = Settings.BLOCK_SIZE;
				}
				r.add(new BlockInfo(this.idx, i * Settings.BLOCK_SIZE, size));
			}
		}
		return r;
	}
}
