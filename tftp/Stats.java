package tftp;

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
	
	private int maxRtt;
	private int minRtt;
	private int minTimeout;
	private int maxTimeout;
	
	private int windowSize;
	private int blockSize;
	
	Stats() {
		startTime = System.currentTimeMillis();
		
		estimatedRTT = 50;
		devRTT = 10;
		timeoutValue = 50;
		
		maxRtt = Integer.MIN_VALUE;
		minRtt = Integer.MAX_VALUE;
		
		maxTimeout = Integer.MIN_VALUE;
		minTimeout = Integer.MAX_VALUE;
		
		windowSize = 0;
		blockSize = 0;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}
	
	public void setBlockSize(int blockSize) {
		this.blockSize = blockSize;
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
		maxRtt = (int)(sampleRTT > maxRtt ? sampleRTT : maxRtt); 
		minRtt = (int)(sampleRTT < minRtt ? sampleRTT : minRtt); 
		
		estimatedRTT = Math.round(estimatedRTT * (1 - ALPHA) + sampleRTT*ALPHA);
		devRTT = Math.round((1 - BETA)*devRTT + BETA * Math.abs(sampleRTT - estimatedRTT));
		timeoutValue = estimatedRTT + 4*devRTT;
		
		maxTimeout = (int)(timeoutValue > maxTimeout ? timeoutValue : maxTimeout); 
		minTimeout = (int)(timeoutValue < minTimeout ? timeoutValue : minTimeout);
	}
	
// any other methods

	void printReport() {
		// compute time spent sending data blocks
		int milliSeconds = (int) (System.currentTimeMillis() - startTime);
		float speed = (float) (totalBytes * 8.0 / milliSeconds / 1000); // M bps
		System.out.println("\nTransfer stats:");
		System.out.println("\nFile size:\t\t\t " + totalBytes);
		System.out.printf("End-to-end transfer time:\t %.3f s\n", (float) milliSeconds / 1000);
		System.out.printf("End-to-end transfer rate:\t %.3f M bps\n", speed);
		System.out.println("Number of data messages sent:\t " + totalDataBlocks);
		System.out.println("Number of Acks received:\t " + totalAcks);
		
		System.out.printf("rtt - min, average, max: \t %d  %d  %d ms \n", minRtt, estimatedRTT, maxRtt);
		System.out.printf("timeOut - min, last, max: \t %d  %d  %d ms \n\n", minTimeout, timeoutValue, maxTimeout);
		
		System.out.println("window size: "+windowSize+" block size: "+ blockSize);
	}
}
