package miniplc0java.util;

public class MyByte {
    public static byte[] toBytes(long x, int len) {
        byte[] result = new byte[len];
        int offset = len*8;
        for(int i=0;i<len;i++){
            offset -= 8;
            result[i] = (byte)((x>>offset)&0xFF);
        }
        return result;
    }
    public static byte[] toBytes(String str){
        return str.getBytes();
    }

    public static String bytesToString(byte[] bytes){
        String str = "";
        for(byte by : bytes){
            str += String.format("%02x",by);
        }
        return str;
    }

    public static byte[] merge(byte[] a,byte []b){
        byte[]c =  new byte[a.length+b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
    public static byte[] merge(byte[] a,String str){
        return merge(a,toBytes(str));
    }
    public static byte[] merge(byte[] a,long x,int len){
        return merge(a,toBytes(x,len));
    }
}
