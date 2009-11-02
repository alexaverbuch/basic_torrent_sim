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
	private TorrentProtocol protocol;

	// Used to reduce repetitive printouts so output log is shorter
	// Seeder only prints ***SEEDING*** once
	private boolean seeding = false;

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
		logger.error(String.format("Peer [%s] created [%d]", this.nodeId,
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

			this.sendMsg(TorrentConfig.TRACKER, new Message("REGISTER_SEED",
					null));

			logger.error(String.format("Peer [%s] (Seeder) joined [%d] [%s]",
					this.nodeId, currentTime, protocol.statusStr()));
		} else {
			protocol = new TorrentProtocol(nodeId, false);

			this.sendMsg(TorrentConfig.TRACKER, new Message("REGISTER", null));

			logger.error(String.format("Peer [%s] (Leecher) joined [%d] %s",
					this.nodeId, currentTime, protocol.statusStr()));
		}

		this.overlay.add(this.nodeId);
	}

	// ----------------------------------------------------------------------------------
	public void leave(long currentTime) {
		// FIXME Uwe/Alex will we every leave?
		// If we do, should we:
		// --> Notify up/downloaders
		// --> Treat it as failure (is this possible?). Rely on failureDetector
		// --> --> This seems elegant & simple to me, but I don't know if it's
		// possible
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
		logger.error(	String.format("Peer [%s] SIGNAL! %s" ,
				this.nodeId, protocol.statusStr()));
		switch (signal) {
		case 1:
			// TODO Uwe/Alex should we have any signals to our nodes?
			// Maybe useful later during experiments
			// Maybe print current file status for debugging purpose
			logger.error(	String.format("Peer [%s] %s" ,
							this.nodeId, protocol.statusStr()));
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

		if (this.protocol.getRequiredCount() == 0) {
			// No need to GET more Chunks, we are seeding
			if (seeding == false) {
				seeding = true;
				logger.info(String.format(
						"Peer [%s] **********SEEDING********** %s",
						this.nodeId, protocol.statusStr()));
			}
			return;
		}

		if (this.protocol.tryTakeDownloadSlot() == false) {
			// All download slots busy
			// Wait until next SYNC event and try again
			// System.out.println( "Peer [" + this.nodeId + "] " +
			// "Ignore SYNC [Max Uploaders] " +
			// protocol.statusStr() );
			return;
		}

		if (this.protocol.downloadingLastChunks()) {
			// All required chunks are already downloading
			// System.out.println( "Peer [" + this.nodeId + "] " +
			// "Downloading Final Chunks... " +
			// protocol.statusStr() );
			return;
		}

		String selectNextChunkFrom = this.protocol.selectNextChunkFrom();

		logger.info(String.format("Peer [%s] RequestNext NextFrom [%s] %s",
				this.nodeId, selectNextChunkFrom, protocol.statusStr()));

		// TODO this returns null sometimes, WHY?
		if (selectNextChunkFrom != null) {
			this.sendMsg(TorrentConfig.TRACKER, new Message("GET_CHUNK_REQ",
					selectNextChunkFrom));
		} else {
			logger.error(String.format("Peer [%s] NextFrom [%s] %s",
					this.nodeId, selectNextChunkFrom, protocol.statusStr()));
		}
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
			this.sendMsg(srcId, new Message("HANDSHAKE_NACK", data.data));

			logger.info(String.format("Peer [%s] rejected handshake request "
					+ "for chunk [%s] from [%s] %s", this.nodeId, data.data,
					srcId, protocol.statusStr()));

			return;
		}

		if (this.protocol.addFriend(srcId.toString())) {
			this.failureDetector.register(srcId, this.nodeId);
		}

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

	// ----------------------------------------------------------------------------------
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

		this.sendMsg(TorrentConfig.TRACKER, new Message("PUT_CHUNK", chunkStr));

//		logger.info(String.format("Peer [%s] finished downloading chunk "
//				+ "[%s] from [%s] Tracker notified %s", this.nodeId, chunkStr,
//				srcId, protocol.statusStr()));
		logger.error(	String.format("Peer [%s] Received [%s] %s", 
						this.nodeId, chunkStr, protocol.chunksStr()));
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
		return null;
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
		this.sendMsg(dest, new Message("DOWNLOAD_CHUNK_RESP", chunkStr));
		this.loopback(new Message("STOP_SEND_CHUNK", dest + ":" + chunkStr),
				delay);
	}

	// ----------------------------------------------------------------------------------
	// Upload has completed
	public void stopSendSegment(String data) {
		String leecherStr = data.substring(0, data.indexOf(":"));
		String chunkStr = data.substring(data.indexOf(":") + 1);

		this.sendMsg(new NodeId(leecherStr), new Message(
				"DOWNLOAD_CHUNK_FINISH", chunkStr));

		this.protocol.successfulUpload(new NodeId(leecherStr), chunkStr);
	}

	// ----------------------------------------------------------------------------------
	public int getNumberOfCompleteSeg() {
		return TorrentConfig.CHUNK_COUNT - this.protocol.getRequiredCount();
	}
}
