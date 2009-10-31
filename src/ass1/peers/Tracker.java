package ass1.peers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import ass1.common.TorrentConfig;
import ass1.file.GlobalFileStatus;

import sicsim.network.core.Bandwidth;
import sicsim.network.core.Monitor;
import sicsim.network.core.OverlayNetwork;
import sicsim.network.core.FailureDetector;
import sicsim.network.links.AbstractLink;
import sicsim.network.peers.AbstractPeer;
import sicsim.network.peers.PeerEventListener;
import sicsim.types.Message;
import sicsim.types.NodeId;

public class Tracker extends AbstractPeer {
	// Chunks peers already have
	// TODO can this be removed? maybe necessary when failures are considered?
	private HashMap<NodeId, Boolean[]> peers = new HashMap<NodeId, Boolean[]>();
	
	// Directory of where to get next Chunks <-- faster lookup than "peers"
	//TODO get seed from?
	private GlobalFileStatus fileStatus = new GlobalFileStatus(TorrentConfig.CHUNK_COUNT, TorrentConfig.SEED); 
	
//----------------------------------------------------------------------------------
	public void init(NodeId nodeId, AbstractLink link, Bandwidth bandwidth, FailureDetector failureDetector, OverlayNetwork overlay, Monitor monitor) {
		super.init(TorrentConfig.TRACKER, link, bandwidth, failureDetector, overlay, monitor);
	}

//----------------------------------------------------------------------------------
	public void create(long currentTime) {
		System.out.println("Tracker [" + this.nodeId + "] Created [" + currentTime + "]");
		this.overlay.add(this.nodeId);
	}
	
//----------------------------------------------------------------------------------
	public void join(long currentTime) {
		System.out.println("ERROR! Tracker [" + this.nodeId + "] Joined [" + currentTime + "]");
	}	
	
//----------------------------------------------------------------------------------
	public void leave(long currentTime) {
		System.out.println("ERROR! Tracker [" + this.nodeId + "] Leaving [" + currentTime + "]");
		this.sendSim(new Message("LEAVE_GRANTED", null));
	}

//----------------------------------------------------------------------------------
	// TODO maybe Tracker can register EVERY peer so it can remove them from FileStatus?
	public void failure(NodeId failedId, long currentTime) {
		System.out.println(	"Tracker [" + this.nodeId + "] " + 
							"Detects Failure Of [" + failedId + "] " + 
							"At Time [" + currentTime + "]");
		//TODO remove failed node from FileStatus?
	}
	
//----------------------------------------------------------------------------------
	public void receive(NodeId srcId, Message data, long currentTime) {
		if (this.listeners.containsKey(data.type))
			this.listeners.get(data.type).receivedEvent(srcId, data);
		else
			System.out.println(	"ERROR! Tracker [" + this.nodeId + "] " + 
								"Event [" + data.type + "] " + 
								"Not Registered");
	}
	
//----------------------------------------------------------------------------------
	//TODO implement
	public void signal(int signal, long currentTime) {
		switch (signal) {
		case 1:
			//TODO print current file status?
			//this.sendMsg(randomNode, new Message("SIGNAL", data2));
			break;
		default:
			System.out.println("Tracker [" + this.nodeId + "] " +
					"Unknown Signal [" + signal + "]");				
		}
	}

//----------------------------------------------------------------------------------
	private void handleRegisterEvent(NodeId srcId) {
		Boolean[] buffer = new Boolean[TorrentConfig.CHUNK_COUNT];
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			buffer[i] = new Boolean(Boolean.FALSE);
		}
		
		this.failureDetector.register(srcId, this.nodeId);
		
		this.peers.put(srcId, buffer);
		
		System.out.println(	"Tracker Registered " + 
							"Leecher [" + srcId + "]");
	}

//----------------------------------------------------------------------------------
	private void handleRegisterSeedEvent(NodeId srcId) {
		Boolean[] buffer = new Boolean[TorrentConfig.CHUNK_COUNT];
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			buffer[i] = new Boolean(Boolean.TRUE);
			fileStatus.addSeeder(i, srcId);
		}
		
		failureDetector.register(srcId, this.nodeId);
		
		peers.put(srcId, buffer);
		
		System.out.println(	"Tracker Registered " + 
							"Seeder [" + srcId + "]");
}

//----------------------------------------------------------------------------------
	private void handlePutChunkEvent(NodeId srcId, Message data) {
		Integer index = Integer.parseInt(data.data);
		
		fileStatus.addSeeder(index, srcId);
		
		Boolean[] peerStatus = peers.get(srcId);
		peerStatus[index] = true;
		peers.put(srcId, peerStatus);
		
		System.out.println(	"Tracker Put " + 
							"Chunk [" + index + "] " + 
							"For Peer [" + srcId + "]");
	}
	
//----------------------------------------------------------------------------------
	private void handleGetChunkReqEvent(NodeId srcId, Message data) {
		ArrayList<Integer> selectNextChunkFrom = selectNextChunkFrom(data.data);		
		String chunkAndSeeder = fileStatus.getRandomFrom(selectNextChunkFrom);
		
		this.sendMsg(srcId,new Message("GET_CHUNK_RESP", chunkAndSeeder) );

//		System.out.println(	"Tracker Sent Chunk Resp [" + chunkAndSeeder + "] " + 
//							"To [" + srcId + "]");
	}
	
//----------------------------------------------------------------------------------
	public void registerEvents() {

		this.addEventListener(new String("REGISTER"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleRegisterEvent(srcId);
			}
		});

		this.addEventListener(new String("REGISTER_SEED"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleRegisterSeedEvent(srcId);
			}
		});
		
		this.addEventListener(new String("PUT_CHUNK"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handlePutChunkEvent(srcId, data);
			}
		});
		
		this.addEventListener(new String("GET_CHUNK_REQ"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleGetChunkReqEvent(srcId, data);
			}
		});
	}

//----------------------------------------------------------------------------------
	//TODO what does this do?
	public void restore(String str) {
//		String friendsList = PatternMatching.getStrValue(str, "friends:");
//		String friendParts[] = friendsList.split(",");
//		for (int i = 0; i < friendParts.length; i++)
//			this.friends.addElement(friendParts[i]);
//		
//		String failedList = PatternMatching.getStrValue(str, "failed:");
//		String failedParts[] = failedList.split(",");
//		for (int i = 0; i < failedParts.length; i++)
//			this.failedFriends.addElement(failedParts[i]);
	}

//----------------------------------------------------------------------------------
	public void syncMethod(long currentTime) {
	}

//----------------------------------------------------------------------------------
	public String toString() {
    	return fileStatus.toString();
	}
//----------------------------------------------------------------------------------
	public ArrayList<Integer> selectNextChunkFrom(String s) {
		ArrayList<Integer> result = new ArrayList<Integer>();
		
		StringTokenizer st = new StringTokenizer(s, ":");
		while (st.hasMoreTokens()) {
			result.add( Integer.parseInt(st.nextToken()) );
		}

		return result;
	}
}
