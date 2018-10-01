import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TFtpPacketV18 {
	
	// Valid TFTP Opcodes
	public static enum OpCode { OP_RRQ, OP_WRQ, OP_DATA, OP_ACK, OP_ERROR, OP_OACK;

		short toShort() {
			return (short)(ordinal() + 1) ;
		}
		
		static OpCode fromShort( short s ) {
			return values()[s - 1];
		}
	}

	private static final int UDP_HEADER_SIZE = 28;
	public static final int TFTP_BLOCK_SIZE = 512;
	public static final int MAX_TFTP_DATAGRAM_SIZE = TFTP_BLOCK_SIZE + UDP_HEADER_SIZE;
	
	protected ByteBuffer bb;

	/**
	 * Constructor for creating a new, initially, empty TftpPacket of a given OpCode.
	 * 
	 **/
	public TFtpPacketV18( OpCode opcode ) {
		this.bb = ByteBuffer.allocate(MAX_TFTP_DATAGRAM_SIZE);
		this.putShort( opcode.toShort() );
	}
	
	/**
	 * Constructor for decoding a TftpPacket contained in a given array of bytes...
	 * 
	 **/
	public TFtpPacketV18( byte[] data, int length) {
		this.bb = ByteBuffer.wrap(data, 0, length);
	}

	/**
	 * Returns this TFTP packet as a datagram addressed to the given destination...
	 * 
	 **/
	public DatagramPacket toDatagramPacket( SocketAddress dst ) {
		return new DatagramPacket( bb.array(), bb.position(), dst);
	}
	
	/**
	 * Returns the OpCode of this TFTP Packet
	 * 
	 */
	public OpCode getOpCode() {
		return OpCode.fromShort(bb.getShort(0));
	}

	/**
	 * Appends a byte to this TFTP Packet
	 * 
	 */
	public TFtpPacketV18 putByte(int b) {
		bb.put((byte) b);
		return this;
	}

	/**
	 * Appends a short (2 bytes, in net order) to this TFTP Packet
	 * 
	 */
	public TFtpPacketV18 putShort(int s) {
		bb.putShort((short) s);
		return this;
	}


	/**
	 * Appends length bytes of the given (block) byte array to this TFTP Packet
	 * 
	 */
	public TFtpPacketV18 putBytes(byte[] block, int length) {
		bb.put(block, 0, length);
		return this;
	}
	
	/**
	 * Appends the (block) byte array to this TFTP Packet
	 * 
	 */
	public TFtpPacketV18 putBytes(byte[] block) {
		return this.putBytes(block, block.length);
	}	
	
	/**
	 * Get the block number of this TFTP Packet, provided it is a DATA or an ACK packet
	 * 
	 */
	public int getBlockNumber() {
		OpCode opcode = this.getOpCode();
		if( opcode.equals( OpCode.OP_DATA) || opcode.equals( OpCode.OP_ACK ) || opcode.equals( OpCode.OP_OACK ))
			return bb.getShort(2);
		else 
			throw new IllegalAccessError("Operation not compatible with packet opcode...");
	}

	/**
	 * Get the error code of this TFTP Packet, provided it is an ERROR packet
	 * 
	 */
	public int getErrorCode() {
		OpCode opcode = this.getOpCode();
		if( opcode.equals( OpCode.OP_ERROR) )
			return bb.getShort(2);
		else 
			throw new IllegalAccessError("Operation not compatible with packet opcode...");
	}
	
	/**
	 * Get the error message of this TFTP Packet, provided it is an ERROR packet
	 * 
	 */
	public String getErrorMessage() {
		OpCode opcode = this.getOpCode();
		if( opcode.equals( OpCode.OP_ERROR) ) {
			return new String( bb.array(), 4, bb.limit() - (2+2+1));
		}
		else 
			throw new IllegalAccessError("Operation not compatible with packet opcode...");
	}
	
	/**
	 * Get the string value of a given option in the TFTP Packet, provided it is an OACK packet
	 * 
	 */
	public String getOptionValue( String option ) {
		OpCode opcode = this.getOpCode();
		if( opcode == OpCode.OP_OACK ) {
			String[] data = new String( bb.array(), 2, bb.limit() - 2).split("\0");
			int index = Arrays.asList( data ).indexOf( option );
			return index > 0 ? data[index+1] : null;
		}
		else 
			throw new IllegalAccessError("Operation not compatible with packet opcode...");
	}
	
}
