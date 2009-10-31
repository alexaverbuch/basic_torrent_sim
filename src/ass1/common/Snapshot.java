package ass1.common;

public class Snapshot {
	private static boolean enable = false;
	private static int totalLookups = 0;
	private static int correctLookups = 0;
	private static int successfulLookups = 0;
	private static int failedLookups = 0;
	private static int hopCount = 0;
	private static int bwConsumption = 0;

//----------------------------------------------------------------------------------
	public static void enable() {
		Snapshot.enable = true;
	}

//----------------------------------------------------------------------------------
	public static void disable() {
		Snapshot.enable = false;
	}

//----------------------------------------------------------------------------------
	public static void incSuccessfulLookups() {
		if (Snapshot.enable)
			Snapshot.successfulLookups++;
	}

//----------------------------------------------------------------------------------
	public static void incTotalLookups() {
		if (Snapshot.enable)
			Snapshot.totalLookups++;
	}

//----------------------------------------------------------------------------------
	public static void incFailedLookups() {
		if (Snapshot.enable)
			Snapshot.failedLookups++;
	}

//----------------------------------------------------------------------------------
	public static void incCorrectLookups() {
		if (Snapshot.enable)
			Snapshot.correctLookups++;
	}

//----------------------------------------------------------------------------------
	public static void incHopCount(int hopCount) {
		if (Snapshot.enable)
			Snapshot.hopCount += hopCount;
	}

//----------------------------------------------------------------------------------
	public static void incBwConsumption() {
		if (Snapshot.enable)
			Snapshot.bwConsumption++;
	}

//----------------------------------------------------------------------------------
	public static boolean isEnable() {
		return Snapshot.enable;
	}

//----------------------------------------------------------------------------------
	public static int getSuccessfulLookups() {
		return Snapshot.successfulLookups;
	}

//----------------------------------------------------------------------------------
	public static int getTotalLookups() {
		return Snapshot.totalLookups;
	}

//----------------------------------------------------------------------------------
	public static int getFailedLookups() {
		return Snapshot.failedLookups;
	}

//----------------------------------------------------------------------------------
	public static int getCorrectLookups() {
		return Snapshot.correctLookups;
	}

//----------------------------------------------------------------------------------
	public static int getTotalHopCount() {
		return Snapshot.hopCount;
	}

//----------------------------------------------------------------------------------
	public static int getBwConsumption() {
		return Snapshot.bwConsumption;
	}

}
