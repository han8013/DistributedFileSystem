import entity.Member;
import entity.Request;
import entity.Response;
import entity.SDFSFile;
import graph.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MP4Server extends Thread {
    private DatagramSocket receiveSocket;
    private static Membership membership = new Membership();
    public static boolean flag = true;
    public static String introduceIP = "172.22.154.111";
    public static String standbyMaster = "172.22.154.110";
    public static String myIP;
    public static int id;
    public static int port = 8000;
    private static final Logger log = Logger.getLogger(MP4Server.class.getName());
    public static FileHandler fh;
    public static Master master;
    public static Worker worker;


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
//                            System.out.println("XXXXXXXXXX Recv heartbeat request from " + ((Member) request.getData()).getId());
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
            if (!membership.membershipList.containsKey(member.getId())) {
                membership.membershipList.put(member.getId(), member);
                SDFSServer.fileManager.updateFileList(membership.membershipList);
            }
            System.out.println("I am introducer, waiting for other join or rejoin.");
            System.out.println("Add to network");

        }
    }

    public void leaveMembership() {
        flag = false;
        membership.leaveCluster();
        log.info("clear own membership list.");
    }

    public void sendMessage(Request request, String ip) {
        try {
            Thread send = new Thread(new SendThread(Utils.objectToByteArray(request), ip));
            send.start();
        } catch (IOException e) {
            log.info("sendMessage Failed");
            e.printStackTrace();
        }
    }

    public static Response sendTCPMessage(Request request, String ip) {
        return SDFSServer.fileManager.sendPackage(ip, request);
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

    public static void startGraphTask(String application, String fileName) {
        master = new Master(id, myIP);
        master.initApplication(application);
        master.setInputFilename(fileName);
        List<Member> workerList = membership.getWorkerList();
        List<Integer> workerIDList = new ArrayList<>();
        for (Member member : workerList) {
            workerIDList.add(member.getId());
        }
        Collections.sort(workerIDList);
        master.setWorkerIDList(workerIDList);
        master.setWorkerList(workerList);
        SDFSServer.fileManager.getFile("input.data", fileName);
        master.loadData("input.data");
        master.getGraph().setApplicationName(application);
        master.printRuntime();
        startSuperStep(master);
        Utils.println("Super step " + master.getSuperStep() + " started.");

    }

    public static void startSuperStep(Master master) {
        List<Member> workerIDList = master.getWorkerList();

        Map<Integer, List<Vertex>> partitionMap = master.getPartitionMap();
        Utils.println("Function startSuperStep: Finish partition data");
        List<Thread> threads = new ArrayList<>();

        for (Member member : workerIDList) {
            int step = master.getSuperStep();
            Thread thread = new Thread(() -> {
                List<Vertex> data = partitionMap.get(member.getId());

                // first send Graph metadata
                Graph metadata = master.getGraph();
                Request requestOfMetadata = new Request("graphMetadata", metadata);
                sendTCPMessage(requestOfMetadata, member.getIp());
                Utils.println("Function startSuperStep: Sent graph metadata data");

                // superStep initial as zero
                Request request = new Request("superStep" + " " + step, data);
                sendTCPMessage(request, member.getIp());
                Utils.println("Function startSuperStep: Sent vertex data");
            });
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        master.printRuntime();

    }

    public static void masterUpdateSuperStep(int id, int superStep, boolean finish) {

        // Aggregate all result of each worker
//        master.updateWorkStep(superStep, data);
        master.getWorkerStepMap().put(id, superStep);
        master.getFinishMap().put(id, finish);
        boolean isFinish = master.isFinish();
        // finished when all vertice's isActive is false
        if (!isFinish && master.finishPrevStep()) {
            master.setSuperStep(master.getSuperStep() + 1);
            master.printRuntime();
            doNextSuperStep();
        } else if (isFinish && master.finishPrevStep()) {
            aggragateAllResult();
            Utils.println("Graph processing finished.");
        }

//        Utils.println("Function masterUpdateSuperstep: finish update superStep ");

    }

    public static void aggragateAllResult() {
        //Get result from Master.getTempResult()
        //For PageRank, print top 20 page(get top 20 from each worker ?)
        //For find smallest node id, print smallest node id
        Utils.println("Function aggragateAllResult: Aggragate result of worker......");
        Object mutex = new Object();
        List<Vertex> vertices = new ArrayList<>();
        ArrayList<Thread> threads = new ArrayList<>();
        for (int id : master.getWorkerIDList()) {
            Thread thread = new Thread(() -> {
                Request request = new Request("collect", null);
                Response response = sendTCPMessage(request, Utils.intToIP(id));
                synchronized (mutex) {
                    vertices.addAll((List<Vertex>) response.getData());
                }
            });
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        master.aggregateResult(vertices);
        System.out.println("Total runtime: " + master.getRuntime() + "s");
    }

    public static void doNextSuperStep() {
        List<Member> workerList = master.getWorkerList();
        updateStandbyMaster();
        for (Member member : workerList) {
            new Thread(() -> {
                int superStep = master.getSuperStep();
                Request request = new Request("superStep" + " " + superStep, "USING PREV STEP DATA");
                sendTCPMessage(request, member.getIp());
            }).start();
        }
        Utils.println("Super step " + master.getSuperStep() + " started");
    }

    public static void updateStandbyMaster() {
        if (myIP.equals(introduceIP)) {
            try {
                new Thread(() -> {
                    Request request = new Request("masterSync", master);
                    sendTCPMessage(request, standbyMaster);
                }).start();
            } catch (Exception e) {

            }
        }
    }

    public static void workerUpdateMetadata(Graph graph) {
        worker = new Worker(id, myIP);
        worker.initApplication(graph.getApplicationName());
        worker.setGraph(graph);
        Utils.println("Function workerUpdateMetadata: Update graph metadata");
    }

    public static List<Vertex> collectAllVertices() {
        return new ArrayList<>(worker.getVertexMap().values());
    }

    public static void workerUpdateSuperStep(int superStep, List<Vertex> data, String masterIP) {
        Utils.println("==== Start super step " + superStep + " ====");
        if (superStep == 0) {
            if (worker == null) {
                worker = new Worker(id, myIP);
            }
            worker.loadData(data);
            Utils.println("Function workerUpdateSuperStep: Update graph metadata");
            while (worker.getGraph() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        worker.setSuperStep(superStep);
        worker.compute();
        sendOutgoingMessage(worker.getMesgListMap());
//        List<Vertex> updatedData = worker.getVertexList();
        boolean finish = true;
        for (Vertex vertex : worker.getVertexMap().values()) {
            if (vertex.isActive()) {
                finish = false;
                break;
            }
        }
        Request request = new Request("returnSuperStep" + " " + worker.getSuperStep(), new Boolean(finish));

        sendTCPMessage(request, masterIP);
        Utils.println("Function workerUpdateSuperStep: Send return of SuperStep");

    }

    public static void updateIncomingMessage(Map<Integer, List<Message>> messageMap) {
        worker.loadInComingMessage(messageMap);
    }

    public static void sendOutgoingMessage(Map<Integer, Map<Integer, List<Message>>> mesgListMap) {
        Utils.println("Start sending outgoing messages");
        List<Thread> threads = new LinkedList<>();
        for (int id : mesgListMap.keySet()) {
            Thread thread = sendOutgoingMessageToWorker(id, mesgListMap.get(id));
            thread.start();
            threads.add(thread);
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static Thread sendOutgoingMessageToWorker(int id, Map<Integer, List<Message>> data) {
        return new Thread(() -> {
            String workerIP = Utils.intToIP(id);
            Request request = new Request("workerOutgoingMessage", data);
            sendTCPMessage(request, workerIP);
            Utils.println("Send outgoing message to worker " + workerIP + ". Message vertexes size: " + data.keySet().size());
        });
    }

    public static void startSavaTask(String cmd) {
        String application = cmd.split(" ")[1];
        String filename = cmd.split(" ")[2];
        System.out.println("Launching " + application);
//                String graphFile = SDFSServer.fileManager.getFileForGraph(filename);
        startGraphTask(application, filename);
    }

    public static void handleFailure(int id) {
        if (myIP.equals(standbyMaster) && Utils.intToIP(id).equals(introduceIP)) {
            // take over
            master.setActive(true);
            doNextSuperStep();
        }
        if (master != null && master.isActive()) {
            // restart task
            for (int workerId : master.getWorkerIDList()) {
                if (workerId == id) {
                    startGraphTask(master.getGraph().getApplicationName(), master.getInputFilename());
                    break;
                }
            }
        }
    }

    public static void main(String args[]) throws InterruptedException, IOException {

        myIP = args[0];
        System.out.println("myIp: " + myIP);
        id = Utils.ipToInt(myIP);

        fh = new FileHandler("log.txt", true);   // true forces append mode
        SimpleFormatter sf = new SimpleFormatter();
        fh.setFormatter(sf);
        log.addHandler(fh);
        log.setUseParentHandlers(false);


        MP4Server serverThread = new MP4Server();
        serverThread.start();
        // Reading from System.in
        Scanner reader = new Scanner(System.in);

        SDFSServer.init();

        while (true) {
            System.out.println("Enter a cmd: ");
            // Scans the next token of the input as an string.
            String cmd = reader.nextLine();
            System.out.println((new Date()));
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
            } else if (cmd.equals("lf")) {
                System.out.println(SDFSServer.fileManager.getDescription());
            } else if (cmd.startsWith("put ") && cmd.split(" ").length == 3) {
                String localFile = cmd.split(" ")[1];
                String filename = cmd.split(" ")[2];
                final boolean[] confirm = {true};
                if (SDFSServer.fileManager.toConfirmPut(filename)) {
                    System.out.print("Last update is within 1 min, are you sure to update? y/n: ");
                    if (reader.hasNextLine()) {
                        String input = reader.nextLine();
                        if (!input.startsWith("y")) {
                            System.out.println("Rejected by user.");
                            confirm[0] = false;
                        } else {
                            confirm[0] = true;
                        }
                    }
                    new Thread(() -> {
                        try {
                            Thread.sleep(30000);
                            if (!confirm[0]) {
                                Utils.println("\nAutomatically rejected after 30s");
                                confirm[0] = false;
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                if (confirm[0]) {
                    Utils.println("Putting " + localFile + " to " + filename);
                    SDFSServer.fileManager.putFile(localFile, filename);
                }
            } else if (cmd.startsWith("get ") && cmd.split(" ").length == 3) {
                String localFile = cmd.split(" ")[2];
                String filename = cmd.split(" ")[1];
                SDFSServer.fileManager.getFile(localFile, filename);
            } else if (cmd.startsWith("delete ") && cmd.split(" ").length == 2) {
                String filename = cmd.split(" ")[1];
                SDFSServer.fileManager.deleteFile(filename);
            } else if (cmd.startsWith("ls ") && cmd.split(" ").length == 2) {
                String filename = cmd.split(" ")[1];
                ArrayList<Integer> arrayList = SDFSServer.fileManager.listFile(filename);
                StringBuilder result = new StringBuilder("Following hosts store file " + filename + ": \n");
                if (arrayList.size() > 0) {
                    for (SDFSFile file : SDFSServer.fileManager.fileLists.get(arrayList.get(0))) {
                        if (file.getFilename().equals(filename)) {
                            result.append("File size in bytes: ").append(file.getFileSize()).append("\n");
                            break;
                        }
                    }
                    for (int key : arrayList) {
                        result.append("host ").append(key).append(" (").append(Utils.intToIP(key)).append(")\n");
                    }
                    System.out.println(result.toString());
                } else {
                    System.out.println("File not found");
                }
            } else if (cmd.equals("store")) {
                System.out.println(SDFSServer.fileManager.getSelfDescription());
            } else if (cmd.startsWith("sava") && cmd.split(" ").length == 3) {
                sendTCPMessage(new Request(cmd, null), introduceIP);
            }

        }
    }
}
