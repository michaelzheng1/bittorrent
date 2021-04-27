package main;

public class Settings {
	static int BLOCK_SIZE = 1 << 13;
	static int PIECE_SIZE = 1 << 15;
	static final String PEER_DEFAULT_ID = "70e2e3f37c31206bf06-"; // This is a 20-bytes random string
	//	static final String PEER_DEFAULT_ID = "-BT7104-37c31206bf06";
	static final long UDP_PROTOCOL_ID = 0x41727101980L;
	static final int ACTION_CONNECT = 0; 
	static final int ACTION_ANNOUNCE = 1; 
	static final int ACTION_SCRAPE = 2; 
	static final int ACTION_ERROR = 3; 
	static final int ACTION_TRANSACTION_ID_ERROR = 256; 
	static final int EVENT_NONE = 0;  
	static final int EVENT_COMPLETED = 1; 
	static final int EVENT_STARTED = 2; 
	static final int EVENT_STOPPED = 3;
	static final int UDP_RESEND_LIMIT = 3;
	static final short DOWNLOADED_PORT = 2334;
	static final int UDP_TIME_OUT = 1000;
	static final int TCP_TIME_OUT = 36000;
	static boolean LOGGING = true;
	static long TCP_SEND_DELAY = 36000;
	static int BLOCKS_NUM_PER_REQUEST = 10;
}
