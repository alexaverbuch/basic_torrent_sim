package ass1.common;

import org.apache.log4j.Logger;

import ass1.peers.Peer;
import sicsim.network.core.Monitor;
import sicsim.types.NodeId;

public class TorrentMonitor extends Monitor {

	static Logger logger = Logger.getLogger(TorrentMonitor.class);

	// ----------------------------------------------------------------------------------
	public void update(long currentTime) {
		String strPeers = "";

		// FIXME: Maybe sufficient to use the OverlayNetwork.toString() method?
		for (String node : this.overlay.getNodes()) {
			Peer peer = (Peer) this.network.getNode(new NodeId(node));
			strPeers += (peer.getId().id + ":" + peer.getNumberOfCompleteSeg() + " ");
		}

		logger.debug(String.format("Step: %d => [%s]", currentTime, strPeers));
	}

	// ----------------------------------------------------------------------------------
	public void verify(long currentTime) {
		// NodeId node;
		// Peer peer;
		//
		// System.out.println("-------------------------------");
		// System.out.println(this.network.size() + " nodes are in overlay!");
		//
		// Iterator<NodeId> nodeIter = this.network.getNodes().iterator();
		// while (nodeIter.hasNext()) {
		// node = new NodeId(nodeIter.next());
		// peer = (Peer)this.network.getNode(node);
		// System.out.println(peer.getId() + ", friends: " + peer.getFriends() +
		// ", failed_friends: " + peer.getFailedFriends());
		// }
	}

	// ----------------------------------------------------------------------------------
	public void snapshot(long currentTime) {
		// NodeId node;
		// Peer peer;
		// String str = new String();
		// String fileName = new String("snapshot-" + currentTime);
		//		
		// str += "time: " + currentTime + "\n\n";
		// Iterator<NodeId> nodeIter = this.network.getNodes().iterator();
		// while (nodeIter.hasNext()) {
		// node = new NodeId(nodeIter.next());
		// peer = (Peer)this.network.getNode(node);
		// str += (peer.getId() + ", friends: " + peer.getFriends() +
		// ", failed_friends: " + peer.getFailedFriends() + "\n");
		// }
		//		
		// FileIO.write(str, fileName);
	}
}
