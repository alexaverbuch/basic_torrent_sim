package ass1.file;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import ass1.common.TorrentConfig;

import sicsim.types.NodeId;

public class GlobalFileStatus {
	private ArrayList<GlobalChunkStatus> chunks = new ArrayList<GlobalChunkStatus>();
	private Random RNG;
	
	public GlobalFileStatus(int chunkCount, int seed) {
		super();
		for (int i = 0; i < chunkCount; i++) {
			chunks.add(new GlobalChunkStatus(i, seed));
		}
		
		RNG = new Random(seed);
//		RNG = new Random();		
	}
	
	public String getRandomFrom(ArrayList<Integer> validChunks) {
		Integer chunk = null;
		NodeId seeder = null;
		
		do {
			int validChunksIndex = RNG.nextInt(validChunks.size());
			chunk = validChunks.remove(validChunksIndex);
			GlobalChunkStatus chunkStatus = chunks.get(chunk);
			seeder = chunkStatus.getRandomSeeder();
		} while (seeder == null);
		
		return seeder.toString() + ":" + chunk.toString();
	}
	
	public boolean addSeeder(Integer index, NodeId seeder) {
		GlobalChunkStatus chunk = chunks.get(index);
		return chunk.addSeeder(seeder);
	}
	
	public void removeSeeder(NodeId seeder) {
		for (int i = 0; i < TorrentConfig.CHUNK_COUNT; i++) {
			GlobalChunkStatus tempChunkStatus = chunks.remove(i);
			tempChunkStatus.removeSeeder(seeder);
			chunks.add(tempChunkStatus);
		}		
	}
	
	public String chunksToString() {
		return null;
	}
	
	public String peersToString() {
		return null;
	}
	
	public String toString() {
		String str = "---FileStatus---\n";
		Iterator<GlobalChunkStatus> chunksIter = this.chunks.iterator();
		
		while (chunksIter.hasNext())
			str += chunksIter.next() + "\n";
		
    	return str;
	}
	
}
