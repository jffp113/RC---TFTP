

/**
 * fcpServer - a very simple TFTP like server - RC FCT/UNL
 * 
 * Limitations of this server:
 * 		- ignores mode (always works as octal)
 * 		- assumes just one client at a time
 * 		- file transfer and timeouts processing are simplified
 *      - assumes that all Java Strings used contain only
 *        ASCII characters. If it's not so, lenght() 
 *        and getBytes().lenght return different sizes
 *        and unexpected problems can appear ... 
 */

import java.io.*;
import java.net.* ;


public class TftpServer {

	static final int PORT = 6969 ; 		// my default port
	static DatagramSocket socket;
	static final int TIMEOUT = 4000; 	// 4 sec.
	static final int BLOCKSIZE = TFtpPacketV18.TFTP_BLOCK_SIZE;  	// default block size 

	
	public static void main(String[] args) throws Exception {

		// create and bind socket to port for receiving client requests
		socket = new DatagramSocket( PORT ) ;

		
		// prepare an empty datagram ...
		byte[] buffer = new byte[TFtpPacketV18.MAX_TFTP_DATAGRAM_SIZE] ;

		for(;;) { // infinite processing loop...	
			System.out.println("NEW tftp like server ready at port "+socket.getLocalPort());
			DatagramPacket msg = new DatagramPacket( buffer, buffer.length );
			System.out.println("Waiting Request");
			socket.receive( msg );
			// look at data as a TFTP packet
			TFtpPacketV18 req = new TFtpPacketV18( msg.getData(), msg.getLength());
			TFtpPacketV18.OpCode op = req.getOpCode();
			switch ( op ) {
				case OP_RRQ: // Read Request 
					System.out.println("Read Request");
					break;
				case OP_WRQ: // Write Request
					System.out.println("Write Request");
					
					//receiveFile( req.getFileName(), msg.getAddress(), msg.getPort() );
					receiveFile( "file", msg.getAddress(), msg.getPort() );
					break;
				default: // error!
					System.err.printf("? Request %d ignored\n", req.getOpCode());
					sendError(0, "Unknown request", msg.getAddress(), msg.getPort() );
			}
		}
	}

	private static void sendError(int err, String str, InetAddress client, int port ) {
		TFtpPacketV18 reply = new TFtpPacketV18(TFtpPacketV18.OpCode.OP_ERROR).putShort(err)
				.putBytes(str.getBytes()).putByte(0);
		try {
			socket.send( reply.toDatagramPacket(new InetSocketAddress(client,port)));
		} catch (IOException e) {
			System.err.println("failed to send error datagram");		} 
	}

	private static void sendACK( int count, InetAddress client, int port ) {
		TFtpPacketV18 reply = new TFtpPacketV18(TFtpPacketV18.OpCode.OP_ACK).putShort(count);
		System.out.printf("sent:  %d ack \n", count);
		try {
			socket.send( reply.toDatagramPacket(new InetSocketAddress(client,port)));
		} catch (IOException e) {
			System.err.println("failed to send ack datagram");
		} 
	}
	

	
	/**
	 * Receives a file from a client
	 * 
	 * @param file	File name to receive from client
	 * @param host	client address
	 * @param port	client port
	 */
	private static void receiveFile(String file, InetAddress client, int port) {
		System.out.println("receiving file: \""+file+"\"");
		// prepare an empty datagram ...
		byte[] buffer = new byte[TFtpPacketV18.MAX_TFTP_DATAGRAM_SIZE] ;
		DatagramPacket msg = new DatagramPacket( buffer, buffer.length ) ;
		try {
			FileOutputStream f = new FileOutputStream(file);
			sendACK(0, client, port);
			int count=1; // expecting first block
			boolean last = false;
			do {
				socket.receive( msg ) ;
				TFtpPacketV18 pkt = new TFtpPacketV18(msg.getData(),msg.getLength());
				if (pkt.getOpCode() == TFtpPacketV18.OpCode.OP_DATA && pkt.getBlockNumber() == count ) {
					int n = msg.getLength() - 4; // received block size
					f.write( msg.getData(), 4, n );			
					System.out.printf("received: %d/%d bytes\n", count, n );
					try {
						Thread.sleep(5);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					sendACK(count, client, port );
					count = ++count % 32768 ;
					last = n < BLOCKSIZE;
				} else if ( pkt.getOpCode() == TFtpPacketV18.OpCode.OP_DATA && pkt.getBlockNumber() != count ) {
					sendACK( count - 1, client, port );
				} else	System.err.printf("received packet from client ignored\n");
			} while ( !last );
			f.close();
		} catch (FileNotFoundException e) {
			System.err.printf("Can't write file \"%s\"\n", file);
			sendError(6, "Impossible to write file", client, port);
		} catch (IOException e) {
			System.err.println("Failed with IO error (file or socket)\n");
		}
	}
}
