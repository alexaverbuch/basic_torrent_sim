package ass1.file;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import sicsim.types.NodeId;

public class GlobalChunkStatus {
	private Random RNG;
	private int ID;
	private ArrayList<NodeId> seeders = new ArrayList<NodeId>();
	
	public GlobalChunkStatus(int iD, long seed) {
		super();
		ID = iD;
//		RNG = new Random(seed);
		RNG = new Random();
	}
	
	public int getID() {
		return ID;
	}
	
	public boolean addSeeder(NodeId seeder) {
		return seeders.add(seeder);
	}
	
	//TODO removeSeeder()? 
	
	public NodeId getRandomSeeder() {
		if (seeders.size() == 0)
			return null;
		
		int index = RNG.nextInt( seeders.size() );
		NodeId seeder = seeders.get(index);
		return seeder;
	}

	public String toString() {
		String str = "Chunk[" + ID + "] - Seeders";
		Iterator<NodeId> seedersIter = this.seeders.iterator();
		
		while (seedersIter.hasNext())
			str += "[" + seedersIter.next() + "]";
		
    	return str;
	}
	
}
