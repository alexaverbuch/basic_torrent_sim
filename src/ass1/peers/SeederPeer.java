package ass1.peers;

import sicsim.network.core.Bandwidth;
import sicsim.network.core.FailureDetector;
import sicsim.network.core.Monitor;
import sicsim.network.core.OverlayNetwork;
import sicsim.network.links.AbstractLink;
import sicsim.types.NodeId;
import ass1.common.TorrentConfig;

public class SeederPeer extends Peer {

	// ----------------------------------------------------------------------------------
	public void init(NodeId nodeId, AbstractLink link, Bandwidth bandwidth,
			FailureDetector failureDetector, OverlayNetwork overlay,
			Monitor monitor) {
		super.init(TorrentConfig.SEEDER, link, bandwidth, failureDetector, overlay, monitor,
				TorrentConfig.MAX_UPLOAD_BW, Integer.MAX_VALUE);
	}
}
