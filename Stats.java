
public class Stats {

	private int totalDataBlocks = 0;
	private int totalAcks = 0;
	private int totalBytes = 0;
	private long startTime = 0L;
	
	//RTT
	private int rtt;
	private static int sumOfRtt;
	private static int rttSumLenght;
	
	Stats() {
		startTime = System.currentTimeMillis();
		rtt = 20;
		sumOfRtt = rtt;
		rttSumLenght = 1;
	}

	public void updateBytesRead(int bytesRead) {
		totalDataBlocks++;
		totalBytes += bytesRead;
	}
	
	public void updateAck() {
		totalAcks++;
	}
	
	public int getRTT() {
		return rtt;
	}
	
	public void updateRTT(long currentRTT) {
		sumOfRtt += Math.round(currentRTT);
		rttSumLenght++;
		rtt = rtt > 1 ? Math.round(sumOfRtt / rttSumLenght) : 1;
	}
	
// any other methods

	void printReport() {
		// compute time spent sending data blocks
		int milliSeconds = (int) (System.currentTimeMillis() - startTime);
		float speed = (float) (totalBytes * 8.0 / milliSeconds / 1000); // M bps
		System.out.println("\nTransfer stats:");
		System.out.println("\nFile size:\t\t\t" + totalBytes);
		System.out.printf("End-to-end transfer time:\t%.3f s\n", (float) milliSeconds / 1000);
		System.out.printf("End-to-end transfer rate:\t%.3f M bps\n", speed);
		System.out.println("Number of data messages sent:\t\t\t" + totalDataBlocks);
		System.out.println("Number of Acks received:\t\t\t" + totalAcks);
		System.out.println("Last RTT Used:\t\t\t" + rtt);
		

	}
}
