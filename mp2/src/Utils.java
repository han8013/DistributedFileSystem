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


}
