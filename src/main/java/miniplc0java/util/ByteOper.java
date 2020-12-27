package miniplc0java.util;

public class ByteOper {
    public static byte[] byteMerger(byte[] bt1, byte[] bt2){
        byte[] bt3 = new byte[bt1.length+bt2.length];
        int i=0;
        for(byte bt: bt1){
            bt3[i]=bt;
            i++;
        }

        for(byte bt: bt2){
            bt3[i]=bt;
            i++;
        }
        return bt3;
    }

    public static byte[] toBytes(String string){
        return string.getBytes();
    }

    public static byte[] toBytes(long x, int offset){
        byte[] res = new byte[offset/8];
        int len = offset/8;
        for(int i=0;i<len;i++){
            offset -= 8;
            res[i] = (byte)((x>>offset)&0xFF);
        }
        return res;
    }
}
