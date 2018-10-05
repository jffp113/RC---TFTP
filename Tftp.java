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
	private static final int WINDOW_SIZE = 10;
	
	private static DatagramSocket socket;
	private static int port;
	private static short seq;
	private static int maxBlockSize;
	
	private static void updateSeq() {
		seq = (short) (++seq % 32768);
	}
	
	private static void inicialProtocol(DatagramPacket data,Stats stats) throws IOException {
		byte[] ackMen = new byte[maxBlockSize];
		DatagramPacket ack = new DatagramPacket(ackMen, ackMen.length);
		TFtpPacketV18 ackPk = null;
		long startTime = System.currentTimeMillis();
		long endTime = 0;
		
		socket.send(data);
		while(true) {
			socket.setSoTimeout(stats.getTimeOut());
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
					maxBlockSize = MAX_SIZE;
					break;
				}
				else if(TFtpPacketV18.OpCode.OP_OACK.equals(ackPk.getOpCode())) {
					maxBlockSize = Integer.parseInt(ackPk.getOptionValue("blksize"));
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
		
		if(maxBlockSize > MAX_SIZE) {
			pk.putBytes("blksize".getBytes());
			pk.putByte(0);
			pk.putBytes(String.valueOf(maxBlockSize).getBytes());
			pk.putByte(0);
		}
		DatagramPacket packet = pk.toDatagramPacket(new InetSocketAddress(addr, port));
		inicialProtocol(packet,stats);
		
	}
		
	public static void ackReceiveFile(Window packetWindow,Stats stats) throws IOException {
		byte[] ackMen = new byte[maxBlockSize];
		DatagramPacket ack = new DatagramPacket(ackMen, ackMen.length);
		TFtpPacketV18 ackPk = null;
		int timeout;
		
		timeout = packetWindow.getFirstTimeOut();
		if(timeout > 0)
			socket.setSoTimeout(timeout);
		else {
			packetWindow.goBackN();
			socket.setSoTimeout(stats.getTimeOut());
		}
		
		try {
			socket.receive(ack);
			stats.updateAck();
			
			//Remove The block if seq number match else GBN
			ackPk = new TFtpPacketV18(ackMen,ack.getLength());
			packetWindow.removeDatagramPacket(ackPk.getBlockNumber());
			
		}catch(SocketTimeoutException e) {
			packetWindow.goBackN();
		}
		
		
	}
	
	private static void sendFile(Stats stats, String fileName,InetAddress server) throws Exception {
		FileInputStream in = new FileInputStream(fileName);
		Window packetWindow = new Window(WINDOW_SIZE,socket,stats); 
		int bytesRead = -1;
		boolean lastFullSize = false;
		
		sendFileName(fileName,server,port,stats);
		
		byte[] fileContent = new byte[maxBlockSize];
		TFtpPacketV18 pk = null;
		
		while(in.available() != 0 || !packetWindow.isEmpty()) {
			
			while(!packetWindow.isFull() && (bytesRead = in.read(fileContent)) != -1) {
				pk = new TFtpPacketV18(TFtpPacketV18.OpCode.OP_DATA,maxBlockSize);
				pk.putShort(seq);
				pk.putBytes(fileContent,bytesRead);
				packetWindow.putDatagramPacket(pk.toDatagramPacket(new InetSocketAddress(server, port)));
				seq++;
				stats.updateBytesRead(bytesRead);
				lastFullSize = bytesRead == maxBlockSize;
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
				server = InetAddress.getByName(args[0]);
			    port = Integer.valueOf(args[1]);
			    fileName = args[2];
			    socket = new DatagramSocket();
			    seq = 0;
				maxBlockSize = Integer.valueOf(args[3]);
				break;
		    case 3:
		        server = InetAddress.getByName(args[0]);
		        port = Integer.valueOf(args[1]);
		        fileName = args[2];
		        socket = new DatagramSocket();
		    	seq = 0;
		    	maxBlockSize = MAX_SIZE; 
		        break;
		    default:
		        System.out.printf("usage: java %s server port filename (blkSize)* \n", Tftp.class.getName());
		        System.out.printf("All options surrounded by * are optional\n");
		        return;
	}

		Stats stats = new Stats();
		sendFile(stats, fileName, server);
		stats.printReport();
	}

	
}
