package ass1.protocol;

import java.util.ArrayList;
import ass1.common.TorrentConfig;
import sicsim.types.NodeId;

public class TorrentProtocol {
	int uploads = 0;
	int downloads = 0;
	
	ArrayList<String> friends = new ArrayList<String>();			//Nodes I know
	ArrayList<String> activeUploads = new ArrayList<String>();	//"seeder:chunk"	
	ArrayList<String> activeDownloads = new ArrayList<String>();	//"seeder:chunk"	
	ArrayList<String> requiredChunks = new ArrayList<String>();		//chunkIndexStr
	
	NodeId nodeId = null;
	
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
	// --> --> multiple failureDetector.register(peer) causes multiple notifications (dumb)
	public boolean addFriend(String friend) {
		if ( friends.contains(friend) == false ) {			
			friends.add(friend);
			return true;
		}
		
		return false;
	}
	
	//=========================================================================
	//********************************CLEANUP**********************************
	//=========================================================================
	
	public void cleanupFriendFailure(String friend) {
		// Cleanup activeDownloads
		cleanupActiveDownloads(friend);
		
		// Cleanup hesitantUploads
		cleanupActiveUploads(friend);
	}
	
	private void cleanupActiveDownloads(String friend) {
		// Cleanup activeDownloads
		ArrayList<String> activeDownloadsToRemove = new ArrayList<String>();
		
		for (int i = 0; i < activeDownloads.size(); i++) {
			String tempActDown = activeDownloads.get(i);
			
			String seederStr = tempActDown.substring(0, tempActDown.indexOf(":") );
			if ( seederStr.equals(friend) ) {
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
			
			String leecherStr = tempActUp.substring(0, tempActUp.indexOf(":") );
			if ( leecherStr.equals(friend) ) {
				activeUploadsToRemove.add(tempActUp);
			}
		}

		for (String actUp : activeUploadsToRemove) {
			activeUploads.remove(actUp);
			uploads--;		
		}
	}
	
	//=========================================================================
	//********************************UPLOADS**********************************
	//=========================================================================
	
	// Return TRUE if 
	// --> Entry did not already exist (and was added)
	// --> Enough upload slots are available
	// --> Entry is not in requiredChunks (e.g. I have requested chunk)
	// TRUE implies an upload will be attempted
	public boolean addUpload(NodeId leecher, String chunkIndex) {
		if ( uploads >= TorrentConfig.MAX_UPLOADS ) {
			return false;
		}
		
		if ( requiredChunks.contains(chunkIndex) == true ) {
			// requiredChunks contains all the chunks I DON NOT HAVE
			// Can not seed a chunk that I do not have
			return false;
		}
		
		String entryStr = leecher + ":" + chunkIndex; 
		if ( activeUploads.contains(entryStr) == true ) {
			// Already trying to upload this chunk to this leecher
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
		
		if ( activeUploads.contains(entryStr) == false ) {
			// Failure may have occurred
			return false;
		}
		
		return true;
	}
	
	// Return TRUE if 
	// --> Entry existed (and was removed)
	// TRUE implies failure/NACK occurred & upload will be terminated
	public boolean cancelUpload(NodeId leecher, Integer chunk) {
		String entryStr = leecher + ":" + chunk; 
		if ( activeUploads.contains(entryStr) == false ) {
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
	
		if ( activeUploads.contains(entryStr) == false ) {
			// Failure occurred
			return false;
		}
	
		activeUploads.remove(entryStr);
		uploads--;
		return true;
	}

	//=========================================================================
	//********************************DOWNLOADS********************************
	//=========================================================================
	
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
		if ( activeDownloads.size() >= TorrentConfig.MAX_DOWNLOADS ) {
			// "downloads" only used to decide if request to be sent to Tracker
			// use activeDownloads.size() to track actual downloads
			
//			downloads--;
			System.out.println(	"PROTOCOL ERROR! Peer [" + this.nodeId + "] " +
								"SLOTS FULL [" + activeDownloads.size() + "] " +
								statusStr());
			
			return false;
		}
		
		String entryStr = seeder + ":" + chunkStr;
		
		// Not important who the seeder is, we just do not want duplicates
		if ( activeDownContains(chunkStr) == true ) {			
			// NOTE: "downloads" is incremented optimistically when requesting chunk from Tracker
			// --> In current implementation Tracker only returns 1 chunk per request
			// --> Tracker may return the same chunk in successive calls if it happens fast enough
			// SO: "downloads" must be decremented if download can not progress
			
			System.out.println(	"Peer [" + this.nodeId + "] [DUPLICATE] " +
								statusStr());
			
			downloads--;
			return false;
		}
		
		activeDownloads.add(entryStr);
		return true;
	}	
	
	// Return TRUE if 
	// --> Entry exists 
	// --> Handshake succeeded
	// --> Download request can continue
	public boolean hasDownload(NodeId seeder, String chunkStr) {
		String entryStr = seeder + ":" + chunkStr;
		
		// Check for "seeder:chunk" rather than only chunk (activeDownloadsContains())
		// In this case we want to know: 
		// --> download has started
		// --> seeder is correct (if not, protocol may be broken)
		if ( activeDownloads.contains(entryStr) == false ) {
			// Failure may have occurred
			return false;
		}
		
		return true;
	}
	
	// Return TRUE if 
	// --> Entry existed (and was removed)
	// TRUE implies failure/NACK occurred & download will be terminated
	public boolean cancelDownload(NodeId seeder, String chunkStr) {
		String entryStr = seeder + ":" + chunkStr;
		
//		if ( activeDownloads.contains(entryStr) == false ) {
//			// Nothing to cancel
//			return false;
//		}
		
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
		
		if ( activeDownloads.contains(entryStr) == false ) {
			// Can't complete download that doesn't exist
			// Failure may have occurred
			return false;
		}
		
		if ( requiredChunks.contains(chunkStr) == false ) {
			// Already had piece, no point in storing it again
			// This should never happen
			System.out.println(	"PROTOCOL ERROR! successfulDownload [" + chunkStr + "] " +
								chunksStr());
			return false;
		}
		
		activeDownloads.remove(entryStr);
		requiredChunks.remove(chunkStr);
		downloads--;		
		return true;
	}
	
	//=========================================================================
	//********************************MISC*************************************
	//=========================================================================
	
	public int getRequiredCount() {
		return requiredChunks.size();
	}
	
	public String statusStr() {
		return 	slotsStr() + chunksStr() + friendsStr(); 
	}

	private String slotsStr() {
		return 	"UP[" + uploads + "/" + TorrentConfig.MAX_UPLOADS + "] " +
				"DOWN[" + downloads + "/" + TorrentConfig.MAX_DOWNLOADS + "] ";
	}
	
	private String chunksStr() {
		String result = "";
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {

			if ( requiredChunks.contains(Integer.toString(i)) == false )  {
				// have already
				result += "+";
			} else if ( activeDownContains(Integer.toString(i)) == true )  {
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
	
	private String friendsStr() {
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
	
	private boolean activeDownContains(String chunkIndex) {
		for (int i = 0; i < activeDownloads.size(); i++) {
			String entryStr = activeDownloads.get(i);
			String chunkStr = entryStr.substring(entryStr.indexOf(":") + 1);
			if ( chunkIndex.equals(chunkStr) ) {
				return true;
			}
		}
		return false;
	}
	
	public String selectNextChunkFrom() {
		String result = "";
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			if ( 	requiredChunks.contains(Integer.toString(i)) == true &&
					activeDownContains(Integer.toString(i)) == false ) {
//					activeDownloads.contains(Integer.toString(i)) == false )  {
				result += i + ":";
			}
		}
		
		if (result.length() > 0) {
			result = result.substring(0, result.length()-1 );
		} else {
			result = null;
		}
		 
		return result;  
	}
}
