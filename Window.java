import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Window {
	
	private int n;
	private DatagramSocket socket;
	private Stats stats;
	private List<DatagramNode> packetsWindow;
	
	public Window(int n, DatagramSocket socket,Stats stats) {
		this.n = n;
		this.socket = socket;
		this.stats = stats;
		packetsWindow = new LinkedList<DatagramNode>();
	}
	
	public void putDatagramPacket(DatagramPacket packet) throws IOException{
		if(!isFull()) {
			packetsWindow.add(new DatagramNode(packet,stats.getTimeOut()));
			socket.send(packet);
		}
	}
	
	public void removeDatagramPacket(int seqNumber) throws IOException {
		/*if(!isEmpty()) {	
				/*if(isFirst(seqNumber)) {
					stats.updateRTT(System.currentTimeMillis() - packetsWindow.get(0).getTimeSent());
					packetsWindow.remove(0);
					return true;
				}else {
					this.goBackN();
				}
		}*/
		if(isFirst(seqNumber))
			stats.updateRTT(System.currentTimeMillis() - packetsWindow.get(0).getTimeSent());
			
		while(!isEmpty() && seqNumber >= packetsWindow.get(0).getSeqNumber()) {
			packetsWindow.remove(0);
		}
		
	}
	
	private boolean isFirst(int seqNumber) {
		return seqNumber == packetsWindow.get(0).getSeqNumber();
	}
	
	public boolean isFull() {
		return n == packetsWindow.size();
	}
	
	public boolean isEmpty() {
		return packetsWindow.isEmpty();
	}
	
	public void goBackN() throws IOException {
		Iterator<DatagramNode> it = packetsWindow.iterator();
		DatagramNode next;
		
		while(it.hasNext()) {
			next = it.next();
			next.updateTimeOut(stats.getTimeOut());
			socket.send(next.getDatagram());
		}
	}
	
	public int getFirstTimeOut() {
		return packetsWindow.get(0).getTimeOut();
	}
	
	public long getPacketTimeSent(){
		if(isEmpty()) {
			return stats.getTimeOut();
		}
		return packetsWindow.get(0).getTimeSent();
	}
	
}
