package ass1.peers;

import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

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
	static Logger logger = Logger.getLogger(Tracker.class);

	// Directory of where to get next Chunks
	private GlobalFileStatus fileStatus = 
		new GlobalFileStatus(TorrentConfig.CHUNK_COUNT, TorrentConfig.SEED);
	
	private int joins = 0;
	private int leaves = 0;
	private int failures = 0;
	private int sentMessages = 0;
	private long startTime = 0;

	// ----------------------------------------------------------------------------------
	public void init(NodeId nodeId, AbstractLink link, Bandwidth bandwidth,
			FailureDetector failureDetector, OverlayNetwork overlay,
			Monitor monitor) {
		super.init(TorrentConfig.TRACKER, link, bandwidth, failureDetector,
				overlay, monitor);
	}

	// ----------------------------------------------------------------------------------
	public void create(long currentTime) {
		logger.debug(String.format("Tracker [%s] created [%d]", this.nodeId,
				currentTime));
		this.overlay.add(this.nodeId);
	}

	// ----------------------------------------------------------------------------------
	public void join(long currentTime) {
		logger.error(String.format("ERROR! Tracker [%s] joined [%d]", this.nodeId,
				currentTime));
	}


	// ----------------------------------------------------------------------------------
	// From Peer
	// Notification that they have left the network
	private void handleStartExperimentEvent(NodeId srcId) {
		logger.debug(String.format("Peer [%s] START_EXPERIMENT", this.nodeId));
	}
	
	// ----------------------------------------------------------------------------------
	public void leave(long currentTime) {
		logger.error(String.format("ERROR! Tracker [%s] leaving [%d]", this.nodeId,
				currentTime));
		this.sendSim(new Message("LEAVE_GRANTED", null));
	}

	// ----------------------------------------------------------------------------------
	// From Peer
	// Notification that they have left the network
	private void handleLeaveEvent(NodeId srcId) {
		leaves++;

		logger.debug(String.format(
				"Tracker [%s] detects leave of [%s]... BYE!",
				this.nodeId, srcId));

		String stateChanges = fileStatus.removeSeeder(srcId);
		
		logger.info(String.format("Tracker [%s] %s", this.nodeId, stateChanges));
	}
	
	// ----------------------------------------------------------------------------------
	public void failure(NodeId failedId, long currentTime) {
		failures++;

		logger.debug(String.format(
				"Tracker [%s] detects failure of [%s] at time [%d]... BOOM!",
				this.nodeId, failedId, currentTime));

		String stateChanges = fileStatus.removeSeeder(failedId);
		
		logger.info(String.format("Tracker [%s] %s", this.nodeId, stateChanges));
	}

	// ----------------------------------------------------------------------------------
	public void receive(NodeId srcId, Message data, long currentTime) {
		if (this.listeners.containsKey(data.type))
			this.listeners.get(data.type).receivedEvent(srcId, data);
		else
			logger.error(String.format(
					"Tracker [%s] has event [%s] not registered", this.nodeId,
					data.type));
	}

	// ----------------------------------------------------------------------------------
	public void signal(int signal, long currentTime) {
		switch (signal) {
		case 1:
			// Inform all Peers to Start Experiment
			logger.debug(String.format("Peer [%s] Received Signal [%d]",
					this.nodeId, signal));
			this.startTime = currentTime;
			this.broadcast(new Message("START_EXPERIMENT", null));
			break;
		default:
			logger.warn(String.format("Tracker [%s] gets unknown signal [%d]",
					this.nodeId, signal));
		}
	}

	// ----------------------------------------------------------------------------------
	private void handleRegisterEvent(NodeId srcId) {
		joins++;
		
		Boolean[] buffer = new Boolean[TorrentConfig.CHUNK_COUNT];
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			buffer[i] = Boolean.valueOf(true);
		}

		this.failureDetector.register(srcId, this.nodeId);

		logger.info(String.format("Tracker [%s] registered Leecher [%s]",
				this.nodeId, srcId));
	}

	// ----------------------------------------------------------------------------------
	private void handleRegisterSeedEvent(NodeId srcId) {
		joins++;
		
		Boolean[] buffer = new Boolean[TorrentConfig.CHUNK_COUNT];
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			buffer[i] = Boolean.valueOf(true);
			fileStatus.addSeeder(i, srcId);
		}

		failureDetector.register(srcId, this.nodeId);

		logger.info(String.format("Tracker [%s] registered Seeder [%s]",
				this.nodeId, srcId));
	}

	// ----------------------------------------------------------------------------------
	private void handlePutChunkEvent(NodeId srcId, Message data) {
		Integer index = Integer.parseInt(data.data);

		if ( fileStatus.addSeeder(index, srcId) ) {
			logger.info(String.format(
					"Tracker [%s] put chunk [%d] for peer [%s]%n%s", this.nodeId,
					index, srcId, fileStatus));
		}

	}

	// ----------------------------------------------------------------------------------
	private void handleGetChunkReqEvent(NodeId srcId, Message data) {
		String chunksStr = data.data.substring(0, data.data.indexOf("-"));
		String requestsStr = data.data.substring(data.data.indexOf("-") + 1);
		int requests = Integer.parseInt(requestsStr);
		
		ArrayList<Integer> selectNextChunkFrom = selectNextChunkFrom(chunksStr);		
		ArrayList<String> responses = fileStatus.getRandomFrom(selectNextChunkFrom, requests);
		
		if (responses == null) {
			// Do nothing here, send nothing back to Peer, this results in:
			// --> Peer will wait forever
			// --> Peer will make no progress and never become Seeder
			// This is OK because:
			// --> If Tracker can not find a Seeder for Chunk, there is no Seeder
			
			// FIXME: maybe a Timeout is better, because there is SLIGHT possibility of:
			// --> Seeder uploaded Chunk to Leecher successfully
			// --> Seeder failed
			// --> Leecher sent Put Chunk to Tracker
			// --> Tracker detected failure and cleaned up state
			// --> Tracker received this GetChunkReq (but there is no Seeder)
			// --> Tracker received Put message
			// --> Leecher becomes Seeder for that Chunk (now there is Seeder)
			
			// TODO: Put timeout on Tracker
			// --> Timeout is 2x longer than Failure Detector period
			// --> If Chunk available after that period, send reply
			// --> If Chunk NOT available after that period, do nothing
			
			logger.error(String.format(
					"Tracker [%s] no Seeder found for [%s] to [%s]", 
					this.nodeId, data.data, srcId));
			
			return;
		}
		
		for (String response : responses) {
			sentMessages++;
			this.sendMsg(srcId, new Message("GET_CHUNK_RESP", response));

			logger.info(String.format(
					"Tracker [%s] sent chunk response [%s] to [%s]", this.nodeId,
					response, srcId));
		}

	}

	// ----------------------------------------------------------------------------------
	public void registerEvents() {

		this.addEventListener("REGISTER", new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleRegisterEvent(srcId);
			}
		});

		this.addEventListener("REGISTER_SEED",
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleRegisterSeedEvent(srcId);
					}
				});

		this.addEventListener("PUT_CHUNK", new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handlePutChunkEvent(srcId, data);
			}
		});

		this.addEventListener("GET_CHUNK_REQ",
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleGetChunkReqEvent(srcId, data);
					}
				});
		
		this.addEventListener("LEAVE",
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleLeaveEvent(srcId);
					}
				});
		
		this.addEventListener("START_EXPERIMENT",
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleStartExperimentEvent(srcId);
					}
				});
		
	}

	// ----------------------------------------------------------------------------------
	public void restore(String str) {
	}

	// ----------------------------------------------------------------------------------
	public void syncMethod(long currentTime) {
	}

	// ----------------------------------------------------------------------------------
	public String toString() {
		// For debugging if necessary
		return fileStatus.toString();
	}

	// ----------------------------------------------------------------------------------
	public ArrayList<Integer> selectNextChunkFrom(String s) {
		ArrayList<Integer> result = new ArrayList<Integer>();

		StringTokenizer st = new StringTokenizer(s, ":");
		while (st.hasMoreTokens()) {
			result.add(Integer.parseInt(st.nextToken()));
		}

		return result;
	}
	
	public int getJoins() {
		return joins;
	}

	public int getLeaves() {
		return leaves;
	}

	public int getFailures() {
		return failures;
	}

	public int getSentMessages() {
		return sentMessages;
	}

	public long getStartTime() {
		return startTime;
	}
}

