
/**
 * This is Packet Class  for ICS460 Class. 
 * @author Andrey Yefermov, Javan Soliday, Khang Tran 
 *
 */package packets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

import javax.swing.JOptionPane;

import globals_constants.Constants;

public class Packet implements Serializable, Constants{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private short cksum; //16-bit 2-byte
	private short len; //16-bit 2-byte
	private int ackno; //32bit 4-byte
	private int seqno;//32bit 4-byte
	private byte[] data; //0-500 bytes. Data packet only Variable
	public final static int ACK = 1;
	public final static int ACK_LEN = 8;
	public final static int DATA = 0;
	public final static int DATA_LEN = 12;
	public final static int DATA_COUNT_SIZE = 4;
	private static int dataSize = DATA_SIZE;
	private int type;
	
	
	public Packet(int type){
		if(type==ACK){
			len=ACK_LEN;
		}
	}
	public Packet(int type, short cksum, int ackno){
		this(type);
		dataSize = DATA_SIZE;
		this.cksum = cksum;
		this.ackno = ackno;
	}
	public Packet(int type, int dataSize) {
		this(type);
		Packet.dataSize = dataSize;
		if(type==DATA){
			len=DATA_LEN;
			data =  new byte[dataSize];
		}
		
	}
	public Packet(byte[] data) {
		len=DATA_LEN;
		dataSize = DATA_SIZE;
		data =  new byte[dataSize];
	
		
	}
	
	public Packet(int type, int dataSize, short cksum, int ackno, int seqno, byte[] data, int dataCount) {
		this(type, dataSize);
		this.cksum = cksum;
		this.ackno = ackno;
		this.seqno = seqno;
		this.data = setValueOfBytesInLoad(dataCount, data);	
		//Packet.dataSize = DATA_SIZE;
		if(data.length<dataSize)
			this.data = createFixedSizeByteArray(this.data);
	}
	
	
	public short getCksum() {
		return cksum;
	}
	public void setCksum(short cksum) {
		this.cksum = cksum;
	}
	public short getLen() {
		return len;
	}
	public void setLen(short len) {
		this.len = len;
	}
	public int getAckno() {
		return ackno;
	}
	public void setAckno(int ackno) {
		this.ackno = ackno;
	}
	public int getSeqno() {
		return seqno;
	}
	public void setSeqno(int seqno) {
		this.seqno = seqno;
	}
	public byte[] getDataWithSizeBytes() {
		return data;
	}
	
	
	public byte[] getData() {
		byte[] answr = new byte[getValueOfBytesInLoad()];
		System.arraycopy(data, DATA_COUNT_SIZE, answr, 0, getValueOfBytesInLoad());
		return answr;
	}
	public void setData(byte[] data) {
		this.data = data;
	}
	public int getType() {
		return type;
	}
	public int getDataCapacity(){
		return data.length;
	}
	public int getValueOfBytesInLoad(){
		byte[] sizeBytes = new byte[Packet.DATA_COUNT_SIZE];
		System.arraycopy(data, 0, sizeBytes, 0, Packet.DATA_COUNT_SIZE);
		return getValueOfBytesInLoad(sizeBytes);
	}
	
	
	public static int getValueOfBytesInLoad(byte[] sizeBytes){
		return ByteBuffer.wrap(sizeBytes).getInt();
	}
	public byte[] setValueOfBytesInLoad(int bytes, byte[] data) {
		byte[] tmp = new byte[data.length + Packet.DATA_COUNT_SIZE];
		System.arraycopy(intToByteArr(bytes), 0, tmp, 0, Packet.DATA_COUNT_SIZE);
			
		System.arraycopy(data, 0, tmp, Packet.DATA_COUNT_SIZE, data.length);
		return tmp;
	}

	public static byte[] intToByteArr(int bytes){
		return ByteBuffer.allocate(4).putInt(bytes).array();
	}
	
	
	public static byte[] serialize(Packet pack) throws IOException {
	    ByteArrayOutputStream out = new ByteArrayOutputStream();
	    ObjectOutputStream os = new ObjectOutputStream(out);
	    os.writeObject(pack);
	    return out.toByteArray();
	}
	public static Packet deserialize(byte[] data) throws IOException, ClassNotFoundException {
	    ByteArrayInputStream in = new ByteArrayInputStream(data);
	    ObjectInputStream is = new ObjectInputStream(in);
	    return (Packet) is.readObject();
	}
	public static byte[] createFixedSizeByteArray(byte[] data){
		if(data == null)
			return new byte[dataSize];
		
		if(data.length==dataSize)
			return data;
		byte[] answr = new byte[dataSize];
		System.arraycopy(data, 0, answr, 0, data.length);
		
		return answr;
	}
	public int readAckNum(Packet packet){
		return packet.getAckno();
	}
	
	
	public String toString(){
		
		String out = "";
		if(len==Packet.DATA_LEN){
			out+="Data";
		}else{
			out+="ACK";
		}
		
		
		out+=" Packet has:"
				+ "\n\t---Len: "+len
				+ "\n\t---cksum: "+cksum
				+ "\n\t---ackno: "+ackno;
		if(len==Packet.DATA_LEN){
			out+= "\n\t---seqno: "+seqno
					+ "\n\t---data size: " +data.length
					+ "\n\t---load size: " +getValueOfBytesInLoad();
		}
		return  out;
					
	}
	
}
