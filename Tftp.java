import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;


public class Tftp {
	
	private static final int MAX_SIZE = 512;
	private static final String MODE = "octet";
	private static final int WINDOW_SIZE = 5;
	
	private static DatagramSocket socket;
	private static int port;
	private static short seq;
	
	private static void updateSeq() {
		seq = (short) (++seq % 32768);
	}
	
	private static void inicialProtocol(DatagramPacket data,Stats stats) throws IOException {
		byte[] ackMen = new byte[MAX_SIZE];
		DatagramPacket ack = new DatagramPacket(ackMen, ackMen.length);
		TFtpPacketV18 ackPk = null;
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		
		socket.send(data);
		while(true) {
			socket.setSoTimeout(stats.getRTT());
			try {
				socket.receive(ack);
				endTime = System.currentTimeMillis();
				stats.updateRTT(endTime - startTime);
				ackPk = new TFtpPacketV18(ackMen,ack.getLength());
				
				//Update port if server decides to change
				port = ack.getPort();
				
				if(ackPk.getOpCode() == TFtpPacketV18.OpCode.OP_ACK && 
					seq == ackPk.getBlockNumber()) {
					updateSeq();
					stats.updateAck();
					break;
				}
			
				throw new SocketTimeoutException();
				
			}
			catch(SocketTimeoutException e) {
				startTime = System.currentTimeMillis();
				socket.send(data);
			}
		}
	}
	
	private static void sendFileName(String fileName,InetAddress addr, int port,Stats stats) throws IOException {
		
		TFtpPacketV18 pk = new TFtpPacketV18(TFtpPacketV18.OpCode.OP_WRQ);
		
		pk.putBytes(("/tmp/"+ fileName).getBytes());
		pk.putByte(0);//short
		pk.putBytes(MODE.getBytes());
		pk.putByte(0);
		
		DatagramPacket packet = pk.toDatagramPacket(new InetSocketAddress(addr, port));
		inicialProtocol(packet,stats);
		
	}
		
	public static void ackReceiveFile(Window packetWindow,Stats stats) throws IOException {
		byte[] ackMen = new byte[MAX_SIZE];
		DatagramPacket ack = new DatagramPacket(ackMen, ackMen.length);
		TFtpPacketV18 ackPk = null;
		boolean successACK = false;
		long packetTimeSent;
		int timeout;
		
		timeout = packetWindow.getFirstTimeOut();
		if(timeout > 0)
			socket.setSoTimeout(packetWindow.getFirstTimeOut());
		else {
			packetWindow.goBackN(stats.getRTT());
			socket.setSoTimeout(stats.getRTT());
		}
		try {
			socket.receive(ack);
			stats.updateAck();
			packetTimeSent = packetWindow.getPacketTimeSent();
			
			//Remove The block if seq number match else GBN
			ackPk = new TFtpPacketV18(ackMen,ack.getLength());
			successACK = packetWindow.removeDatagramPacket(ackPk.getBlockNumber(),stats.getRTT());
			
			//Update RTT time 
			if(successACK)
				stats.updateRTT(System.currentTimeMillis() - packetTimeSent);
		
		}catch(SocketTimeoutException e) {
			packetWindow.goBackN(stats.getRTT());
			System.out.println("Timeout");
		}
		
		
	}
	
	private static void sendFile(Stats stats, String fileName,InetAddress server) throws Exception {
		FileInputStream in = new FileInputStream(fileName);
		Window packetWindow = new Window(WINDOW_SIZE,socket,stats); 
		int bytesRead = -1;
		boolean lastFullSize = false;
		byte[] fileContent = new byte[MAX_SIZE];
		TFtpPacketV18 pk = null;
		
		sendFileName(fileName,server,port,stats);
		
		while(in.available() != 0 || !packetWindow.isEmpty()) {
			
			while(!packetWindow.isFull() && (bytesRead = in.read(fileContent)) != -1) {
				pk = new TFtpPacketV18(TFtpPacketV18.OpCode.OP_DATA);
				pk.putShort(seq);
				pk.putBytes(fileContent,bytesRead);
				packetWindow.putDatagramPacket(pk.toDatagramPacket(new InetSocketAddress(server, port)));
				seq++;
				stats.updateBytesRead(bytesRead);
				
				lastFullSize = bytesRead == MAX_SIZE;
			}
	
			ackReceiveFile(packetWindow,stats);
			
		}
		
		if(lastFullSize) {
			pk = new TFtpPacketV18(TFtpPacketV18.OpCode.OP_DATA);
			pk.putShort(seq);
			socket.send(pk.toDatagramPacket(new InetSocketAddress(server, port)));
		}
			
		in.close();
		socket.close();
	}
	
	
	public static void main(String[] args) throws Exception {
		InetAddress server;
		//int port;
		String fileName;
		
		switch (args.length) {
			case 4: 
				
		    case 3:
		        server = InetAddress.getByName(args[0]);
		        port = Integer.valueOf(args[1]);
		        fileName = args[2];
		        socket = new DatagramSocket();
		    	seq = 0;
		        break;
		    default:
		        System.out.printf("usage: java %s server port filename\n", Tftp.class.getName());
		        return;
	}

		Stats stats = new Stats();
		sendFile(stats, fileName, server);
		stats.printReport();
	}

	
}
