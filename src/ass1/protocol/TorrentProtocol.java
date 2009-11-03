package ass1.protocol;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import ass1.common.TorrentConfig;
import sicsim.types.NodeId;

public class TorrentProtocol {
	static Logger logger = Logger.getLogger(TorrentProtocol.class);

	private int uploads = 0;
	private int downloads = 0;

	private ArrayList<String> friends = new ArrayList<String>(); // Nodes I know
	private ArrayList<String> exFriends = new ArrayList<String>(); // Friends that left/failed
	private ArrayList<String> activeUploads = new ArrayList<String>(); // "seeder:chunk"
	private ArrayList<String> activeDownloads = new ArrayList<String>(); // "seeder:chunk"
	private ArrayList<String> requiredChunks = new ArrayList<String>(); // chunkIndexStr

	private NodeId nodeId = null;

	public TorrentProtocol(NodeId nodeId, boolean seeding) {
		super();
		this.nodeId = nodeId;
		if (seeding == false) {
			for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
				requiredChunks.add(Integer.toString(i));
			}
		}
	}

	// Currently "friends" only ensures:
	// --> We only register with failureDetector once per peer
	// --> --> multiple failureDetector.register(peer) causes multiple
	// notifications (dumb)
	public boolean addFriend(String friend) {
		if (friends.contains(friend) == false) {
			friends.add(friend);
			return true;
		}

		return false;
	}

	// =========================================================================
	// ********************************CLEANUP**********************************
	// =========================================================================

	public void cleanupFriendFailure(String friend) {
		// Cleanup activeDownloads
		cleanupActiveDownloads(friend);

		// Cleanup hesitantUploads
		cleanupActiveUploads(friend);

		if (exFriends.contains(friend) == false) {
			exFriends.add(friend);
		}
	}

	private void cleanupActiveDownloads(String friend) {
		// Cleanup activeDownloads
		ArrayList<String> activeDownloadsToRemove = new ArrayList<String>();

		for (int i = 0; i < activeDownloads.size(); i++) {
			String tempActDown = activeDownloads.get(i);

			String seederStr = tempActDown.substring(0, tempActDown.indexOf(":"));
			if (seederStr.equals(friend)) {
				activeDownloadsToRemove.add(tempActDown);
			}
		}

		for (String actDown : activeDownloadsToRemove) {
			activeDownloads.remove(actDown);
			downloads--;
		}
	}

	private void cleanupActiveUploads(String friend) {
		// Cleanup activetUploads
		ArrayList<String> activeUploadsToRemove = new ArrayList<String>();

		for (int i = 0; i < activeUploads.size(); i++) {
			String tempActUp = activeUploads.get(i);

			String leecherStr = tempActUp.substring(0, tempActUp.indexOf(":"));
			if (leecherStr.equals(friend)) {
				activeUploadsToRemove.add(tempActUp);
			}
		}

		for (String actUp : activeUploadsToRemove) {
			activeUploads.remove(actUp);
			uploads--;
		}
	}

	// =========================================================================
	// ********************************UPLOADS**********************************
	// =========================================================================

	// Return TRUE if
	// --> Entry did not already exist (and was added)
	// --> Enough upload slots are available
	// --> Entry is not in requiredChunks (e.g. I have requested chunk)
	// TRUE implies an upload will be attempted
	public boolean addUpload(NodeId leecher, String chunkIndex) {
		
		if (uploads >= TorrentConfig.MAX_UPLOADS) {
			return false;
		}

		if (requiredChunks.contains(chunkIndex) == true) {
			// requiredChunks contains all the chunks I DO NOT HAVE
			// Can not seed a chunk that I do not have
			return false;
		}

		String entryStr = leecher + ":" + chunkIndex;
		if (activeUploads.contains(entryStr) == true) {
			// Already trying to upload this chunk to this leecher
			return false;
		}

		if (exFriends.contains(leecher) == true) {
			// Node recently left/failed
			// This is probably the result of an old handshake request
			return false;
		}

		activeUploads.add(entryStr);
		uploads++;
		return true;
	}

	// Return TRUE if
	// --> Entry exists in activeUploads
	// --> Upload request can continue
	public boolean hasUpload(NodeId leecher, String chunkStr) {
		
		String entryStr = leecher + ":" + chunkStr;

		if (activeUploads.contains(entryStr) == false) {
			// Failure may have occurred
			return false;
		}

		if (exFriends.contains(leecher.toString()) == true) {
			// activeDownloads should NOT contain seeder if exFriends does
			// This may mean cleanup method, or protocol are incorrect
			
			logger.error(String.format("Peer [%s] Protocol Error in hasUpload." +
										" exFriends & activeDownloads both contain leecher %s",
										this.nodeId, statusStr()));
			return false;
		}

		return true;
	}

	// Return TRUE if
	// --> Entry existed (and was removed)
	// TRUE implies failure/NACK occurred & upload will be terminated
	public boolean cancelUpload(NodeId leecher, Integer chunk) {
		
		String entryStr = leecher + ":" + chunk;
		if (activeUploads.contains(entryStr) == false) {
			// Can not cancel an upload that does not exist
			return false;
		}

		if (exFriends.contains(leecher.toString()) == true) {
			// activeDownloads should NOT contain seeder if exFriends does
			// This may mean cleanup method, or protocol are incorrect
			
			logger.error(String.format("Peer [%s] Protocol Error in cancelUpload." +
										" exFriends & activeDownloads both contain leecher %s",
										this.nodeId, statusStr()));
			return false;
		}

		activeUploads.remove(entryStr);
		uploads--;
		return true;
	}

	// Return TRUE if
	// --> Entry existed in activeUploads (and was removed)
	// TRUE implies upload completed successfully & state has been updated
	public boolean successfulUpload(NodeId leecher, String chunkStr) {
		
		String entryStr = leecher + ":" + chunkStr;

		if (activeUploads.contains(entryStr) == false) {
			// Failure occurred
			return false;
		}

		if (exFriends.contains(leecher.toString()) == true) {
			// activeDownloads should NOT contain seeder if exFriends does
			// This may mean cleanup method, or protocol are incorrect
			
			logger.error(String.format("Peer [%s] Protocol Error in successfulUpload." +
										" exFriends & activeDownloads both contain leecher %s",
										this.nodeId, statusStr()));
			return false;
		}
		
		activeUploads.remove(entryStr);
		uploads--;
		return true;
	}

	// =========================================================================
	// ********************************DOWNLOADS********************************
	// =========================================================================

	// "downloads" only used to decide if request to be sent to Tracker
	public boolean tryTakeDownloadSlot() {
		
		if (downloads < TorrentConfig.MAX_DOWNLOADS) {
			downloads++;
			return true;
		}
		return false;
	}

	// "downloads" only used to decide if request to be sent to Tracker
	public boolean downloadingLastChunks() {
		return (requiredChunks.size() - activeDownloads.size()) <= 0;
	}

	// Return TRUE if
	// --> Entry did not already exist (and was added)
	// --> Enough download slots are available
	// TRUE implies a download will be attempted
	public boolean addDownload(NodeId seeder, String chunkStr) {
		
		if (activeDownloads.size() >= TorrentConfig.MAX_DOWNLOADS) {
			// "downloads" only used to decide if request to be sent to Tracker
			// use activeDownloads.size() to track actual downloads

			// downloads--;
			logger.debug(String.format("Peer [%s] addDownload [SLOTS FULL] [%d] %s",
					this.nodeId, activeDownloads.size(), statusStr()));

			return false;
		}
		
		// Not important who the seeder is, we just do not want duplicates
		if (activeDownContainsChunk(chunkStr) == true) {
			// NOTE: "downloads" is incremented optimistically when requesting
			// chunk from Tracker
			// --> In current implementation Tracker only returns 1 chunk per
			// request
			// --> Tracker may return the same chunk in successive calls if it
			// happens fast enough
			// SO: "downloads" must be decremented if download can not progress

			logger.debug(String.format("Peer [%s] chunk [%s] [DUPLICATE] %s",
					this.nodeId, chunkStr, statusStr()));

			downloads--;
			return false;
		}
		
		// Only allow one download per seeder at any one time (fairness)
		if (activeDownContainsSeeder(seeder.toString()) == true) {
			logger.info(String.format("Peer [%s] already downloading from seeder [%s] %s",
					this.nodeId, seeder.toString(), statusStr()));

			downloads--;
			return false;
		}
		
		if (exFriends.contains(seeder.toString()) == true) {
			// Node recently left/failed
			// Let cleanup-method/failed-handler take care of cleanup
			// Cleanup "downloads" here
			// cleanup-method/failed-handler will not cleanup "downloads", only:
			// --> activeDownloads
			// --> activeUploads
			downloads--;
			return false;
		}

		String entryStr = seeder + ":" + chunkStr;
		activeDownloads.add(entryStr);
		return true;
	}

	// Return TRUE if
	// --> Entry exists
	// --> Handshake succeeded
	// --> Download request can continue
	public boolean hasDownload(NodeId seeder, String chunkStr) {
		
		String entryStr = seeder + ":" + chunkStr;

		// Check for "seeder:chunk" rather than only chunk
		// (activeDownloadsContains())
		// In this case we want to know:
		// --> download has started
		// --> seeder is correct (if not, protocol may be broken)
		if (activeDownloads.contains(entryStr) == false) {
			// Failure may have occurred
			return false;
		}

		if (exFriends.contains(seeder.toString()) == true) {
			// activeDownloads should NOT contain seeder if exFriends does
			// This may mean cleanup method, or protocol are incorrect
			
			logger.error(String.format("Peer [%s] Protocol Error in hasDownload." +
										" exFriends & activeDownloads both contain seeder %s",
										this.nodeId, statusStr()));
			return false;
		}
		
		
		return true;
	}

	// Return TRUE if
	// --> Entry existed (and was removed)
	// TRUE implies failure/NACK occurred & download will be terminated
	public boolean cancelDownload(NodeId seeder, String chunkStr) {
		
		String entryStr = seeder + ":" + chunkStr;

		if ( activeDownloads.contains(entryStr) == false ) {
			// TODO Alex: this IF was commented out for some reason
			// --> Maybe it is incorrect, but I can't remember why now...?
			
			// Nothing to cancel
			// May have been cleaned up already due to leave/failure
			return false;
		}

		if (exFriends.contains(seeder.toString()) == true) {
			// activeDownloads should NOT contain seeder if exFriends does
			// This may mean cleanup method, or protocol are incorrect
			
			logger.error(String.format("Peer [%s] Protocol Error in cancelDownload." +
										" exFriends & activeDownloads both contain seeder %s",
										this.nodeId, statusStr()));
			return false;
		}
		
		activeDownloads.remove(entryStr);
		downloads--;
		return true;
	}

	// Return TRUE if
	// --> Entry existed in activeDownloads (and was removed)
	// --> Entry existed in requiredChunks (and was removed)
	// TRUE implies download completed succcessfully & state has been updated
	public boolean successfulDownload(NodeId seeder, String chunkStr) {
		
		String entryStr = seeder + ":" + chunkStr;

		if (activeDownloads.contains(entryStr) == false) {
			// Can't complete download that doesn't exist
			// Failure may have occurred
			return false;
		}

		if (requiredChunks.contains(chunkStr) == false) {
			// Already had piece, no point in storing it again
			// This should never happen
			logger.error(	String.format("Peer [%s] Protocol Error in successfulDownload [%s] %s", 
							chunkStr, chunksStr()));
			return false;
		}

		if (exFriends.contains(seeder.toString()) == true) {
			// activeDownloads should NOT contain seeder if exFriends does
			// This may mean cleanup method, or protocol are incorrect
			
			logger.error(String.format("Peer [%s] Protocol Error in successfulDownload." +
										" exFriends & activeDownloads both contain seeder %s",
										this.nodeId, statusStr()));
			return false;
		}
		
		activeDownloads.remove(entryStr);
		requiredChunks.remove(chunkStr);
		downloads--;
		return true;
	}

	// =========================================================================
	// ********************************MISC*************************************
	// =========================================================================

	public int getRequiredCount() {
		
		return requiredChunks.size();
	}

	public String statusStr() {
		return slotsStr() + chunksStr() + friendsStr();
	}

	private String slotsStr() {
		return "UP[" + uploads + "/" + TorrentConfig.MAX_UPLOADS + "] "
				+ "DOWN[" + downloads + "/" + TorrentConfig.MAX_DOWNLOADS
				+ "] ";
	}

	public String chunksStr() {
		String result = "";
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {

			if (requiredChunks.contains(Integer.toString(i)) == false) {
				// have already
				result += "+";
			} else if (activeDownContainsChunk(Integer.toString(i)) == true) {
				// downloading
				result += "~";
			} else {
				// need
				result += " ";
			}
		}

		if (result.length() > 0) {
			result = "Status [" + result + "]";
		} else {
			result = "Status [ERR]";
		}

		return result;
	}

	public String friendsStr() {
		String result = "";

		for (String friend : friends) {
			result += friend + ":";
		}

		if (result.length() > 0) {
			result = "[" + result + "]";
		} else {
			result = "[]";
		}

		return result;
	}

	private boolean activeDownContainsChunk(String chunkIndex) {
		for (int i = 0; i < activeDownloads.size(); i++) {
			String entryStr = activeDownloads.get(i);
			String chunkStr = entryStr.substring(entryStr.indexOf(":") + 1);
			if (chunkIndex.equals(chunkStr)) {
				return true;
			}
		}
		return false;
	}

	private boolean activeDownContainsSeeder(String seeder) {
		for (int i = 0; i < activeDownloads.size(); i++) {
			String entryStr = activeDownloads.get(i);
			String seederStr = entryStr.substring(0,entryStr.indexOf(":"));
			if (seeder.equals(seederStr)) {
				return true;
			}
		}
		return false;
	}

	public String selectNextChunkFrom() {
		String result = "";
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			if (	requiredChunks.contains(Integer.toString(i)) == true
					&& activeDownContainsChunk(Integer.toString(i)) == false) {
				result += i + ":";
			}
		}

		if (result.length() > 0) {
			result = result.substring(0, result.length() - 1);
		} else {
			result = null;
		}

		return result;
	}
}
