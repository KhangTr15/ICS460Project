package testings;

import java.nio.ByteBuffer;

import packets.Packet;

public class Testing_Things {
	private static byte[] data = "Test.txt".getBytes();
	public static void main(String[] args) {
		
		System.out.println("Value from byte arr: "+getValueOfBytesInLoad(new byte[]{0,0,0,8}));
		System.out.println("This is text.txt arr:  "+data[0]+" "+data[1] + " " + data[2]+ " "+data[3]+ " " +data[4] + " "
				+data[5] + " " + data[6]+ " "+data[7]+ " "+data.length);
		
		System.out.println("This is Wong: "+getValueOfBytesInLoad(new byte[]{84,101,115,46}));
		data = setValueOfBytesInLoad(data.length);
		System.out.println("Value of bytes in load: "+getValueOfBytesInLoad(data));
		System.out.println(getValueOfBytesInLoad(data)+"Value of BYtes:"+data.length);
		
		
		
		
		System.out.println("KKK "+getValueOfBytesInLoad(new byte[]{0,0,0,8,84,101,115,46,116,120,116}));
	}
	
	
	public int getValueOfBytesInLoad(){
		byte[] sizeBytes = new byte[Packet.DATA_COUNT_SIZE];
		System.arraycopy(data, 0, sizeBytes, 0, Packet.DATA_COUNT_SIZE);
		return getValueOfBytesInLoad(sizeBytes);
	}
	
	public static int getValueOfBytesInLoad(byte[] sizeBytes){
		return ByteBuffer.wrap(sizeBytes).getInt();
	}
	public static byte[] setValueOfBytesInLoad(int bytes) {
		byte[] tmp = new byte[data.length + Packet.DATA_COUNT_SIZE];
		System.arraycopy(intToByteArr(bytes), 0, tmp, 0, Packet.DATA_COUNT_SIZE);
		System.arraycopy(data , 0, tmp, Packet.DATA_COUNT_SIZE, data.length);
		return tmp;
	}
	public static byte[] intToByteArr(int bytes){
		return ByteBuffer.allocate(4).putInt(bytes).array();
	}

}
