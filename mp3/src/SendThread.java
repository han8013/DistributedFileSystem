import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SendThread implements Runnable {
    private byte[] message;
    private String ip;

    public SendThread(byte[] message, String ip) {
        this.message = message;
        this.ip = ip;
    }

    @Override
    public void run() {
        try {
            InetAddress receiverHost = InetAddress.getByName(ip);
            DatagramSocket socket = new DatagramSocket();
            DatagramPacket packet = new DatagramPacket(message, message.length, receiverHost, MP3Server.port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
