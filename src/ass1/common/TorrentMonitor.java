package ass1.common;

import java.util.Iterator;

import org.apache.log4j.Logger;

import ass1.peers.Peer;
import ass1.peers.Tracker;
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
		
		int failures = 0;
		int leaves = 0;
		int joins = 0;
		int messages = 0;
		long startTime = 0;
		
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
				 messages += peer.getSentMessages();
				 startTime += peer.getStartTime();

				 System.out.println(String.format("Peer[%9s] %s", peer.getId(), peer.protocol.chunksStr()));
			 } else {
				 trackers++;
				 Tracker tracker = (Tracker)this.network.getNode(nodeId);
				 joins = tracker.getJoins();
				 failures = tracker.getFailures();
				 leaves = tracker.getLeaves();
				 messages += tracker.getSentMessages();
				 startTime += tracker.getStartTime();
			 }
		 }
		 
		 System.out.println(String.format("-------------------------------------------------------------"));
		 System.out.println(String.format("Nodes in overlay:\t%d",this.network.size()));
		 System.out.println();
		 System.out.println(String.format("Trackers:\t\t%d",trackers));
		 System.out.println(String.format("Seeders:\t\t%d",seeders));
		 System.out.println(String.format("Leechers:\t\t%d",leechers));
		 System.out.println();
		 System.out.println(String.format("Joins:\t\t\t%d",joins));
		 System.out.println(String.format("Failures:\t\t%d",failures));
		 System.out.println(String.format("Leaves:\t\t\t%d",leaves));
		 System.out.println();
		 System.out.println(String.format("Messages sent:\t\t%d",messages));
		 System.out.println(String.format("Protocol Time:\t\t%d",currentTime-startTime));		 
		 System.out.println(String.format("Simulation Time:\t%d",currentTime));		 
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
//		System.err.println("lkj;lkjl;kjlk;jl;k");
	}
}
