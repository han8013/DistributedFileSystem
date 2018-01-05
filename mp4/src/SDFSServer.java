import entity.Request;
import entity.Response;
import entity.SDFSFile;
import graph.Graph;
import graph.Master;
import graph.Message;
import graph.Vertex;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class SDFSServer {

    public static final int tcpPort = 8001;
    public static final FileManager fileManager = new FileManager();
    private static final Logger log = Logger.getLogger(SDFSServer.class.getName());

    public static void init() throws IOException {
        FileHandler fh = new FileHandler("sdfs.txt", true);   // true forces append mode
        fh.setFormatter(new SimpleFormatter());
        log.addHandler(fh);
        log.setUseParentHandlers(false);

        new Thread(() -> {
            try {
                ServerSocket mainSocket = new ServerSocket(tcpPort);
                log.info("SDFS server set up");
                while (true) {
                    Socket socket = mainSocket.accept();
                    handleRequest(socket);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleRequest(Socket socket) {
        new Thread(() -> {
            try {
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                Request request = (Request) ois.readObject();
                System.out.println("                                              Receiving '" + request.getAction() + "' package from " + socket.getInetAddress().getHostAddress());
                // Put a file
                if (request.getAction().equals("put")) {
                    // get file
                    SDFSFile file = (SDFSFile) request.getData();
                    // store file
                    fileManager.storeFile(file);

                    // response
                    defaultRespond(socket);

                    log.info("Put request: stored " + file.getFilename() + " to " + file.getSHA256());
                    // add to list
                    ArrayList<SDFSFile> list = fileManager.fileLists.get(Utils.ipToInt(MP4Server.myIP));
                    SDFSFile lightFile = new SDFSFile(file.getFilename(), file.getTimestamp(), file.getSHA256(), file.getFileSize(), file.getLastModificationTime());
                    SDFSFile temp = null;
                    for (SDFSFile f : list) {
                        if (f.getFilename().equals(lightFile.getFilename())) {
                            temp = f;
                            break;
                        }
                    }
                    if (temp != null) {
                        list.remove(temp);
                    }
                    list.add(lightFile);
                    // spread the message
                    fileManager.spreadPackage(new Request("add", lightFile));
                }
                // A file is added to another process, update myself with this message
                else if (request.getAction().equals("add")) {
                    SDFSFile incomingFile = (SDFSFile) request.getData();
                    int key = Utils.ipToInt(socket.getInetAddress().getHostAddress());
                    ArrayList<SDFSFile> list = fileManager.fileLists.get(key);
                    SDFSFile temp = null;
                    if (list != null) {
                        for (SDFSFile file : list) {
                            if (file.getFilename().equals(incomingFile.getFilename())) {
                                temp = file;
                                break;
                            }
                        }
                        if (temp != null) {
                            list.remove(temp);
                        }
                        list.add(incomingFile);
                    }
                    defaultRespond(socket);
                }
                // get other processes's file list when join
                else if (request.getAction().equals("fileList")) {
                    ArrayList<SDFSFile> arrayList = (ArrayList<SDFSFile>) request.getData();
                    if (arrayList == null) {
                        arrayList = new ArrayList<>();
                    }
                    int key = Utils.ipToInt(socket.getInetAddress().getHostAddress());
                    if (!fileManager.fileLists.containsKey(key)) {
                        Utils.println("+ Add process" + key + " to file list");
                    }
                    fileManager.fileLists.put(key, arrayList);
                    defaultRespond(socket);
                }
                // get file
                else if (request.getAction().equals("get")) {
                    String filename = (String) request.getData();
                    byte[] bytes = fileManager.readFile(filename);
                    if (bytes != null) {
                        respond(socket, new Response("ok", bytes));
                    }
                }
                // delete file
                else if (request.getAction().equals("delete")) {
                    String filename = (String) request.getData();
                    SDFSFile temp = null;
                    ArrayList<SDFSFile> list = fileManager.fileLists.get(Utils.ipToInt(MP4Server.myIP));
                    for (SDFSFile file : list) {
                        if (file.getFilename().equals(filename)) {
                            temp = file;
                            break;
                        }
                    }
                    if (temp != null) {
                        list.remove(temp);
                        defaultRespond(socket);
                        fileManager.spreadPackage(new Request("remove", temp));
                    } else {
                        respond(socket, new Response("not found", null));
                    }
                }
                // A file is removed to another process, update myself with this message
                else if (request.getAction().equals("remove")) {
                    SDFSFile incomingFile = (SDFSFile) request.getData();
                    int key = Utils.ipToInt(socket.getInetAddress().getHostAddress());
                    ArrayList<SDFSFile> list = fileManager.fileLists.get(key);
                    SDFSFile temp = null;
                    if (list != null) {
                        for (SDFSFile file : list) {
                            if (file.getFilename().equals(incomingFile.getFilename())) {
                                temp = file;
                                break;
                            }
                        }
                        if (temp != null) {
                            list.remove(temp);
                        }
                    }
                    defaultRespond(socket);
                } else if (request.getAction().startsWith("superStep")) {
                    String[] messages = request.getAction().split(" ");
                    int workerSupterstep = Integer.valueOf(messages[1]);
                    Object raw = request.getData();
                    List<Vertex> data = null;
                    if (raw instanceof List) {
                        data = (List<Vertex>) raw;
                    }
//                    System.out.println("Recv superStep message from master with vertex data");
                    MP4Server.workerUpdateSuperStep(workerSupterstep, data, socket.getInetAddress().getHostAddress());
                    defaultRespond(socket);
                } else if (request.getAction().startsWith("graphMetadata")) {

                    Graph graph = (Graph) request.getData();
//                    System.out.println("Recv graphMetadata from master");
                    MP4Server.workerUpdateMetadata(graph);
                    defaultRespond(socket);
                } else if (request.getAction().startsWith("returnSuperStep")) {
                    String[] messages = request.getAction().split(" ");
                    int workerSupterstep = Integer.valueOf(messages[1]);
                    Boolean finish = (Boolean) request.getData();
//                    List<Vertex> data = (List<Vertex>) request.getData();
//                    System.out.println("Recv returnSuperStep message from worker of previous superStep");
                    MP4Server.masterUpdateSuperStep(Utils.ipToInt(socket.getInetAddress().getHostAddress()), workerSupterstep, finish);
                    defaultRespond(socket);
                } else if (request.getAction().startsWith("workerOutgoingMessage")) {
                    Map<Integer, List<Message>> messageMap = (Map<Integer, List<Message>>) request.getData();
//                    System.out.println("Recv workerOutgoingMessage from other worker");
                    MP4Server.updateIncomingMessage(messageMap);
                    Utils.println("Incoming data loaded from " + socket.getInetAddress().getHostAddress());
                    defaultRespond(socket);
                } else if (request.getAction().equals("collect")) {
                    respond(socket, new Response("collect", MP4Server.collectAllVertices()));
                } else if (request.getAction().startsWith("sava")) {
                    MP4Server.startSavaTask(request.getAction());
                    defaultRespond(socket);
                } else if (request.getAction().equals("masterSync")) {
                    MP4Server.master = (Master) request.getData();
                    defaultRespond(socket);
                }
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void respond(Socket socket, Response response) throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        oos.writeObject(response);
        oos.close();
    }

    private static void defaultRespond(Socket socket) throws IOException {
        respond(socket, new Response("ok", null));
    }

}
