
public class Stats {

	//Constants
	public static final float ALPHA = 0.250f;
	public static final float BETA = 0.250f;
	
	
	//Vars
	private int totalDataBlocks = 0;
	private int totalAcks = 0;
	private int totalBytes = 0;
	private long startTime = 0L;
	
	//RTT
	
	private int devRTT;
	private int estimatedRTT;
	private int timeoutValue;
	
	private int maxRTT;
	private int minRTT;
	
	Stats() {
		startTime = System.currentTimeMillis();
		
		estimatedRTT = 50;
		devRTT = 5;
		timeoutValue = estimatedRTT;
		
		maxRTT = Integer.MIN_VALUE;
		minRTT = Integer.MAX_VALUE;
	}

	public void updateBytesRead(int bytesRead) {
		totalDataBlocks++;
		totalBytes += bytesRead;
	}
	
	public void updateAck() {
		totalAcks++;
	}
	
	public int getTimeOut() {
		return timeoutValue;
	}
	
	public void updateRTT(long sampleRTT) {
		//UPDATE max and min rtt during file transfer
		maxRTT = (int)(sampleRTT > maxRTT ? sampleRTT : maxRTT); 
		minRTT = (int)(sampleRTT < minRTT ? sampleRTT : minRTT); 
		
		estimatedRTT = Math.round(estimatedRTT * (1 - ALPHA) + sampleRTT*ALPHA);
		devRTT = Math.round((1 - BETA)*devRTT + BETA * Math.abs(sampleRTT - estimatedRTT));
		timeoutValue = estimatedRTT + 4*devRTT;
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
		System.out.println("Last Timeout Used:\t\t\t" + timeoutValue);
		System.out.println("Max RTT:\t\t\t" + maxRTT);
		System.out.println("Min RTT:\t\t\t" + minRTT);
		
	}
}
