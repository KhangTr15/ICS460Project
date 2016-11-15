package checkSumTools;

public class CheckSumTools {
    public CheckSumTools() {

    }

    public byte[] byteArrayAppend(byte[] data, byte b1, byte b2){
        byte[] testData = new byte[data.length + 2];
        for(int i = 0; i < data.length; i++){
            testData[i] = data[i];
        }
        testData[data.length] = b1;
        testData[data.length+1] = b2;
        return testData;
    }

    private short shortAdd(short s1, short s2) {
        int answer = 0;
        answer = ( (s1 & 0xFFFF) + (s2 & 0xFFFF));
        int carry = answer & 0x10000;
        short accumulator = (short) answer;
        carry = carry >> 16;
        answer = accumulator + carry;
        return (short) answer;
    }

    public short createCheckSum(byte[] data) {
        short total = 0;
        for ( int i = 0; i < data.length; i += 2 ) {
            short s = 0;
            if ( i + 1 > data.length ) {
                s = (short) ( ( (data[i] & 0xFF) << 8) | (0));
            }
            s = (short) ( ( (data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF));
            total = shortAdd(s, total);
        }
        total = (short) ( ~total);
        return total;
    }

    public boolean checkSumTest(byte[] data) {
        short total = 0;
        for ( int i = 0; i < data.length; i += 2 ) {
            short s = 0;
            if ( i + 1 > data.length ) {
                s = (short) ( ( (data[i] & 0xFF) << 8) | (0));
            }
            s = (short) ( ( (data[i] & 0xFF) << 8) | (data[i + 1] & 0xFF));
            total = shortAdd(s, total);
        }
        total = (short) ( ~total);
        return total == 0;
    }
}
