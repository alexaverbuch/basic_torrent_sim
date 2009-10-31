package ass1.protocol;

import java.util.ArrayList;
import ass1.common.TorrentConfig;
import sicsim.types.NodeId;

public class TorrentProtocol {
	int downloaders = 0;
	int uploaders = 0;
	
	ArrayList<String> hesitantDownloads = new ArrayList<String>();	//"leecher:chunk"
	ArrayList<String> hesitantUploads = new ArrayList<String>();	//"seeder:chunk"	
	ArrayList<String> activeDownloads = new ArrayList<String>();	//"seeder:chunk"	
	ArrayList<String> requiredChunks = new ArrayList<String>();		//chunkIndex
	
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
	
	// Return TRUE if 
	// --> Entry did not already exist (and was added)
	// --> Enough upload (downloaders) slots are available
	// --> Entry is not in requiredChunks (e.g. I have requested chunk)
	// TRUE implies an upload will be attempted
	public boolean addHesitantUpload(NodeId leecher, Integer chunk) {
		if ( downloaders >= TorrentConfig.MAX_NUM_DOWNLOADERS ) {
			return false;
		}
		
		if ( requiredChunks.contains(chunk.toString()) == true ) {
			return false;
		}
		
		String entryStr = leecher + ":" + chunk; 
		if ( hesitantUploads.contains(entryStr) == true ) {
			// Already trying to upload this chunk to this leecher
			return false;
		}
		
		hesitantUploads.add(entryStr);
		downloaders++;		
		return true;
	}	
	
	// Return TRUE if 
	// --> Entry existed (and was removed)
	// TRUE implies timeout occurred & upload will be terminated
	public boolean cancelHesitantUpload(NodeId leecher, Integer chunk) {
		String entryStr = leecher + ":" + chunk; 
		if ( hesitantUploads.contains(entryStr) == false ) {
			return false;
		}
		
		hesitantUploads.remove(entryStr);
		downloaders--;		
		return true;
	}
	
	// Return TRUE if 
	// --> Entry existed (and was removed)
	// TRUE implies timeout had not yet occurred & upload will begin as planned
	public boolean commitToUpload(NodeId leecher, Integer chunk) {
		String entryStr = leecher + ":" + chunk; 
		if ( hesitantUploads.contains(entryStr) == false ) {
			// Timeout has already occurred, fail
			return false;
		}
		
		hesitantUploads.remove(entryStr);
		return true;
	}
	
	public void decUploads() {
		this.downloaders--;
	}
	
	// Return TRUE if 
	// --> Entry did not already exist (and was added)
	// --> Enough download slots (uploaders) are available
	// TRUE implies a download will be attempted
	public boolean addHesitantDownload(NodeId seeder, Integer chunk) {
		// TODO seeder not used at present, maybe this will need to change
		// Each chunk should only be downloaded form one seeder in this protocol

		if ( uploaders >= TorrentConfig.MAX_NUM_UPLOADERS ) {
			return false;
		}
		
//		String entryStr = seeder + ":" + chunk;
		
//		if ( hesitantDownloads.contains(entryStr) == true ) {
		if ( hesitantDownloads.contains(chunk.toString()) == true ) {
			// Already trying to download this chunk
			return false;
		}
		
//		if ( activeDownloads.contains(entryStr) == true ) {
		if ( activeDownloads.contains(chunk.toString()) == true ) {
			// Download of this chunk is in progress already 
			return false;
		}
		
//		hesitantDownloads.add(entryStr);
		hesitantDownloads.add(chunk.toString());
		uploaders++;		
		return true;
	}	
	
	// Return TRUE if 
	// --> Entry exists 
	// --> Handshake succeeded
	// --> Timeout has not occurred yet
	// --> Download request can continue
	public boolean hasHesitantDownload(NodeId seeder, Integer chunk) {
		// TODO seeder not used at present, maybe this will need to change
		// Each chunk should only be downloaded form one seeder in this protocol
		
//		String entryStr = seeder + ":" + chunk;
		
//		if ( hesitantDownloads.contains(entryStr) == false ) {
		if ( hesitantDownloads.contains(chunk.toString()) == false ) {
			// Timeout must have already occurred
			return false;
		}
		
		return true;
	}
	
	// Return TRUE if 
	// --> Entry existed (and was removed)
	// TRUE implies timeout occurred & download will be terminated
	public boolean cancelHesitantDownload(NodeId seeder, Integer chunk) {
		// TODO seeder not used at present, maybe this will need to change
		// Each chunk should only be downloaded form one seeder in this protocol
		
//		String entryStr = seeder + ":" + chunk;
		
//		if ( hesitantDownloads.contains(entryStr) == false ) {
		if ( hesitantDownloads.contains(chunk.toString()) == false ) {
			// Progress was already made, good
			return false;
		}
		
//		hesitantDownloads.remove(entryStr);
		hesitantDownloads.remove(chunk.toString());
		uploaders--;		
		return true;
	}
	
	// Return TRUE if 
	// --> Entry existed in hesitantDownloads (and was removed)
	// TRUE implies timeout had not yet occurred & download will begin as planned
	public boolean commitToDownload(NodeId seeder, Integer chunk) {
		// TODO seeder not used at present, maybe this will need to change
		// Each chunk should only be downloaded from one seeder in this protocol
		
//		String entryStr = seeder + ":" + chunk;
		
//		if ( hesitantDownloads.contains(entryStr) == false ) {
		if ( hesitantDownloads.contains(chunk.toString()) == false ) {
			// Timeout has already occurred, fail
			return false;
		}
		
//		if ( activeDownloads.contains(entryStr) == true ) {
		if ( activeDownloads.contains(chunk.toString()) == true ) {
			// Download of this chunk is already in progress
			return false;
		}
		
//		hesitantDownloads.remove(entryStr);
		hesitantDownloads.remove(chunk.toString());
		
		activeDownloads.add(chunk.toString());
		
		return true;
	}

	// Return TRUE if 
	// --> Entry existed (and was removed)
	// TRUE implies timeout occurred & download will be terminated
	public boolean cancelDownload(NodeId seeder, Integer chunk) {
		// TODO seeder not used at present, maybe this will need to change
		// Each chunk should only be downloaded form one seeder in this protocol
		
//		String entryStr = seeder + ":" + chunk;
		
//		if ( activeDownloads.contains(entryStr) == false ) {
		if ( activeDownloads.contains(chunk.toString()) == false ) {
			// Can't abort a download that doesn't exist
			return false;
		}
		
//		activeDownloads.remove(entryStr);
		activeDownloads.remove(chunk.toString());
		uploaders--;		
		return true;
	}
	
	// Return TRUE if 
	// --> Entry existed in activeDownloads (and was removed)
	// --> Entry existed in requiredChunks (and was removed)
	// TRUE implies download completed succcessfully & state has been updated
	public boolean successfulDownload(NodeId seeder, Integer chunk) {
		// TODO seeder not used at present, maybe this will need to change
		// Each chunk should only be downloaded form one seeder in this protocol
		
//		String entryStr = seeder + ":" + chunk;
		
//		if ( activeDownloads.contains(entryStr) == false ) {
		if ( activeDownloads.contains(chunk.toString()) == false ) {
			// Can't complete download that doesn't exist
			// Either never started, or timed out
			return false;
		}
		
		if ( requiredChunks.contains(chunk.toString()) == false ) {
			// Already had piece, no point in storing it again
			// This should never happen
			return false;
		}
		
//		activeDownloads.remove(entryStr);
		activeDownloads.remove(chunk.toString());
		requiredChunks.remove(chunk.toString());
		uploaders--;		
		return true;
	}
	
	public void decDownloads() {
		this.uploaders--;
	}
	
	public int getRequiredCount() {
		return requiredChunks.size();
	}
	
	public int getNotYetDownloading() {
		return requiredChunks.size() - activeDownloads.size();
	}

	public boolean hasAvailableDownloadSlots() {
		return uploaders < TorrentConfig.MAX_NUM_UPLOADERS;
	}
	
//	public String requiredChunksStr() {
//		String chunksStr = "[-";
//		for (int i = 0; i < requiredChunks.size(); i++) {
//			chunksStr += requiredChunks.get(i) + "-";
//		}
//		chunksStr += "]";
//		return "Required Chunks " + chunksStr;
//	}
	
	public String slotsStr() {
		return 	"Downloaders [" + downloaders + "/" + TorrentConfig.MAX_NUM_DOWNLOADERS + "] " +
				"Uploaders [" + uploaders + "/" + TorrentConfig.MAX_NUM_UPLOADERS + "] ";
	}
	
	public String statusStr() {
		String result = "";
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			if ( requiredChunks.contains(Integer.toString(i)) == false )  {
				// have already
				result += "+";
			} else if ( activeDownloads.contains(Integer.toString(i)) == true )  {
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
	
	public String selectNextChunkFrom() {
		String result = "";
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			if ( 	requiredChunks.contains(Integer.toString(i)) == true &&
					hesitantDownloads.contains(Integer.toString(i)) == false &&
					activeDownloads.contains(Integer.toString(i)) == false )  {
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
