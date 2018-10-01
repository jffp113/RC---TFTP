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
	private static DatagramSocket socket;
	private static int port;
	private static short seq;
	
	private static void updateSeq() {
		seq = (short) (++seq % 32768);
	}
	
	private static void sendDataWithACKCheck(DatagramPacket data,Stats stats) throws IOException {
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
				
				if(ackPk.getOpCode().equals(TFtpPacketV18.OpCode.OP_ACK) && 
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
		TFtpPacketV18.OpCode op = TFtpPacketV18.OpCode.OP_WRQ;
		TFtpPacketV18 pk = new TFtpPacketV18(op);
		
		byte[] payload = new byte[MAX_SIZE];
		
		pk.putBytes(fileName.getBytes());
		pk.putByte(0);
		pk.putBytes(MODE.getBytes());
		pk.putByte(0);
		
		DatagramPacket packet = pk.toDatagramPacket(new InetSocketAddress(addr, port));
		sendDataWithACKCheck(packet,stats);
		
	}
	
	private static void sendFile(Stats stats, String fileName,InetAddress server,int ports) throws Exception {
		FileInputStream in = new FileInputStream(fileName);
		int bytesRead = -1;
		boolean lastFullSize = false;
		byte[] fileContent = new byte[MAX_SIZE];
		TFtpPacketV18 pk = null;
		
		sendFileName(fileName,server,ports,stats);
		
		while((bytesRead = in.read(fileContent)) != -1) {
			
			pk = new TFtpPacketV18(TFtpPacketV18.OpCode.OP_DATA);
			pk.putShort(seq);
			pk.putBytes(fileContent,bytesRead);
			sendDataWithACKCheck(pk.toDatagramPacket(new InetSocketAddress(server, ports)),stats);
			
			stats.updateBytesRead(bytesRead);
			
			lastFullSize = MAX_SIZE == bytesRead;
		}
		
		if(lastFullSize) {
			pk = new TFtpPacketV18(TFtpPacketV18.OpCode.OP_DATA);
			pk.putShort(seq);
			sendDataWithACKCheck(pk.toDatagramPacket(new InetSocketAddress(server, ports)),stats);
		
		}
		
		in.close();
		socket.close();
	}
	
	public static void main(String[] args) throws Exception {
		InetAddress server;
		//int port;
		String fileName;
		
		switch (args.length) {
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
		sendFile(stats, fileName, server, port);
		stats.printReport();
	}

}
