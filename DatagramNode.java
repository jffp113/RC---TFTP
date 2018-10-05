import java.net.DatagramPacket;

public class DatagramNode {
	//Constants
	
	
	//Variables
	private long timeOutTime;
	private DatagramPacket packet;
	private long timeSent;
	private TFtpPacketV18 converter;
	
	//Constructor
	public DatagramNode(DatagramPacket packet, int rtt) {
		updateTimeOut(rtt);
		this.packet = packet; 
		converter = new TFtpPacketV18(packet.getData(), packet.getLength());
	}
	
	public int getTimeOut() {
		return Math.round(timeOutTime - System.currentTimeMillis());
	}
	
	public DatagramPacket getDatagram() {
		return packet;
	}
	
	public void updateTimeOut(int rtt) {
		this.timeSent = System.currentTimeMillis();
		this.timeOutTime = timeSent + rtt;
	}
	
	public long getTimeSent() {
		return this.timeSent;
	}
	
	public int getSeqNumber() {
		return converter.getBlockNumber();
	}
}
