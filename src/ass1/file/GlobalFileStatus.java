package ass1.file;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import ass1.common.TorrentConfig;

import sicsim.types.NodeId;

public class GlobalFileStatus {
	private ArrayList<GlobalChunkStatus> chunks = new ArrayList<GlobalChunkStatus>();
	ArrayList<String> exNodes = new ArrayList<String>(); // Nodes that left/failed

	private Random RNG;

	public GlobalFileStatus(int chunkCount, int seed) {
		super();
		for (int i = 0; i < chunkCount; i++) {
			chunks.add(new GlobalChunkStatus(i, seed));
		}

		RNG = new Random(seed);
		// RNG = new Random();
	}

	public ArrayList<String> getRandomFrom(ArrayList<Integer> validChunks, int requests) {
		ArrayList<String> responses = new ArrayList<String>();
		
		Integer chunk = null;
		NodeId seeder = null;		

		do {
			int validChunksIndex = RNG.nextInt(validChunks.size());
			chunk = validChunks.remove(validChunksIndex);
			GlobalChunkStatus chunkStatus = chunks.get(chunk);
			seeder = chunkStatus.getRandomSeeder();
			
			if (seeder != null) {
				requests--;
				responses.add(seeder.toString() + ":" + chunk.toString());
			}
		} while ( requests > 0 && validChunks.size() > 0 );

		if (responses.size() == 0) {
			return null;
		}
		
		return responses;
	}

	public boolean addSeeder(Integer index, NodeId seeder) {
		// Check if Node has left/failed previously
		// --> Current assumption is that once Nodes leave, they never return
		// --> If Node has left/failed, this is a late message
		if (exNodes.contains(seeder.toString()) == true) {
			return false;
		}
		
		GlobalChunkStatus chunk = chunks.get(index);
		return chunk.addSeeder(seeder);
	}

	// Handle failure/leaving of Nodes
	public String removeSeeder(NodeId seeder) {
		// Current assumption is that once Nodes leave, they never return
		if (exNodes.contains(seeder.toString()) == false) {
			exNodes.add(seeder.toString());
		}
		
		StringBuffer result = new StringBuffer("\n");
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			GlobalChunkStatus tempChunkStatusBefore = chunks.get(i);
			result.append("BEFORE[" + seeder + "]"
					+ tempChunkStatusBefore.toString() + "\n");
			tempChunkStatusBefore.removeSeeder(seeder);
			GlobalChunkStatus tempChunkStatusAfter = chunks.get(i);
			result.append("AFTER [" + seeder + "]"
					+ tempChunkStatusAfter.toString() + "\n");
		}
		return result.toString();
	}

	public String toString() {
		StringBuffer str = new StringBuffer("---FileStatus---\n");
		Iterator<GlobalChunkStatus> chunksIter = this.chunks.iterator();

		while (chunksIter.hasNext())
			str.append(chunksIter.next() + "\n");

		return str.toString();
	}

}
