package main;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class Peer {
/**
 * Follow the specification, define the below class variable.
 */
	boolean am_choking, am_interested;
	boolean peer_choking, peer_interested;
	InetAddress ip;
	int port;
	TrackerInfo tracker;
	InputStream in;
	DataOutputStream out;
	Socket client;
	String log_head;
	BitSet bitField;
	LinkedList<Byte> in_buf = new LinkedList<>();
	Queue<Byte> out_buf = new LinkedList<>();
	boolean server_mode = false;
	byte[] file_data;
	
	// Use for the server
	public void setServerMode(String file) throws IOException {
		this.server_mode = true;
		// fill peerls real with real data
		file_data = Files.readAllBytes(Paths.get(file));
	}
	
	public Peer(InetAddress ip, int port, TrackerInfo tracker) {
		this.initializePeer(ip, port, tracker);
	}
	
	public Peer(InetAddress ip, int port, TrackerInfo tracker, String file_location) throws IOException {	
		this.initializePeer(ip, port, tracker);
		setServerMode(file_location);
	}

	ServerSocket serverSocket;
	public void waitingForClient() throws IOException {
		serverSocket = new ServerSocket(this.port);
		this.client = serverSocket.accept();
		this.client.setSoTimeout(Settings.TCP_TIME_OUT);
		this.in = this.client.getInputStream();
		this.out = new DataOutputStream(this.client.getOutputStream());
		System.out.println("Success Listen Client On->" + 
				this.client.getInetAddress() + this.client.getPort());
		
		this.port = this.client.getPort();
		this.ip = this.client.getInetAddress();
		this.log_head = String.format("ServerTCP==>%s:%d==>", ip, port);
	}
	
	private void initializePeer(InetAddress ip, int port, TrackerInfo tracker) {
		this.ip = ip;
		this.port = port;
		this.tracker = tracker;
		this.am_choking = true;
		this.am_interested = false;
		this.peer_choking = true;
		this.peer_interested = false;
		this.log_head = String.format("TCP==>%s:%d==>", ip, port);
	}

	boolean initialTCP() {
		System.out.println(this.log_head + "start establish tcp");
		try {
			this.client = new Socket(ip, port);
			this.client.setSoTimeout(Settings.TCP_TIME_OUT);
			this.in = this.client.getInputStream();
			this.out = new DataOutputStream(this.client.getOutputStream());
		} catch (IOException e) {
			System.out.println(log_head + "ERROR: can't get the socket use the ip and port==>" + e.getMessage());
			return false;
		}
		System.out.println(this.log_head + "success establish tcp");
		return true;
	}
	
	boolean handShake() {
		// make handshake for out
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
		DataOutputStream bout = new DataOutputStream(buffer);
		try {
			bout.writeByte(19);
			bout.writeBytes("BitTorrent protocol");
			bout.writeLong(0);
			bout.write(tracker.info_sha1);
			bout.write(tracker.peer_id);
			this.setOutBuf(buffer.toByteArray());
		} catch (IOException e) {
			System.out.println(this.log_head + e.getMessage());
			return false;
		}
		return true;
	}
	
	boolean is_hand_shake_response = true;
	boolean recieveHandle() {
		while(in_buf.size() > 3) {
			// check the info hash in the handshake msg to make the validation of the peer.
			if(is_hand_shake_response) {
				if(checkInfoHash()) {
					is_hand_shake_response = false;
					if(Settings.LOGGING) System.out.println(this.log_head + "Valid Handshake");
					if(this.server_mode) {
						handShake();
						this.makeInterestedAndUnChokingMsg();
						return true;
					}
				}else {
					if(Settings.LOGGING) System.out.println(this.log_head + "Invalid Handshake");
					return false;
				}
			}
			if(this.in_buf.size() < 4) {
				return true;
			}
			int len = ByteBuffer.wrap(this.viewInBuf(0, 4)).getInt();
			// check whether is the keep alive msg
			if(this.in_buf.size() == 4 || len == 0) {
				return true;
			}
			
			byte id = in_buf.get(4);
//			if(Settings.LOGGING) System.out.println(this.log_head + "len=" + len + " id=" + id);
			byte[] payload = this.viewInBuf(5, len + 4);
			if(payload.length < len - 1) {
				// don't get full msg
//				if(Settings.LOGGING) System.out.println(this.log_head + "Recieve message is not complete.");
				return true;
			}
			this.removePartOfInBuf(len + 4);
			if(id == 0) {
				this.peer_choking = true;
				if(Settings.LOGGING) System.out.println(this.log_head + "Peer choking");
				continue;
			}else if(id == 1) {
				this.peer_choking = false;
				if(Settings.LOGGING) System.out.println(this.log_head + "Peer unchoking");
				if(this.server_mode) {
					// also make an unchoke msg to client
					
				}else {
					makeRequestBlockMsg();
					// Not use have msg. the reason why we don't use it is that, we want to make suere we download full data from the peer.
					// For some reason we don't know, the implementation of bittorrent doesn't return all the msg it have
					// And our experiment environment can only get one peer which we deploy a server and install bittorrent on it.
				}
				// this time means can send to request msg
			}else if(id == 2) {
				this.peer_interested = true;
				if(Settings.LOGGING) System.out.println(this.log_head + "Peer interested");
			}else if(id == 3) {
				this.peer_interested = false;
				if(Settings.LOGGING) System.out.println(this.log_head + "Peer uninterested");
				continue;
			} else if(id == 4) {
				// Handle have information
				int num = ByteBuffer.wrap(payload).getInt();
				if(Settings.LOGGING) System.out.println(this.log_head + "Have num " + num + " piece");
				this.bitField.set(num);
			}else if(id == 5) {
				// Handle bitfield information
				this.bitField = BitSet.valueOf(payload);
			}else if(id == 6) {
				sendPieceBlock(payload);
			}
			else if(id == 7) {
				int piece_idx = ByteBuffer.wrap(Arrays.copyOfRange(payload, 0, 4)).getInt();
				int block_offset = ByteBuffer.wrap(Arrays.copyOfRange(payload, 4, 8)).getInt();
				byte[] block_data = Arrays.copyOfRange(payload, 8, payload.length);
				if(TransferManager.cur_piece.idx != piece_idx) {
					return true;
				}
				Piece cur_piece = TransferManager.cur_piece;
				cur_piece.addBlockData(block_offset, block_data);
				if(cur_piece.isPieceFullDownloaded() && cur_piece.checkPieceHash()) {
					if(Settings.LOGGING) System.out.println(this.log_head + "Success get piece " + TransferManager.downloaded_num);		
					TransferManager.nextPieces();
				}else {
					if(Settings.LOGGING) System.out.println(this.log_head + "Get piece data error. Retry.");
				}
				makeRequestBlockMsg();
			}else {
				makeRequestBlockMsg();
			}
			if(!this.server_mode && this.peer_choking) {
				makeInterestedAndUnChokingMsg();
			}
		}
		return true;
	}
	
	void sendPieceBlock(byte[] payload) {
		int piece_idx = ByteBuffer.wrap(Arrays.copyOfRange(payload, 0, 4)).getInt();
		int offset = ByteBuffer.wrap(Arrays.copyOfRange(payload, 4, 8)).getInt();
		int length = ByteBuffer.wrap(Arrays.copyOfRange(payload, 8, 12)).getInt();
		System.out.println(this.log_head + String.format(this.log_head + "send piece_%d offset_%d length_%d", piece_idx, offset, length));
		int begin = piece_idx * Settings.PIECE_SIZE + offset;
		int end = begin + length;
		byte[] send_data = Arrays.copyOfRange(this.file_data, begin, end);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
		DataOutputStream bout = new DataOutputStream(buffer);
		try {
			bout.writeInt(9 + length);
			bout.writeByte(7);
			bout.writeInt(piece_idx);
			bout.writeInt(offset);
			bout.write(send_data);
			this.setOutBuf(buffer.toByteArray());
		}catch (IOException e) {
			System.out.println(this.log_head + this.log_head + e.getMessage());
		}
	}
	
	void makeRequestBlockMsg() {
		if(this.peer_choking || this.out_buf.size() > 0 || TransferManager.fileDonwloaded()) {
			return ;
		}
		// send many block request to accurate the download process
		List<BlockInfo> blockInfoLs = TransferManager.cur_piece.getBlockInfoLs();
		for(BlockInfo info : blockInfoLs) {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
			DataOutputStream bout = new DataOutputStream(buffer);
			try {
				bout.writeInt(13);
				bout.writeByte(6);
				bout.writeInt(info.idx);
				bout.writeInt(info.offset);
				bout.writeInt(info.size);
				this.setOutBuf(buffer.toByteArray());
			} catch (IOException e) {
				System.out.println(this.log_head + this.log_head + e.getMessage());
			}
			if(Settings.LOGGING) System.out.println(this.log_head + "Piece_idx: " + TransferManager.cur_piece.idx + " Send block request:"  + info.toString());
		}
	}
	
	void makeInterestedAndUnChokingMsg() {
		this.am_choking = false;
		this.am_interested = true;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(32);
		DataOutputStream bout = new DataOutputStream(buffer);
		try {
			bout.writeInt(1);
			bout.writeByte(1);
			bout.writeInt(1);
			bout.writeByte(2);
			this.setOutBuf(buffer.toByteArray());
		} catch (IOException e) {
			System.out.println(this.log_head + this.log_head + e.getMessage());
		}
	}
	
	byte[] msg = new byte[1080];
	boolean recieve() {
		int msg_len = -1;
		try {
			msg_len = in.read(msg);
			if(msg_len < 0) {
				System.out.println(this.log_head + "Close by peer");
				return false;
			}
		} catch (IOException e) {
			System.out.println(this.log_head + e.getMessage());
			return false;
		}
		this.setInBuf(msg, msg_len);
		return true;
	}
	
	boolean send() {
		try {
			out.write(this.getOutBuf());
		} catch (IOException e) {
			System.out.println(this.log_head + e.getMessage());
			return false;
		}
		return true;
	}
	
	boolean checkInfoHash() {
		int sha1_start = 28;
		int sha1_end = 28 + tracker.info_sha1.length;
		byte[] res_sha1 = this.viewInBuf(sha1_start, sha1_end);
		if(Arrays.equals(res_sha1, tracker.info_sha1)) {
			this.removePartOfInBuf(sha1_end + 20);
			return true;
		}else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + port;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Peer other = (Peer) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (port != other.port)
			return false;
		return true;
	}
	
	public boolean closeTCP() {
		try {
			this.client.close();
		} catch (IOException e) {
			System.out.println(this.log_head + e.getMessage());
			return false;
		}
		return true;
	}
	
	public void setOutBuf(byte[] data) {
		for(byte d : data) {
			this.out_buf.offer(d);
		}
	}
	
	public byte[] getOutBuf() {
		byte[] data = new byte[this.out_buf.size()];
		int idx = 0;
		while(!this.out_buf.isEmpty()) {
			data[idx++] = this.out_buf.poll();
		}
		return data;
	}
	
	public void setInBuf(byte[] data, int length) {
		for(int i = 0; i < length; i ++) {
			this.in_buf.offer(data[i]);
		}
	}
	
	public byte[] viewInBuf(int start, int end) {
		int bound = Math.min(end, in_buf.size());
		byte[] data = new byte[bound - start];
		int idx = 0;
		for(int i = start; i < bound; i ++) {
			data[idx++] = in_buf.get(i);
		}
		return data;
	}
	
	public void removePartOfInBuf(int length) {
		while(length -- > 0) {
			this.in_buf.poll();
		}
	}
	
}
