package ass1.common;

public class Snapshot {
	private static boolean enable = false;
	private static int bwConsumption = 0;

	// ----------------------------------------------------------------------------------
	public static void enable() {
		Snapshot.enable = true;
	}

	// ----------------------------------------------------------------------------------
	public static void disable() {
		Snapshot.enable = false;
	}

	// ----------------------------------------------------------------------------------
	public static void incBwConsumption() {
		if (Snapshot.enable)
			Snapshot.bwConsumption++;
	}

	// ----------------------------------------------------------------------------------
	public static boolean isEnable() {
		return Snapshot.enable;
	}

	// ----------------------------------------------------------------------------------
	public static int getBwConsumption() {
		return Snapshot.bwConsumption;
	}

}
