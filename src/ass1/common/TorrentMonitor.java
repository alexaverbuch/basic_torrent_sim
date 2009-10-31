package ass1.common;

import java.util.Iterator;

import ass1.peers.Peer;

import sicsim.network.core.Monitor;
import sicsim.types.NodeId;
import sicsim.utils.FileIO;

public class TorrentMonitor extends Monitor {
	
//----------------------------------------------------------------------------------
	public void update(long currentTime) {
		String str = new String("step: " + currentTime + " ==> [ ");
		
		for (String node: this.overlay.getNodes()) {
			Peer peer = (Peer)this.network.getNode(new NodeId(node));
			str += (peer.getId().id + ":" + peer.getNumberOfCompleteSeg() + " ");
		}
		
		str += "]";
		
		System.out.println(str);
	}

	//----------------------------------------------------------------------------------
	public void verify(long currentTime) {
//		NodeId node;
//		Peer peer;
//
//		System.out.println("-------------------------------");
//		System.out.println(this.network.size() + " nodes are in overlay!");
//
//		Iterator<NodeId> nodeIter = this.network.getNodes().iterator();
//		while (nodeIter.hasNext()) {
//			node = new NodeId(nodeIter.next());
//			peer = (Peer)this.network.getNode(node);
//			System.out.println(peer.getId() + ", friends: " + peer.getFriends() + ", failed_friends: " + peer.getFailedFriends());
//		}
	}

//----------------------------------------------------------------------------------
	public void snapshot(long currentTime) {
//		NodeId node;
//		Peer peer;
//		String str = new String();
//		String fileName = new String("snapshot-" + currentTime);
//		
//		str += "time: " + currentTime + "\n\n";
//		Iterator<NodeId> nodeIter = this.network.getNodes().iterator();
//		while (nodeIter.hasNext()) {
//			node = new NodeId(nodeIter.next());
//			peer = (Peer)this.network.getNode(node);
//			str += (peer.getId() + ", friends: " + peer.getFriends() + ", failed_friends: " + peer.getFailedFriends() + "\n");
//		}
//		
//		FileIO.write(str, fileName);
	}
}
