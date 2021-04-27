package main;

import java.util.*;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import bencoding.Bencode;
import bencoding.Type;

public class RarestFirst {
    
    public static LinkedList<Piece> pieces;
    public static int piece_num;
    public static List<Peer> peers;
    public static Map<Integer, Integer> piece_count;
    
    public  RarestFirst(TrackerInfo t) {
	peers = Collections.synchronizedList(HelpFunction.getPeersFromTrackerInfo(t));
	pieces = HelpFunction.getPiecesFromTrackerInfo(t);
	piece_num = pieces.size();
	piece_count = new HashMap<Integer, Integer>();
	System.out.println("the piece_num is" +piece_num);
	for (int i = 0; i < piece_num; i++) {
	    piece_count.put(i, 0);
	}

       
    }    
    public static void update(Integer index) {
	Integer value = piece_count.get(index);
	value = value + 1;
	piece_count.put(index, value);
   
    }
    
    public static void removePiece(Integer index) {
	piece_count.remove(index);
    }
    
    public static int getRarestPiece() {
	int min = Integer.MAX_VALUE;
	int minKey = -1;
	List<Integer> potential_rare_piece =  new ArrayList<Integer>();
	Map<Integer, Integer> clone = new HashMap<Integer,Integer>(piece_count);
	// Finding 4 potential rare pieces
	for (int i = 0; i < 4; i++) {
	    for (Integer key: clone.keySet()) {
		int value = clone.get(key);
		if (value > 0) {
		    if (value < min ) {
			minKey = key;
			min = value;
		    }
		}	
	    }
	    // If all the key values are zero then pick a random index
	    if (minKey < 0 && clone.size() > 0) {

		int pick = ThreadLocalRandom.current().nextInt(clone.size());
		clone.remove(pick);
		potential_rare_piece.add(pick);
	    } else {
		clone.remove(minKey);
		potential_rare_piece.add(minKey);
	    }
	}
	
	// Randomly select between the 4 potential rare peices
	int rand_num = ThreadLocalRandom.current().nextInt(0, 4); 
	
	//piece_count.remove(potential_rare_piece.get(rand_num));	
	return potential_rare_piece.get(rand_num);
    }
	
	    
}
	
