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
		RNG = new Random(seed);
		// RNG = new Random();
	}

	public int getID() {
		return ID;
	}

	public boolean addSeeder(NodeId seeder) {
		if (seeders.contains(seeder) == true) {
			return false;
		}
		return seeders.add(seeder);
	}

	public boolean removeSeeder(NodeId seeder) {
		return seeders.remove(seeder);
	}

	public NodeId getRandomSeeder() {
		if (seeders.size() == 0)
			return null;

		int index = RNG.nextInt(seeders.size());
		NodeId seeder = seeders.get(index);
		return seeder;
	}

	public String toString() {
		StringBuffer str = new StringBuffer("Chunk[" + ID + "][" + seeders.size() + "] - Seeders");
		Iterator<NodeId> seedersIter = this.seeders.iterator();
		  
		while (seedersIter.hasNext())
			str.append("[" + seedersIter.next() + "]");

		return str.toString();
	}

}
