package ass1.peers;

import org.apache.log4j.Logger;

import ass1.common.TorrentConfig;
import ass1.protocol.TorrentProtocol;

import sicsim.network.core.Bandwidth;
import sicsim.network.core.Monitor;
import sicsim.network.core.OverlayNetwork;
import sicsim.network.core.FailureDetector;
import sicsim.network.links.AbstractLink;
import sicsim.network.peers.BandwidthPeer;
import sicsim.network.peers.PeerEventListener;
import sicsim.types.Message;
import sicsim.types.NodeId;

public class Peer extends BandwidthPeer {
	static Logger logger = Logger.getLogger(Peer.class);

	// Protocol logic & state is maintained in this badboy
	public TorrentProtocol protocol;

	// Used to reduce repetitive printouts so output log is shorter
	// Seeder only prints ***SEEDING*** once
	private boolean seeding = false;
	private boolean startExperiment = false;
	private int sentMessages = 0;
	
	// ----------------------------------------------------------------------------------
	public void init(NodeId nodeId, AbstractLink link, Bandwidth bandwidth,
			FailureDetector failureDetector, OverlayNetwork overlay,
			Monitor monitor) {
		super.init(nodeId, link, bandwidth, failureDetector, overlay, monitor,
				TorrentConfig.MAX_UPLOAD_BW, Integer.MAX_VALUE);
		this.failureDetector.register(new NodeId(TorrentConfig.TRACKER),
				this.nodeId);
	}	
	
	// ----------------------------------------------------------------------------------
	public void create(long currentTime) {
		logger.debug(String.format("Peer [%s] created [%d]", this.nodeId,
				currentTime));
	}

	// ----------------------------------------------------------------------------------
	public void join(long currentTime) {
		if (this.overlay.getNodes().size() == 0) {
			logger.error(String.format("ERROR! Peer [%s] joined (before tracker)",
					this.nodeId));
			return;
		}

		if (this.overlay.getNodes().size() == 1) {
			protocol = new TorrentProtocol(nodeId, true);

			sentMessages++;
			this.sendMsg(TorrentConfig.TRACKER, new Message("REGISTER_SEED",
					null));

			logger.debug(String.format("Peer [%s] (Seeder) joined [%d] [%s]",
					this.nodeId, currentTime, protocol.statusStr()));
		} else {
			protocol = new TorrentProtocol(nodeId, false);

			sentMessages++;
			this.sendMsg(TorrentConfig.TRACKER, new Message("REGISTER", null));

			logger.debug(String.format("Peer [%s] (Leecher) joined [%d] %s",
					this.nodeId, currentTime, protocol.statusStr()));
		}

		this.overlay.add(this.nodeId);
	}

	// ----------------------------------------------------------------------------------
	// From Peer
	// Notification that they have left the network
	private void handleStartExperimentEvent(NodeId srcId) {
		this.startExperiment = true;

		logger.debug(String.format("Peer [%s] START_EXPERIMENT", this.nodeId));
	}
	
	// ----------------------------------------------------------------------------------
	public void leave(long currentTime) {
		logger.info(String.format("Peer [%s] Leaving...",this.nodeId));
		
		this.broadcast(new Message("LEAVE", null));
		this.sendSim(new Message("LEAVE_GRANTED", null));	
	}

	// ----------------------------------------------------------------------------------
	// From Peer
	// Notification that they have left the network
	private void handleLeaveEvent(NodeId srcId) {
		this.protocol.cleanupFriendFailure(srcId.toString());

		logger.info(String.format("Peer [%s] detected leave of [%s] %s",
				this.nodeId, srcId, protocol.statusStr()));
	}
	
	// ----------------------------------------------------------------------------------
	public void failure(NodeId failedId, long currentTime) {
		this.protocol.cleanupFriendFailure(failedId.toString());

		logger.info(String.format("Peer [%s] detected failure of [%s] %s",
				this.nodeId, failedId, protocol.statusStr()));
	}
	
	// ----------------------------------------------------------------------------------
	public void receive(NodeId srcId, Message data, long currentTime) {
		if (this.listeners.containsKey(data.type))
			this.listeners.get(data.type).receivedEvent(srcId, data);
		else
			logger.error(String.format("Peer [%s] event [%s] unknown",
					this.nodeId, data.type));
	}

	// ----------------------------------------------------------------------------------
	public void signal(int signal, long currentTime) {
		switch (signal) {
		case 1:
			// Inform all Peers to Start Experiment
			logger.error(String.format("Peer [%s] Received Signal [%d]",
					this.nodeId, signal));
			this.startExperiment = true;
			this.broadcast(new Message("START_EXPERIMENT", null));
			break;
		default:
			logger.error(String.format("Peer [%s] Received Unknown Signal [%d]",
					this.nodeId, signal));
		}
	}

	// ----------------------------------------------------------------------------------
	// This method is called every one step by the simulator
	// Request new Chunk if necessary
	// TODO Uwe/Alex find out where the frequency of this event is configured
	public void syncMethod(long currentTime) {
		
		if (this.startExperiment == false) {
			return;
		}
		
		if (this.protocol.getRequiredCount() == 0) {
			// No need to GET more Chunks, we are seeding
			if (seeding == false) {
				// TODO Save "currentTime for Finish_Time?
				// --> Might make for an interesting plot
				seeding = true;
				logger.info(String.format(
						"Peer [%s] **********SEEDING********** %s",
						this.nodeId, protocol.statusStr()));
			}
			return;
		}

		if (this.protocol.downloadingLastChunks()) {
			// All required chunks are already downloading
			logger.info(String.format("Peer [%s] Downloading Final Chunks... %s",
					this.nodeId, protocol.statusStr()));
			return;
		}

		String selectNextChunkFrom = this.protocol.tryReserveDownloadSlots();

		if (selectNextChunkFrom == null) {
			// All download slots busy
			// Wait until next SYNC event and try again
			return;
		}

		logger.info(String.format("Peer [%s] RequestNext NextFrom [%s] %s",
				this.nodeId, selectNextChunkFrom, protocol.statusStr()));

		sentMessages++;
		this.sendMsg( TorrentConfig.TRACKER, new Message("GET_CHUNK_REQ",
				selectNextChunkFrom));
	}

	// ----------------------------------------------------------------------------------
	// Response from Tracker for Chunk Info
	// Now send Handshake request to Seeder
	private void handleGetChunkRespEvent(NodeId srcId, Message data) {
		String seederStr = data.data.substring(0, data.data.indexOf(":"));
		NodeId seeder = new NodeId(seederStr);

		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);

		if (this.protocol.addDownload(seeder, chunkStr) == false) {
			// Can not handshake with Seeder
			logger.info(String.format("Peer [%s] can not attempt handshake "
					+ "with [%s] for chunk [%s] %s", this.nodeId, seederStr,
					chunkStr, protocol.statusStr()));

			return;
		}

		if (this.protocol.addFriend(seederStr)) {
			this.failureDetector.register(seeder, this.nodeId);
		}

		sentMessages++;
		this.sendMsg(new NodeId(seederStr), new Message("HANDSHAKE_REQ",
				chunkStr));

		logger.info(String.format(
				"Peer [%s] sent handshake request to [%s] for chunk [%s] %s",
				this.nodeId, seederStr, chunkStr, protocol.statusStr()));
	}

	// ----------------------------------------------------------------------------------
	// Receive Handshake request from Peer that wants to Leech a Chunk
	private void handleHandshakeReqEvent(NodeId srcId, Message data) {
		if (protocol.addUpload(srcId, data.data) == false) {
			// Can not upload Chunk to Leecher
			sentMessages++;
			this.sendMsg(srcId, new Message("HANDSHAKE_NACK", data.data));

			logger.info(String.format("Peer [%s] rejected handshake request "
					+ "for chunk [%s] from [%s] %s", this.nodeId, data.data,
					srcId, protocol.statusStr()));

			return;
		}

		if (this.protocol.addFriend(srcId.toString())) {
			this.failureDetector.register(srcId, this.nodeId);
		}

		sentMessages++;
		this.sendMsg(srcId, new Message("HANDSHAKE_ACK", data.data));

		logger.info(String.format("Peer [%s] accepted handshake request for "
				+ "chunk [%s] from " + "[%s] %s", this.nodeId, data.data,
				srcId, protocol.statusStr()));
	}

	// ----------------------------------------------------------------------------------
	// Receive Handshake Ack from Peer that can Seed a Chunk
	private void handleHandshakeAckEvent(NodeId srcId, Message data) {
		if (this.protocol.hasDownload(srcId, data.data) == false) {
			// Can not download Chunk from Seeder
			// Do nothing, Seeder must have failed
			logger.info(String.format("Peer [%s] aborted download for chunk "
					+ "[%s] from [%s] - failure? %s", this.nodeId, data.data,
					srcId, protocol.statusStr()));

			return;
		}

		sentMessages++;
		this.sendMsg(srcId, new Message("DOWNLOAD_CHUNK_REQ", data.data));

		logger.info(String.format("Peer [%s] sent download request for chunk "
				+ "[%s] to [%s] %s", this.nodeId, data.data, srcId, protocol
				.statusStr()));
	}

	// ----------------------------------------------------------------------------------
	// Handshake NACK from Seeder, abort download attempt
	private void handleHandshakeNackEvent(NodeId srcId, Message data) {
		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);

		if (protocol.cancelDownload(srcId, chunkStr)) {
			logger.info(String.format("Peer [%s] NACK! Handshake [%s:%s] %s",
					this.nodeId, srcId, chunkStr, protocol.statusStr()));
		}
	}

	// ----------------------------------------------------------------------------------
	// From another Peer requesting a chunk from me
	private void handleDownloadReqEvent(NodeId srcId, String chunkStr) {
		if (protocol.hasUpload(srcId, chunkStr) == false) {
			// Can not upload Chunk to Leecher
			// Failure may have occurred
			// Do nothing, cleanup was/will be handled when in failureDetector
			// handler
			logger.info(String.format("Peer [%s] rejected download request "
					+ "from [%s] for chunk " + "[%s] %s", this.nodeId, srcId,
					chunkStr, protocol.statusStr()));

			return;
		}

		this.startSendSegment(srcId, chunkStr);

		logger.info(String.format("Peer [%s] accepted download request from "
				+ "[%s] for chunk [%s] %s", this.nodeId, srcId, chunkStr,
				protocol.statusStr()));
	}

	// ----------------------------------------------------------------------------------
	// Try to begin actual download
	// Setup a download timeout if able to begin download
	private void handleDownloadRespEvent(NodeId srcId, String chunkStr) {
		if (this.protocol.hasDownload(srcId, chunkStr) == false) {
			// Can not download Chunk from Seeder
			// Failure may have occurred
			// Do nothing, cleanup was/will be handled when in failureDetector
			// handler

			logger.info(String.format("Peer [%s] could not download chunk "
					+ "[%s] from [%s] %s", this.nodeId, chunkStr, srcId,
					protocol.statusStr()));

			return;
		}

		logger.info(String.format("Peer [%s] Downloading... chunk "
				+ "[%s] from [%s] %s", this.nodeId, chunkStr, srcId, protocol
				.statusStr()));
	}

	// ----------------------------------------------------------------------------------
	// Loopback event (from myself)
	// Triggered when I have finished Uploading Chunk to another Peer
	private void handleStopSendChunkEvent(NodeId nodeId, Message data) {
		this.stopSendSegment(data.data);

		String leecherStr = data.data.substring(0, data.data.indexOf(":"));
		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);

		logger.info(String.format("Peer [%s] finished uploading chunk "
				+ "[%s] to [%s] %s", this.nodeId, chunkStr, leecherStr,
				protocol.statusStr()));
	}

	//----------------------------------------------------------------------------------
	// From Seeder
	// Notification that I have completed a chunk download
	private void handleFinishDownloadSegmentEvent(NodeId srcId, String chunkStr) {
		if (this.protocol.successfulDownload(srcId, chunkStr) == false) {
			// Can not finish downloading Chunk from Seeder
			// Failure may have occurred
			// Do nothing, cleanup was/will be handled when in failureDetector
			// handler

			logger.info(String.format("Peer [%s] could not finish "
					+ "downloading chunk [%s] from [%s] %s", this.nodeId,
					chunkStr, srcId, protocol.statusStr()));

			return;
		}

		sentMessages++;
		this.sendMsg(TorrentConfig.TRACKER, new Message("PUT_CHUNK", chunkStr));

		logger.debug(String.format("Peer [%s] finished downloading chunk "
				+ "[%s] from [%s] Tracker notified %s", this.nodeId, chunkStr,
				srcId, protocol.statusStr()));
	}

	// ----------------------------------------------------------------------------------
	public void registerEvents() {

		this.addEventListener("DOWNLOAD_CHUNK_REQ", new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleDownloadReqEvent(srcId, data.data);
			}
		});

		this.addEventListener("DOWNLOAD_CHUNK_RESP", new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleDownloadRespEvent(srcId, data.data);
			}
		});

		this.addEventListener("DOWNLOAD_CHUNK_FINISH", new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleFinishDownloadSegmentEvent(srcId, data.data);
			}
		});

		this.addEventListener(new String("STOP_SEND_CHUNK"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleStopSendChunkEvent(srcId, data);
					}
				});

		this.addEventListener(new String("GET_CHUNK_RESP"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleGetChunkRespEvent(srcId, data);
					}
				});

		this.addEventListener(new String("HANDSHAKE_REQ"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleHandshakeReqEvent(srcId, data);
					}
				});

		this.addEventListener(new String("HANDSHAKE_ACK"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleHandshakeAckEvent(srcId, data);
					}
				});

		this.addEventListener(new String("HANDSHAKE_NACK"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleHandshakeNackEvent(srcId, data);
					}
				});
		
		this.addEventListener(new String("LEAVE"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleLeaveEvent(srcId);
					}
				});
		
		this.addEventListener(new String("START_EXPERIMENT"),
				new PeerEventListener() {
					public void receivedEvent(NodeId srcId, Message data) {
						handleStartExperimentEvent(srcId);
					}
				});
		
	}
	// ----------------------------------------------------------------------------------
	public void restore(String str) {
		// TODO Uwe/Alex put stuff here?
	}

	// ----------------------------------------------------------------------------------
	public String toString() {
		// TODO Uwe/Alex do we need this method, for:
		// --> Debugging?
		// --> Logging?
		return protocol.statusStr();
	}

	// ----------------------------------------------------------------------------------
	// After receiving request from Leecher call this method
	// "dest" = address of Downloader
	// "segmentIndex" = index of Chunk
	// "delay" = duration of uploading a Chunk
	// Uploader then sends itself loopback event to stop Uploading after "delay"
	public void startSendSegment(NodeId dest, String chunkStr) {
		int uploadBwPerNode = this.uploadBandwidth / TorrentConfig.MAX_UPLOADS;
		int delay = (TorrentConfig.CHUNK_SIZE * TorrentConfig.TIME_UNIT)
				/ uploadBwPerNode;
		sentMessages++;
		this.sendMsg(dest, new Message("DOWNLOAD_CHUNK_RESP", chunkStr));
		this.loopback(new Message("STOP_SEND_CHUNK", dest + ":" + chunkStr),
				delay);
	}

	// ----------------------------------------------------------------------------------
	// Upload has completed
	public void stopSendSegment(String data) {
		String leecherStr = data.substring(0, data.indexOf(":"));
		String chunkStr = data.substring(data.indexOf(":") + 1);

		sentMessages++;
		this.sendMsg(new NodeId(leecherStr), new Message(
				"DOWNLOAD_CHUNK_FINISH", chunkStr));

		this.protocol.successfulUpload(new NodeId(leecherStr), chunkStr);
	}

	// ----------------------------------------------------------------------------------
	public int getNumberOfCompleteSeg() {
		return TorrentConfig.CHUNK_COUNT - this.protocol.getRequiredCount();
	}
	
	public int getSentMessages() {
		return sentMessages;
	}

}
