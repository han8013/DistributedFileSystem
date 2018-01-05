import java.io.*;

public class Utils {

    public static byte[] objectToByteArray(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(object);
        byte[] result = bos.toByteArray();
        out.close();
        bos.close();
        return result;
    }

    public static Object byteArrayToObject(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = new ObjectInputStream(bis);
        return in.readObject();
    }

    public static String intToIP(int key) {
        if (key >= 1 && key <= 8) {
            int num = key + 1;
            return "172.22.154.10" + num;
        } else if (key == 9){
            return "172.22.154.110";
        } else if (key == 10) {
            return "172.22.154.111";
        }
        return "127.0.0.1";
    }

    public static int ipToInt(String ip) {
        return ((Integer.parseInt("" + ip.charAt(ip.length() - 1)) + 8) % 10) + 1;
    }

    public static void println(String s) {
        System.out.println(s);
    }

}
