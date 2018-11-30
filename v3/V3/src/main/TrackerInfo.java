package main;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bencoding.Type;

public class TrackerInfo {
	
	Map<String, Object> tracker, info;
	byte[] info_sha1, pieces_sha1;
	long piece_length;
	List<String> announce_list;
	byte[] peer_id;
	String file_name;
	
	// Below Are Optional Variable
	String comment, created_by, encoding;
	Long creation_date;
	
	// Canculte useful value
	Long file_length = 0L;
	int pieces_num;
	List<FileInfo> fileinfoLs = new ArrayList<>();
	boolean mutifile_mode = false;
	
	public TrackerInfo(String filepath) {
		try {
			peer_id = Settings.PEER_DEFAULT_ID.getBytes("ISO-8859-1");
			tracker = HelpFunction.bencode.decode(Files.readAllBytes(Paths.get(filepath)), Type.DICTIONARY);
			
			this.info = (Map<String, Object>) this.tracker.get("info");
			this.info_sha1 = MessageDigest.getInstance("SHA-1").digest(HelpFunction.bencode.encode(this.info));
			
			this.announce_list = new ArrayList<>();
			if(tracker.containsKey("announce-list")) {
				List<List<String>> als = (List<List<String>>) tracker.get("announce-list");
				for(List<String> a : als) {
					this.announce_list.add(a.get(0));
				}
			}else {
				this.announce_list.add((String) tracker.get("announce"));
			}
			
			this.piece_length = (long) info.get("piece length");
			Settings.PIECE_SIZE = Long.valueOf(this.piece_length).intValue();
			Settings.BLOCK_SIZE = Settings.PIECE_SIZE / 4;
			this.pieces_sha1 = ((String)this.info.get("pieces")).getBytes(HelpFunction.bencode.getCharset());
			
			this.comment = (String) tracker.get("comment");
			this.created_by = (String) tracker.get("created by");
			this.encoding = (String) tracker.get("encoding");
			this.creation_date = (Long) tracker.get("creation date");
			this.file_name = (String) info.get("name");
			
			// Consider the mutifile situation
			if(info.containsKey("files")){
				this.mutifile_mode = true;
				List<Map<String, Object>> files = (List<Map<String, Object>>) (info.get("files"));
				for(Map<String, Object> file : files) {
					long length = (Long) file.get("length");
					List<String> path = (List<String>) file.get("path");
					this.fileinfoLs.add(new FileInfo(length, path));
					this.file_length += length;
				}
			}else {
				this.file_length = (Long) info.get("length");
			}
			this.pieces_num = (int) Math.ceil((double)this.file_length / (double)this.piece_length);
			
		} catch (IOException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String get_info_hash() {
		return HelpFunction.bytesToHex(this.info_sha1);
	}
	
}
