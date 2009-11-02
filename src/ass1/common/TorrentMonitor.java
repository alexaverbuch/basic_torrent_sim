package ass1.common;

import java.util.Iterator;

import org.apache.log4j.Logger;

import ass1.peers.Peer;
import sicsim.network.core.Monitor;
import sicsim.types.NodeId;

public class TorrentMonitor extends Monitor {

	static Logger logger = Logger.getLogger(TorrentMonitor.class);

	// ----------------------------------------------------------------------------------
	public void update(long currentTime) {
//		String strPeers = "";
//
//		// FIXME: Maybe sufficient to use the OverlayNetwork.toString() method?
//		for (String node : this.overlay.getNodes()) {
//			Peer peer = (Peer) this.network.getNode(new NodeId(node));
//			strPeers += (peer.getId().id + ":" + peer.getNumberOfCompleteSeg() + " ");
//		}
//
////		logger.debug(String.format("Step: %d => [%s]", currentTime, strPeers));
//		System.out.println("\nUPDATE\n");
	}

	// ----------------------------------------------------------------------------------
	public void verify(long currentTime) {
		NodeId nodeId;
		Peer peer;
		
		int trackers = 0;
		int seeders = 0;
		int leechers = 0;
		
		 Iterator<NodeId> nodeIter = this.network.getNodes().iterator();
		 while (nodeIter.hasNext()) {
			 nodeId = new NodeId(nodeIter.next());
			 
			 if (nodeId.equals(TorrentConfig.TRACKER) == false) {
				 peer = (Peer)this.network.getNode(nodeId);
				 
				 if (peer.protocol.getRequiredCount() > 0) {
					 leechers++;
				 } else {
					seeders++; 
				 }					 

				 System.out.println(String.format("Peer[%s] %s", peer.getId(), peer.protocol.chunksStr()));
			 } else {
				 trackers++;
			 }
		 }
		 
		 System.out.println(String.format("-------------------------------------------------------------"));
		 System.out.println(String.format("Nodes in overlay:\t%d",this.network.size()));
		 System.out.println(String.format("Trackers:\t\t%d",trackers));
		 System.out.println(String.format("Seeders:\t\t%d",seeders));
		 System.out.println(String.format("Leechers:\t\t%d",leechers));
		 System.out.println(String.format("Time (mS):\t\t%d",currentTime-10000));
		 System.out.println(String.format("-------------------------------------------------------------"));
			
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
