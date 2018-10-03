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
			packetsWindow.add(new DatagramNode(packet,stats.getRTT()));
			socket.send(packet);
		}
	}
	
	public boolean removeDatagramPacket(int seqNumber,int rtt) throws IOException {
		if(!isEmpty()) {
				
				if(isFirst(seqNumber)) {
					packetsWindow.remove(0);
					return true;
				}else 
					this.goBackN(rtt);
		}
		return false;
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
	
	public void goBackN(int rtt) throws IOException {
		Iterator<DatagramNode> it = packetsWindow.iterator();
		DatagramNode next;
		
		while(it.hasNext()) {
			next = it.next();
			socket.send(next.getDatagram());
			next.updateTimeOut(rtt);
		}
	}
	
	public int getFirstTimeOut() {
		return packetsWindow.get(0).getTimeOut();
	}
	
	public long getPacketTimeSent(){
		if(isEmpty())
			return stats.getRTT();
		
		return packetsWindow.get(0).getTimeSent();
	}
	
}
