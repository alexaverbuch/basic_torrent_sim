package ass1.peers;

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
	
	// Protocol logic & state is maintained in this badboy
	private TorrentProtocol protocol;
	
	// Used to reduce repetitive printouts so output log is shorter
	// Seeder only prints ***SEEDING*** once
	private boolean seeding = false;
	
//----------------------------------------------------------------------------------
	public void init(NodeId nodeId, AbstractLink link, Bandwidth bandwidth, FailureDetector failureDetector, OverlayNetwork overlay, Monitor monitor) {
		super.init(nodeId, link, bandwidth, failureDetector, overlay, monitor, TorrentConfig.MAX_UPLOAD_BW, Integer.MAX_VALUE);
		this.failureDetector.register(new NodeId(TorrentConfig.TRACKER) , this.nodeId);
	}

//----------------------------------------------------------------------------------
	public void create(long currentTime) {
		System.out.println("ERROR! Peer [" + this.nodeId + "] Created [" + currentTime + "]");
	}
	
//----------------------------------------------------------------------------------
	public void join(long currentTime) {
		if (this.overlay.getNodes().size() == 0) {
			System.out.println("ERROR! Peer [" + this.nodeId + "] Joined [before tracker]");
			return;
		} 
		
		if (this.overlay.getNodes().size() == 1) {
			protocol = new TorrentProtocol(nodeId,true);
			
			this.sendMsg(TorrentConfig.TRACKER, new Message("REGISTER_SEED", null));
			
			System.out.println(	"Peer (Seeder) [" + this.nodeId + "] " + 
								"Joined [" + currentTime + "]");
		} else {
			protocol = new TorrentProtocol(nodeId,false);
			
			this.sendMsg(TorrentConfig.TRACKER, new Message("REGISTER", null));
			
			System.out.println(	"Peer (Leecher) [" + this.nodeId + "] " +
								"Joined [" + currentTime + "]");
		}
		
		this.overlay.add(this.nodeId);
	}	
	
//----------------------------------------------------------------------------------
	public void leave(long currentTime) {
		// TODO Uwe/Alex will we every leave?
		// If we do, should we:
		// --> Just leave. Rely on TimeOuts which are already implemented
		// --> Notify up/downloaders. Faster than TimeOut method?
		// --> Treat it as failure (is this possible?). Rely on failureDetector
		// --> --> This seems elegant & simple to me, but I don't know if it's possible
	}

//----------------------------------------------------------------------------------
	public void failure(NodeId failedId, long currentTime) {
		System.out.println(	"Peer [" + this.nodeId + "] " +
							"Detected Failure Of [" + failedId + "]");
		
		this.protocol.cleanupFriendFailure(failedId.toString());
	}
	
//----------------------------------------------------------------------------------
	public void receive(NodeId srcId, Message data, long currentTime) {
		if (this.listeners.containsKey(data.type))
			this.listeners.get(data.type).receivedEvent(srcId, data);
		else
			System.out.println(	"ERROR! Peer [" + this.nodeId + "] " + 
								"Event [" + data.type + "] Unknown");
	}
	
//----------------------------------------------------------------------------------
	public void signal(int signal, long currentTime) {
		switch (signal) {
		case 1:
			// TODO should we have any signals to our nodes?
			// Maybe useful later during experiments 
			// Maybe print current file status for debugging purpose
			break;
		default:
			System.out.println(	"Tracker [" + this.nodeId + "] " +
								"Unknown Signal [" + signal + "]");				
		}
	}

//----------------------------------------------------------------------------------
	// This method is called every one step by the simulator
	// Request new Chunk if necessary
	// TODO Uwe/Alex find out where the frequency of this event is configured
	public void syncMethod(long currentTime) {
		
		if ( this.protocol.getRequiredCount() == 0) {
			// No need to GET more Chunks, we are seeding
			if (seeding == false) {
				seeding = true;
				System.out.println(	"Peer [" + this.nodeId + "] " +
									"**********SEEDING**********");
			}
			return;
		}
		
		if ( this.protocol.getNotYetDownloading() == 0) {
			// All required chunks are being downloaded/requested
			// Not yet finished, but last needed chunks are on their way
			System.out.println(	"Peer [" + this.nodeId + "] " +
								"Ignore SYNC [Downloading Last Chunk...]");
			return;
		}
		
		if ( this.protocol.hasAvailableDownloadSlots() == false) {
			// All download slots busy
			// Wait until next SYNC event and try again
//			System.out.println(	"Peer [" + this.nodeId + "] " +					 
//								"Ignore SYNC [Max Uploaders] " +
//								this.protocol.slotsStr() );
			return;
		}
		
		if ( this.protocol.tryMakeRequest() == false ) {
			// Maximum requests in progress already
			// Requests are assumed to ALWAYS return seeder/chunk response
			// --> Tracker is reliable, response will eventually arrive
			// --> No timeout, response time unknown
			// --> Response would be even slower all peers send many requests
			// When response is received we will attempt to download
//			System.out.println(	"Peer [" + this.nodeId + "] " +					 
//								"Ignore SYNC [Max Requests In Flight] " +
//								this.protocol.slotsStr() );
			return;
		}

		String selectNextChunkFrom = this.protocol.selectNextChunkFrom();
		
//		System.out.println(	"Peer [" + this.nodeId + "] Requesting Next Chunk From Tracker " +
////							"Select Next Chunk From [" + selectNextChunkFrom + "] "
//							this.protocol.slotsStr());
		
		// TODO this returns null sometimes, WHY?
		if (selectNextChunkFrom != null) {
			this.sendMsg( 	TorrentConfig.TRACKER, 
							new Message("GET_CHUNK_REQ", selectNextChunkFrom ) );
		} else {
			System.out.println(	"WTF");
		}
	}

//----------------------------------------------------------------------------------
	// Response from Tracker for Chunk Info
	// Now send Handshake request to Seeder
	private void handleGetChunkRespEvent(NodeId srcId, Message data) {
		String seederStr = data.data.substring(0, data.data.indexOf(":") );
		NodeId seeder = new NodeId(seederStr);
		
		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);
		Integer chunk = Integer.parseInt(chunkStr);
		
		if ( this.protocol.addHesitantDownload( seeder, chunk) == false ) {
			// Can not handshake with Seeder
			System.out.println(	"Peer [" + this.nodeId + "] Can Not Attempt Handshake " + 
								"With [" + seederStr + "] " +
								"For Chunk [" + chunkStr + "] " +
								this.protocol.slotsStr());
			
			return;
		}

		System.out.println(	"Peer [" + this.nodeId + "] Sent Handshake Req " +
							"To [" + seederStr + "] " +
							"For Chunk [" + chunkStr + "] " +
							this.protocol.slotsStr() +
							this.protocol.statusStr());
		
		if ( this.protocol.addFriend(seederStr) ) {
			this.failureDetector.register(new NodeId(seederStr), this.nodeId);
		}

		this.sendMsg(new NodeId(seederStr), new Message("HANDSHAKE_REQ", chunkStr));
		
		this.loopback(	new Message("HESITANT_DOWN_TIMEOUT", seederStr + ":" + chunkStr), 
						TorrentConfig.STANDARD_TIMEOUT);
	}
	
//----------------------------------------------------------------------------------
	// Receive Handshake request from Peer that wants to Leech a Chunk
	private void handleHandshakeReqEvent(NodeId srcId, Message data) {
		if ( protocol.addHesitantUpload(srcId, Integer.parseInt(data.data)) == false ) {
			// Can not upload Chunk to Leecher 
			System.out.println(	"Peer [" + this.nodeId + "] Rejected Handshake Request " + 
								"For Chunk [" + data.data + "] " +  
								"From [" + srcId + "]");

			this.sendMsg(srcId, new Message("HANDSHAKE_NACK", srcId + ":" + data.data));
	
			return;
		}
		
		System.out.println(	"Peer [" + this.nodeId + "] Accepted Handshake Request " + 
							"For Chunk [" + data.data + "] " +  
							"From [" + srcId + "] " + 
							"Hesitant Timeout Initialized");

		if ( this.protocol.addFriend(srcId.toString()) ) {
			this.failureDetector.register(srcId, this.nodeId);
		}

		this.loopback(	new Message("HESITANT_UP_TIMEOUT", srcId + ":" + data.data), 
						TorrentConfig.STANDARD_TIMEOUT);

		this.sendMsg(srcId, new Message("HANDSHAKE_ACK", data.data));
	}
	
//----------------------------------------------------------------------------------
	// Receive Handshake Ack from Peer that can Seed a Chunk
	private void handleHandshakeAckEvent(NodeId srcId, Message data) {
		if ( this.protocol.hasHesitantDownload(srcId, Integer.parseInt(data.data)) == false) {
			// Can not download Chunk from Seeder
			// Do nothing, no NACK, no promise to keep/break, #async
			System.out.println(	"Peer [" + this.nodeId + "] Aborted Hesitant Download " + 
								"For Chunk [" + data.data + "] " +  
								"To [" + srcId + "]");

			return;
		}
		
		System.out.println(	"Peer [" + this.nodeId + "] Sent Download Request " + 
							"For Chunk [" + data.data + "] " +  
							"To [" + srcId + "] " + 
					"Hesitant Timeout Initialized");

		this.sendMsg(srcId, new Message("DOWNLOAD_CHUNK_REQ", data.data));
	}
	
//----------------------------------------------------------------------------------
	// Hesitant Download timeout, abort if no progress has been made
	private void handleHandshakeNackEvent(NodeId srcId, Message data) {
		String seederStr = data.data.substring(0, data.data.indexOf(":") );
		NodeId seeder = new NodeId(seederStr);
		
		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);
		Integer chunk = Integer.parseInt(chunkStr);
		
		if ( protocol.cancelHesitantDownload(seeder, chunk) ) {
			System.out.println(	"NACK! Peer [" + this.nodeId + "] " +
								"Hesitant Download [" + seederStr + ":" + chunkStr + "]" );
		}
	}	
	
//----------------------------------------------------------------------------------
	// From another Peer requesting a chunk from me
	private void handleDownloadReqEvent(NodeId srcId, int chunkIndex) {
		if ( protocol.commitToUpload(srcId,chunkIndex) == false ) {
			// Can not upload Chunk to Leecher
			// Timeout occurred earlier during Handshake phase
			// Do nothing, no NACK, no promise to keep/break, #async
			System.out.println(	"Peer [" + this.nodeId + "] Rejected Download Req " + 
								"From [" + srcId + "] " +
								"For Chunk [" + chunkIndex + "]" );
			return;
		}

		System.out.println(	"Peer [" + this.nodeId + "] Accepted Download Req " + 
							"From [" + srcId + "] " +
							"For Chunk [" + chunkIndex + "]" );
				
		this.startSendSegment(srcId,chunkIndex);
	}

//----------------------------------------------------------------------------------
	// Try to commit to download 
	// Setup a download timeout if able to commit to download  
	private void handleDownloadRespEvent(NodeId srcId, int chunkIndex) {
		if ( this.protocol.commitToDownload(srcId, chunkIndex) == false ) {
//			System.out.println(	"Peer [" + this.nodeId + "] Could Not Download " + 
//								"Chunk [" + chunkIndex + "] " +  
//								"From [" + srcId + "]" );

			return;
		}
		
		System.out.println(	"Peer [" + this.nodeId + "] Downloading... " + 
							"Chunk [" + chunkIndex + "] " +  
							"From [" + srcId + "] " +
							this.protocol.slotsStr() + 
							this.protocol.statusStr());
	
		this.setupDownloadTimeout(srcId,chunkIndex);
	}
	
//----------------------------------------------------------------------------------
	// Loopback from myself. Triggered when I have finished Uploading Chunk to another Peer
	private void handleStopSendChunkEvent(NodeId nodeId, Message data) {
		this.stopSendSegment(data.data);
		
		String leecherStr = data.data.substring(0, data.data.indexOf(":"));
		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);
		
		System.out.println(	"Peer [" + this.nodeId + "] Finished Uploading " + 
							"Chunk [" + chunkStr + "] " +
							"To [" + leecherStr + "] " + 
							this.protocol.slotsStr() );
	}
	
//----------------------------------------------------------------------------------
	// From Seeder
	// Notification that I have completed a chunk download
	private void handleFinishDownloadSegmentEvent(NodeId srcId, Integer chunkIndex) {
		if ( this.protocol.successfulDownload(srcId, chunkIndex) == false ) {
//			System.out.println(	"Peer [" + this.nodeId + "] Could Not Finish Downloading " + 
//								"Chunk [" + chunkIndex + "] " +  
//								"From [" + srcId + "]");

			return;
		} 
		
		System.out.println(	"Peer [" + this.nodeId + "] Finished Downloading " + 
							"Chunk [" + chunkIndex + "] " +  
							"From [" + srcId + "] " +
							"Tracker Notified " +
							this.protocol.slotsStr() + 
							this.protocol.statusStr());

		this.sendMsg(TorrentConfig.TRACKER, new Message("PUT_CHUNK",Integer.toString(chunkIndex)) );
	}
	
//----------------------------------------------------------------------------------
	// Hesitant Upload timeout, abort if no progress has been made
	private void handleHesistantUpTimeoutEvent(NodeId srcId, Message data) {
		String leecherStr = data.data.substring(0, data.data.indexOf(":") );
		NodeId leecher = new NodeId(leecherStr); 
		
		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);
		Integer chunk = Integer.parseInt(chunkStr);
		
		if ( protocol.cancelHesitantUpload(leecher, chunk) ) {
			System.out.println(	"TIMEOUT! Peer [" + this.nodeId + "] " +
								"Hesitant Upload [" + leecherStr + ":" + chunkStr + "]" );
		}
	}
	
//----------------------------------------------------------------------------------
	// Hesitant Download timeout, abort if no progress has been made
	private void handleHesistantDownTimeoutEvent(NodeId srcId, Message data) {
		String seederStr = data.data.substring(0, data.data.indexOf(":") );
		NodeId seeder = new NodeId(seederStr); 
		
		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);
		Integer chunk = Integer.parseInt(chunkStr);
		
		if ( protocol.cancelHesitantDownload(seeder, chunk) ) {
			System.out.println(	"TIMEOUT! Peer [" + this.nodeId + "] " +
								"Hesitant Download [" + seederStr + ":" + chunkStr + "]" );
		}
	}	
	
//----------------------------------------------------------------------------------
	// Hesitant Download timeout, abort if no progress has been made
	private void handleDownTimeoutEvent(NodeId srcId, Message data) {
		String seederStr = data.data.substring(0, data.data.indexOf(":"));
		NodeId seeder = new NodeId(seederStr);
		
		String chunkStr = data.data.substring(data.data.indexOf(":") + 1);
		Integer chunk = Integer.parseInt(chunkStr);
		
		// TODO recent change 12:37 1/11/09, may be wrong
//		if ( this.protocol.cancelDownload(srcId, Integer.parseInt(chunkStr)) ) {
		if ( this.protocol.cancelDownload(seeder, chunk) ) {
			System.out.println(	"TIMEOUT! Peer [" + this.nodeId + "] " +
								"Download [" + seederStr + ":" + chunkStr + "]" );
		}
	}	
	
//----------------------------------------------------------------------------------
	public void registerEvents() {
		
	    this.addEventListener("DOWNLOAD_CHUNK_REQ", new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleDownloadReqEvent(srcId, Integer.parseInt(data.data));
			}
		});

	    this.addEventListener("DOWNLOAD_CHUNK_RESP", new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleDownloadRespEvent(srcId, Integer.parseInt(data.data));
			}
		});

	    this.addEventListener("DOWNLOAD_CHUNK_FINISH", new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleFinishDownloadSegmentEvent(srcId, Integer.parseInt(data.data));
			}
		});

		this.addEventListener(new String("STOP_SEND_CHUNK"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleStopSendChunkEvent(srcId, data);
			}
		});
		
		this.addEventListener(new String("GET_CHUNK_RESP"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleGetChunkRespEvent(srcId, data);
			}
		});

		this.addEventListener(new String("HANDSHAKE_REQ"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleHandshakeReqEvent(srcId, data);
			}
		});

		this.addEventListener(new String("HANDSHAKE_ACK"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleHandshakeAckEvent(srcId, data);
			}
		});

		this.addEventListener(new String("HANDSHAKE_NACK"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleHandshakeNackEvent(srcId, data);
			}
		});

		this.addEventListener(new String("HESITANT_UP_TIMEOUT"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleHesistantUpTimeoutEvent(srcId, data);
			}
		});
		
		this.addEventListener(new String("HESITANT_DOWN_TIMEOUT"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleHesistantDownTimeoutEvent(srcId, data);
			}
		});
		
		this.addEventListener(new String("DOWN_TIMEOUT"), new PeerEventListener() {
			public void receivedEvent(NodeId srcId, Message data) {
				handleDownTimeoutEvent(srcId, data);
			}
		});
		
	}

//----------------------------------------------------------------------------------
	public void restore(String str) {
		// TODO Uwe/Alex put stuff here?
	}

//----------------------------------------------------------------------------------
	public String toString() {
		// TODO Uwe/Alex do we need this method, for:
		// --> Debugging?
		// --> Logging?
    	return null;
    }

//----------------------------------------------------------------------------------
	// After receiving request from Leecher call this method
	// "dest" = address of Downloader
	// "segmentIndex" = index of Chunk
	// "delay" = duration of uploading a Chunk
	// Uploader then sends itself loopback event to stop Uploading after "delay"
	public void startSendSegment(NodeId dest, int segmentIndex) {
		int uploadBwPerNode = this.uploadBandwidth / TorrentConfig.MAX_NUM_DOWNLOADERS;
		int delay = (TorrentConfig.CHUNK_SIZE * TorrentConfig.TIME_UNIT) / uploadBwPerNode;
		this.sendMsg(dest, new Message("DOWNLOAD_CHUNK_RESP", Integer.toString(segmentIndex)));
		this.loopback(new Message("STOP_SEND_CHUNK", dest + ":" + Integer.toString(segmentIndex)), delay);
    }

//----------------------------------------------------------------------------------
	// Upload has completed
	public void stopSendSegment(String data) {				
		String dest = data.substring(0, data.indexOf(":"));
		String segment = data.substring(data.indexOf(":") + 1);
		
		this.sendMsg(new NodeId(dest), new Message("DOWNLOAD_CHUNK_FINISH", segment));
		this.protocol.decUploads();
    }

//----------------------------------------------------------------------------------
	// Timeout TWICE as long as expected download time
	private void setupDownloadTimeout(NodeId srcId, int chunkIndex) {
		int uploadBwPerNode = this.uploadBandwidth / TorrentConfig.MAX_NUM_DOWNLOADERS;
		int delay = (TorrentConfig.CHUNK_SIZE * TorrentConfig.TIME_UNIT) / uploadBwPerNode;
		this.loopback(new Message("DOWN_TIMEOUT", srcId + ":" + Integer.toString(chunkIndex)), delay*2);
	}

	//----------------------------------------------------------------------------------
	public int getNumberOfCompleteSeg() {
		return TorrentConfig.CHUNK_COUNT - this.protocol.getRequiredCount();
	}
}
