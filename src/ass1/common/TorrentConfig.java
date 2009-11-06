package ass1.common;

import sicsim.types.NodeId;

public class TorrentConfig {
	public static int MAX_DOWNLOADS = 4; //4;
	public static int MAX_UPLOADS = 4; //4;
	public static int CHUNK_COUNT = 10; // 10
	public static int CHUNK_SIZE = 256; // 256;
	public static int MAX_UPLOAD_BW = 128; // 128;
	public static NodeId TRACKER = new NodeId(0, 0);
	public static NodeId SEEDER = new NodeId(-1, -1);
	public static int TIME_UNIT = 10;
	public static int SEED = 9999;
}
