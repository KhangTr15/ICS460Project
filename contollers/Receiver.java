/**
 * This is Receiver Class aka Server for ICS460 Class.
 * 
 * @author Andrey Yefermov, Javan Soliday, Khang Tran
 *
 */

package contollers;

import java.io.ByteArrayInputStream;
import java.util.Queue;

import checkSumTools.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Calendar;
import java.util.LinkedList;

import clientThreads.PacketReceiverThread;
import globals_constants.Constants;
import gui.ServerGUI;
import packets.Packet;

public class Receiver implements Runnable, Constants {

    private Packet ackPacket;
    private static int expectedSeqNum;
    private Packet[] buffer;
    private Packet[] bufferForWindow;
    private int buffersIndex;
    private DatagramPacket inStream;
    private DatagramPacket outStream;
    private DatagramSocket socket;
    private static boolean shutdowned;
    private DataOutputStream writer;
    private static Double errorRate;
    private static Double dropRate;
    private static int timeout = 2000;
    private long stopTime = 0;
    private PacketReceiverThread receiver;
    // https://github.com/tomersimis/Go-Back-N/blob/master/Receiver.java

    public Receiver() {

        resetAllVariables();
    }

    public void resetAllVariables() {
        ackPacket = new Packet(Packet.ACK);
        ackPacket.setAckno(1);
        buffer = new Packet[BUFFER_SIZE];
        expectedSeqNum = 1;
        buffersIndex = 0;
        errorRate = 0.3;
        dropRate = 0.3;
        outStream = null;
        inStream = null;
        socket = null;
        timeout = 5000;
        shutdowned = false;

    }

    private void cleanServer() {
        ackPacket = new Packet(Packet.ACK);
        ackPacket.setAckno(1);
        buffer = new Packet[BUFFER_SIZE];
        expectedSeqNum = 1;
        buffersIndex = 0;
        errorRate = 0.08;
        dropRate = 0.08;
        timeout = 2000;
        shutdowned = false;

    }

    public void startUDPServer() {
        out(Calendar.getInstance().getTime() + "\n" + "Server: Starting UDP Server on:" + PORT + " port............");
        out("....Waiting for incoming traffic.......\n");

        Queue<Packet> received = new LinkedList<Packet>(); // code I added
        CheckSumTools ckSum = new CheckSumTools();// CheckSumTools for processing checksums
        Packet pIn = null;
        String filesName = null;
        expectedSeqNum = STARTING_SEQ_NUM;

        try {
            socket = new DatagramSocket(PORT);
            socket.setSoTimeout(0);
            receiver = new PacketReceiverThread(socket);

            while ( !shutdowned ) {

                inStream = new DatagramPacket(new byte[BUFFER_SISE_OF_DATAGRAM], BUFFER_SISE_OF_DATAGRAM);
                if ( expectedSeqNum == STARTING_SEQ_NUM ) {
                    pIn = receiver.receivePacket(0, inStream);
                } else {
                    pIn = receiver.receivePacket(timeout, inStream);
                }

                if ( pIn != null && pIn.getSeqno() == STARTING_SEQ_NUM && expectedSeqNum == STARTING_SEQ_NUM ) {
                    out("\n\n" + Calendar.getInstance().getTime() + "\nServer: I got FIRST Packet");
                    filesName = this.getName(pIn);
                    out("Opening File For Writing: " + filesName);
                    writer = openFileToWrite(filesName);
                    ackPacket.setAckno(expectedSeqNum++ );
                    try {
                        pause(timeout);
                        this.respond(socket, inStream, ackPacket);
                        received.add(pIn); // added
                        out("Server: Sending ACK " + ackPacket.getAckno() + ">>>>>>>>>>>>>" + ackPacket);
                    } catch ( IOException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    continue;
                }

                // END OF TRANSACTION FILE HANDINGS
                if ( pIn != null && pIn.getSeqno() == END_OF_FLAG && expectedSeqNum > STARTING_SEQ_NUM ) {

                    out("Server: I got END_OF_TRANSACTION Packet\n" + pIn);
                    out("\n\nServer: I am Finishing writing file by Closing it>>>" + filesName + "\n");
                    try {
                        if ( writer != null )
                            writer.close();
                    } catch ( IOException e1 ) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    ackPacket.setAckno(END_OF_FLAG);
                    expectedSeqNum = STARTING_SEQ_NUM;
                    received.add(pIn); // added
                    filesName = "";
                    try {
                        pause(timeout);
                        this.respond(socket, inStream, ackPacket);
                    } catch ( IOException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    ackPacket = null;
                    cleanServer();
                    continue;
                }
                // process checksum data
                Boolean sumCheck = false;
                if ( pIn != null ) {
                    Short sum = pIn.getCksum();
                    byte b1 = (byte) ( (sum >> 8) & 0xFF); // splits checksum into two bytes
                    System.out.format("b1 = %5x\n", b1);
                    byte b2 = (byte) (sum & 0xFF);
                    System.out.format("b2 = %5x\n", b2);
                    sumCheck = ckSum.checkSumTest( (ckSum.byteArrayAppend(pIn.getData(), b1, b2)));
                    // adds bytes to data[] and checks the checksum and stores the boolean result
                    System.out.format("CheckSum is " + sumCheck + "\n");
                }
                if ( pIn == null && expectedSeqNum > STARTING_SEQ_NUM ) {
                    out(Calendar.getInstance().getTime() + "\nServer: TIMEOUT resending ACK......" + ackPacket);
                    continue;
                }
                // Added test for checksum here. I don't think you need the inner if though but
                // since it doesn't make a difference I left it.
                if ( pIn != null && pIn.getSeqno() > STARTING_SEQ_NUM && pIn.getSeqno() != END_OF_FLAG && sumCheck ) {
                    if ( !this.isDataPacket(pIn) || !isLegimativeDataPacket(pIn) ) {
                        out(Calendar.getInstance().getTime() + "\nServer: Data Packet is WRONG...disposing\n" + pIn);
                        continue;
                    }
                }

                if ( pIn != null && pIn.getSeqno() == END_OF_FLAG ) {
                    try {
                        pause(timeout);
                        this.respond(socket, inStream, getEndOfTransacktionACK());
                    } catch ( IOException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    continue;
                }

                out(Calendar.getInstance().getTime() + "\nServer: I got Packet:\n" + pIn);

                System.out.println("111111HJEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE " + pIn.getSeqno() +
                    " Does The Seq Num match the expected Seq Num? " + (pIn.getSeqno() == expectedSeqNum) +
                    " Expected Seq Num" + expectedSeqNum);
                System.out.println("Something Khang added" + received.peek());

                if ( pIn != null && pIn.getSeqno() == expectedSeqNum ) {
                    System.out.println("22222HJEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE " + pIn.getSeqno() +
                        " Does The Seq Num match the expected Seq Num? " + (pIn.getSeqno() == expectedSeqNum) +
                        " Expected Seq Num" + expectedSeqNum);
                    received.add(pIn);
                    out(Calendar.getInstance().getTime() + "\n" + "Server: I got RIGHT Data Packet\n" + pIn);
                    System.out.println("333332HJEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE " + pIn.getSeqno() +
                        " Does The Seq Num match the expected Seq Num? " + (pIn.getSeqno() == expectedSeqNum) +
                        " Expected Seq Num" + expectedSeqNum);

                    try {
                        if ( writer != null && pIn != null ) {
                            System.out.println("Succesfully wrote to Writer " + pIn.getSeqno() +
                                " Does The Seq Num match the expected Seq Num? " + (pIn.getSeqno() == expectedSeqNum) +
                                " Expected Seq Num" + expectedSeqNum);

                            writer.write(pIn.getData());
                        }
                    } catch ( IOException e1 ) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    try {
                        System.out.println("55555555HJEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE " + pIn.getSeqno() +
                            " Does The Seq Num match the expected Seq Num? " + (pIn.getSeqno() == expectedSeqNum) +
                            " Expected Seq Num" + expectedSeqNum);

                        if ( getProbability(errorRate) ) {
                            Packet p = new Packet(Packet.ACK);
                            p.setCksum((short) -23);
                            out("Server: SENDING ACK WITH ERROR >>> " + p);
                            pause(timeout);
                            this.respond(socket, inStream, p);
                        } else if ( getProbability(dropRate) ) {
                            out("Server: DROPPING ACK " + ackPacket.getAckno() + " >>> " + ackPacket);
                            pause(2000);
                        } else {
                            pause(timeout);
                            out("Server: Sending ACK " + ackPacket.getAckno() + " >>> " + ackPacket);
                            this.respond(socket, inStream, ackPacket);
                            ackPacket.setAckno(expectedSeqNum++ );

                        }
                    } catch ( IOException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    System.out.println("Managed to Re-send packet " + pIn.getSeqno() +
                        " Does The Seq Num match the expected Seq Num? " + (pIn.getSeqno() == expectedSeqNum) +
                        " Expected Seq Num" + expectedSeqNum);

                } else {
                    out(Calendar.getInstance().getTime() + "\n" + "Server: Packet I got is Wrong Disposing...\n" + pIn);
                    pIn = null;
                }
            }

        } catch ( SocketException e ) {
            if ( pIn != null )
                System.out.println("77777777777777777777777777772HJEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE" +
                    pIn.getSeqno() + " Does The Seq Num match the expected Seq Num? " +
                    (pIn.getSeqno() == expectedSeqNum) + " Expected Seq Num" + expectedSeqNum);
            System.out.println("77777777777777777777777777772HJEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE");
        }

    }

    private Packet getEndOfTransacktionACK() {
        Packet p = new Packet(Packet.ACK);
        p.setAckno(END_OF_FLAG);
        return p;
    }

    private void respond(DatagramSocket socket, DatagramPacket packet, Packet askPacket) throws IOException {
        byte[] outByteArr = serialize(askPacket);
        out(Calendar.getInstance().getTime() + "\n+Server: Sending Ack " + askPacket.getAckno() + " (" +
            outByteArr.length + ")bytes to send>>>>\n" + askPacket);
        DatagramPacket out = new DatagramPacket(outByteArr, outByteArr.length, packet.getAddress(), packet.getPort());
        socket.setSoTimeout(10000);
        socket.send(out);
        Thread.yield();

    }

    public static void setStartingSeqNum(int num) {
        expectedSeqNum = num;
    }

    public static void setErrorRate(Double errorRate1) {
        errorRate = errorRate1;
    }

    public static void setDropRate(Double dropRate1) {
        dropRate = dropRate1;
    }

    public boolean addPacketsToBuffer(Packet... packets) {
        if ( (buffer.length - buffersIndex) < packets.length )
            return false;
        int count = 0;
        for ( Packet p : packets ) {

            if ( addPacketToBuffer(p) ) {
                count++ ;
            }
        }
        return count == packets.length;
    }

    private boolean addPacketToBuffer(Packet p) {
        if ( p == null || buffer.length == buffersIndex )
            return false;

        buffer[buffersIndex++ ] = p;

        return true;
    }

    private boolean isBufferFull() {
        return buffersIndex == buffer.length;
    }

    private boolean isBufferEmpty() {
        return buffersIndex == 0;
    }

    public void clearBuffer() {
        buffer = new Packet[BUFFER_SIZE];
        buffersIndex = 0;
    }

    public DataOutputStream openFile(File file) throws FileNotFoundException {

        return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

    }

    private DataOutputStream openFileToWrite(String filesName) {
        try {
            return openFile(new File(filesName));
        } catch ( FileNotFoundException e ) {

        }
        return null;
    }

    private void sendAckWithError(DatagramSocket socket) {
        byte[] packet;

        try {
            ackPacket.setCksum((short) -12);
            packet = serialize(ackPacket);
            out("Server: is Sending>>>>>>" + deserialize(packet).toString());
            outStream = new DatagramPacket(packet, packet.length, inStream.getAddress(), inStream.getPort());
            socket.send(outStream);
        } catch ( IOException | ClassNotFoundException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void dumpBufferToDisk(DataOutputStream writer) {

        if ( !isBufferEmpty() ) {
            for ( int i = 0; i < buffersIndex; i++ ) {
                try {
                    writer.write(buffer[i].getData());

                } catch ( IOException e ) {

                }
            }
            clearBuffer();
        }

    }

    private void out(String msg) {
        ServerGUI.appendToOutput(msg);
        System.out.println(msg);
    }

    private boolean getProbability(double rate) {
        return Math.random() < rate;
    }

    private String getName(Packet pIn) {
        String result = null;
        try {
            result = new String(pIn.getData(), 0, pIn.getValueOfBytesInLoad(), "US-ASCII");
        } catch ( UnsupportedEncodingException e ) {

        }
        return result;
    }

    private void sendAck(DatagramSocket socket) {
        byte[] packet;

        try {
            packet = serialize(ackPacket);
            out("Server is Sending>>>>>>" + deserialize(packet).toString());
            outStream = new DatagramPacket(packet, packet.length, inStream.getAddress(), inStream.getPort());
            socket.send(outStream);
        } catch ( IOException | ClassNotFoundException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    // Removed the checksum check in here.
    private boolean isLegimativeDataPacket(Packet p) {
        if ( p == null )
            return false;

        return isDataPacket(p);
    }

    private boolean isDataPacket(Packet p) {
        if ( p == null )
            return false;
        return p.getLen() == Packet.DATA_LEN;
    }

    /*
     * Commented these out since I added the real checkSum processing.
     * 
     * private boolean checkChekSum(Packet packet) { return packet.getCksum() ==
     * calcCheckSum(packet); }
     * 
     * private short calcCheckSum(Packet packet) { // TODO Auto-generated method stub return 0; }
     * 
     * private short calcChekSum(byte[] nextChankOfData) { // TODO Auto-generated method stub return
     * 0; }
     */
    public boolean isEOTPAcket(Packet p) {
        return p.getLen() == Packet.DATA_LEN && p.getSeqno() == END_OF_FLAG;
    }

    public void pause(int mills) {
        try {
            Thread.sleep(mills);
        } catch ( InterruptedException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    @Override
    public void run() {
        // new ServerGUI();
        startUDPServer();

    }

    public static void Timeout(int timeout) {
        Receiver.timeout = timeout;

    }

    public static void setErrorRate(double errorRate) {
        Receiver.errorRate = errorRate;

    }
}
