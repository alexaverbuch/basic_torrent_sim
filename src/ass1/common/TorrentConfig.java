package ass1.common;

import sicsim.types.NodeId;

public class TorrentConfig {
	public static int MAX_NUM_UPLOADERS = 4; 
	public static int MAX_NUM_DOWNLOADERS = 4; 
	public static int CHUNK_COUNT = 5; //10
	public static int CHUNK_SIZE = 32; //256;
	public static int MAX_UPLOAD_BW = 256; //128;
	public static NodeId TRACKER = new NodeId(0, 0);
	public static int TIME_UNIT = 10;
	public static int STANDARD_TIMEOUT = 100; 
	public static int SEED = 9999;
}
