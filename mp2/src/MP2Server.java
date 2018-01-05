import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Date;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MP2Server extends Thread {
    private DatagramSocket receiveSocket;
    private Membership membership = new Membership();
    public static boolean flag = true;
    public static String introduceIP = "172.22.154.111";
    public static String myIP;
    public static int id;
    public static int port = 8000;
    private static final Logger log = Logger.getLogger(MP2Server.class.getName());
    public static FileHandler fh;

    @Override
    public void run() {
        membership.startMembershipSpread();
        membership.startHeartbeat();
        membership.startDetection();
        while (true) {
            Request request = receiveRequest();
            handleRequest(request);
        }
    }

    private void handleRequest(Request request) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                if (flag) {
                    // management
                    if (request.getAction().equals("join")) {
                        log.info("Recv join request.");
                        membership.responseJoin(request);
                    } else if (request.getAction().equals("leave")) {
                        log.info("Recv leave request.");
                        membership.responseLeave(request);
                    } else if (request.getAction().matches("update.*")) {
                        log.info("Recv update request." + request.getAction());
                        membership.updateMembershipList((TreeMap<Integer, Member>) request.getData());
//                            membership.gossipSend(request);
                    } else if (request.getAction().equals("heartbeat")) {
                        if (request.getData() != null) {
                            log.info("Recv heartbeat request from " + ((Member) request.getData()).getId());
                            membership.updateHeartbeat((Member) request.getData());
                        }
                    }
                }
            }
        };
        thread.start();
    }

    public void joinMembership(int id) {
        // update membership from incoming update package.

        flag = true;
        if (id < 10 && id > 0) {
            //send join request to introducer
            Member member = new Member(myIP, 0);
            member.setLatestUpdateTime(new Date());
            Request request = new Request("join", member, 0);
            sendMessage(request, introduceIP);
            log.info("send join request to introducer.");
        } else {
            //waiting for rejoin from network
//            membershipManagement.responseJoin(new Member(myIP,0));
            Member member = new Member(myIP, 0);
            member.setLatestUpdateTime(new Date());
            if (!membership.membershipList.containsKey(member.getId()))
                membership.membershipList.put(member.getId(), member);
            System.out.println("I am introducer, waiting for other join or rejoin.");
            System.out.println("Add to network");

        }
    }

    public void leaveMembership() {
        flag = false;
        this.membership.leaveCluster();
        log.info("clear own membership list.");
    }

    public void sendMessage(Request request, String ip) {
        try {
            Thread send = new Thread(new SendThread(Utils.objectToByteArray(request), ip));
            send.start();
        } catch (IOException e) {
            //todo: log
            log.info("sendMessage Failed");
            e.printStackTrace();
        }
    }
    
    public void printMembershipList() {
        System.out.println(membership.toString());
    }

    public Request receiveRequest() {
        try {
            if (receiveSocket == null)
                receiveSocket = new DatagramSocket(port);
            int MESSAGE_LEN = 4096;
            byte[] recvBuffer = new byte[MESSAGE_LEN];
            DatagramPacket dataPacket = new DatagramPacket(recvBuffer, MESSAGE_LEN);
            receiveSocket.receive(dataPacket);
//            String from_ip = dataPacket.getAddress().toString();
//            log.info("Get packet from "+from_ip);

            byte[] recvData = new byte[dataPacket.getLength()];
            System.arraycopy(recvBuffer, 0, recvData, 0, dataPacket.getLength());

            return (Request) Utils.byteArrayToObject(recvData);

        } catch (Exception e) {
            e.printStackTrace();
            log.info("receiveRequest Exception");
            return null;
        }
    }
    
    public static void main(String args[]) throws InterruptedException, IOException {

        myIP = args[0];
        System.out.println("myIp: " + myIP);
        id = Integer.valueOf(myIP.substring(myIP.length() - 2, myIP.length())) - 1;

        fh = new FileHandler("log.txt", true);   // true forces append mode
        SimpleFormatter sf = new SimpleFormatter();
        fh.setFormatter(sf);
        log.addHandler(fh);
        log.setUseParentHandlers(false);


        MP2Server serverThread = new MP2Server();
        serverThread.start();
        // Reading from System.in
        Scanner reader = new Scanner(System.in);

        while (true) {
            System.out.println("Enter a cmd: ");
            // Scans the next token of the input as an string.
            String cmd = reader.nextLine();
            if (cmd.equals("list id")) {
                System.out.println("My ID: " + id);
                if (id == 10)
                    System.out.println("This is a introducer.");

            } else if (cmd.equals("join")) {

                serverThread.joinMembership(id);

            } else if (cmd.equals("lm")) {
                serverThread.printMembershipList();

            } else if (cmd.equals("leave")) {
                serverThread.leaveMembership();
            }
//            System.out.println("------------------------------------------");
//            System.out.println("Current List: " + new Date().toString());
//            serverThread.printMembershipList();
//            System.out.println("------------------------------------------");
        }
    }
}
