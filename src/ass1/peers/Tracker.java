package ass1.peers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
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
	private GlobalFileStatus fileStatus = new GlobalFileStatus(
			TorrentConfig.CHUNK_COUNT, TorrentConfig.SEED);

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
		logger.error(String.format("Tracker [%s] joined [%d]", this.nodeId,
				currentTime));
	}

	// ----------------------------------------------------------------------------------
	public void leave(long currentTime) {
		logger.error(String.format("Tracker [%s] leaving [%d]", this.nodeId,
				currentTime));
		this.sendSim(new Message("LEAVE_GRANTED", null));
	}

	// ----------------------------------------------------------------------------------
	// FIXME maybe Tracker can register EVERY peer so it can remove them from
	// FileStatus?
	public void failure(NodeId failedId, long currentTime) {
		logger.debug(String.format(
				"Tracker [%s] detects failure of [%s] at time [%d]... BOOM!",
				this.nodeId, failedId, currentTime));

		// FIXME Uwe/Alex check if this is correct
		logger.debug(String.format("Tracker [%s] %s", this.nodeId, fileStatus
				.removeSeeder(failedId)));
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
			// TODO should we have any signals to our nodes?
			// Maybe useful later during experiments
			// Maybe print current file status for debugging purpose
			break;
		default:
			logger.warn(String.format("Tracker [%s] gets unknown signal [%d]",
					this.nodeId, signal));
		}
	}

	// ----------------------------------------------------------------------------------
	private void handleRegisterEvent(NodeId srcId) {
		Boolean[] buffer = new Boolean[TorrentConfig.CHUNK_COUNT];
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			buffer[i] = new Boolean(Boolean.FALSE);
		}

		this.failureDetector.register(srcId, this.nodeId);

		logger.debug(String.format("Tracker [%s] registered Leecher [%s]",
				this.nodeId, srcId));
	}

	// ----------------------------------------------------------------------------------
	private void handleRegisterSeedEvent(NodeId srcId) {
		Boolean[] buffer = new Boolean[TorrentConfig.CHUNK_COUNT];
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			buffer[i] = new Boolean(Boolean.TRUE);
			fileStatus.addSeeder(i, srcId);
		}

		failureDetector.register(srcId, this.nodeId);

		logger.debug(String.format("Tracker [%s] registered Seeder [%s]",
				this.nodeId, srcId));
	}

	// ----------------------------------------------------------------------------------
	private void handlePutChunkEvent(NodeId srcId, Message data) {
		Integer index = Integer.parseInt(data.data);

		fileStatus.addSeeder(index, srcId);

		logger.debug(String.format(
				"Tracker [%s] put chunk [%d] for peer [%s]%n%s", this.nodeId,
				index, srcId, fileStatus));
	}

	// ----------------------------------------------------------------------------------
	private void handleGetChunkReqEvent(NodeId srcId, Message data) {
		ArrayList<Integer> selectNextChunkFrom = selectNextChunkFrom(data.data);
		String chunkAndSeeder = fileStatus.getRandomFrom(selectNextChunkFrom);

		this.sendMsg(srcId, new Message("GET_CHUNK_RESP", chunkAndSeeder));

		logger.debug(String.format(
				"Tracker [%s] sent chunk response [%s] to [%s]", this.nodeId,
				chunkAndSeeder, srcId));
	}

	// ----------------------------------------------------------------------------------
	public void registerEvents() {

		this.addEventListener(new String("REGISTER"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleRegisterEvent(srcId);
			}
		});

		this.addEventListener(new String("REGISTER_SEED"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleRegisterSeedEvent(srcId);
					}
				});

		this.addEventListener(new String("PUT_CHUNK"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handlePutChunkEvent(srcId, data);
			}
		});

		this.addEventListener(new String("GET_CHUNK_REQ"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleGetChunkReqEvent(srcId, data);
					}
				});
	}

	// ----------------------------------------------------------------------------------
	// FIXME Uwe/Alex find out what this does
	// Is this used to:
	// --> Simulate crash/recovery?
	// --> ...?
	public void restore(String str) {
		// String friendsList = PatternMatching.getStrValue(str, "friends:");
		// String friendParts[] = friendsList.split(",");
		// for (int i = 0; i < friendParts.length; i++)
		// this.friends.addElement(friendParts[i]);
		//		
		// String failedList = PatternMatching.getStrValue(str, "failed:");
		// String failedParts[] = failedList.split(",");
		// for (int i = 0; i < failedParts.length; i++)
		// this.failedFriends.addElement(failedParts[i]);
	}

	// ----------------------------------------------------------------------------------
	public void syncMethod(long currentTime) {
		// TODO Uwe/Alex find out if Tracker needs any "polling" type activity
		// Alex: can't think of anything, Tracker seems to just be a passive
		// server
	}

	// ----------------------------------------------------------------------------------
	public String toString() {
		// TODO Use this somewhere?
		// Alex: For debugging if necessary, but its formatting can be improved
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
}
