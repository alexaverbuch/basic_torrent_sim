package ass1.common;

import sicsim.types.NodeId;

public class TorrentConfig {
	public static final int MAX_DOWNLOADS = 4; //4;
	public static final int MAX_UPLOADS = 4; //4;
	public static final int CHUNK_COUNT = 10; // 10
	public static final int CHUNK_SIZE = 64; // 256;
	public static final int MAX_UPLOAD_BW = 128; // 128;
	public static final NodeId TRACKER = new NodeId(0, 0);
	public static final NodeId SEEDER = new NodeId(-1, -1);
	public static final int TIME_UNIT = 10;
	public static final int SEED = 9999;
	public static final int POLITE_TIME = 250;
}
