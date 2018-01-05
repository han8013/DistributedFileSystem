import entity.Member;
import entity.Request;
import entity.Response;
import entity.SDFSFile;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class FileManager {

    public Hashtable<Integer, ArrayList<SDFSFile>> fileLists = new Hashtable<>();
    private static final Logger log = Logger.getLogger(Membership.class.getName());

    public FileManager() {
        FileHandler fh = MP4Server.fh;
        fh.setFormatter(new SimpleFormatter());
        log.setUseParentHandlers(false);
        log.addHandler(fh);
    }

//    public String getFileForGraph(String filename) {
//        for (SDFSFile file : fileLists.get(Utils.ipToInt(MP4Server.myIP))) {
//            if (file.getFilename().equals(filename)) {
//                return new String("files/" + file.getSHA256());
//            }
//        }
//        return null;
//    }

    public void storeFile(SDFSFile file) throws IOException {
        FileOutputStream fos = new FileOutputStream("files/" + file.getSHA256());
        fos.write(file.getFile());
        fos.close();
    }

    public byte[] readFile(String filename) {
        for (SDFSFile file : fileLists.get(Utils.ipToInt(MP4Server.myIP))) {
            if (file.getFilename().equals(filename)) {
                try {
                    return Files.readAllBytes(Paths.get("files/" + file.getSHA256()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void updateFileList(TreeMap<Integer, Member> membershipList) {
        boolean toBackup = false;
        for (Integer key : membershipList.keySet()) {
            if (fileLists.get(key) == null) {
                fileLists.put(key, new ArrayList<>());
                // send my file list to the newly joined process.
                int myKey = Utils.ipToInt(MP4Server.myIP);
                if (key != myKey) {
                    if (fileLists.containsKey(myKey) && fileLists.get(myKey).size() > 0) {
                        sendFileList(key);
                    }
                }
                toBackup = true;
                Utils.println("+ Add process" + key + " to file list");
                log.info("Add process" + key + " to file list");
            }
        }
        ArrayList<Integer> removeList = new ArrayList<>();
        for (Integer key : fileLists.keySet()) {
            if (membershipList.get(key) == null) {
                removeList.add(key);
                toBackup = true;
                Utils.println("- Delete process" + key + " from file list");
                log.info("Delete process" + key + " from file list");
                MP4Server.handleFailure(key);
            }
        }
        for (Integer key : removeList) {
            fileLists.remove(key);
        }
        if (toBackup) {
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    backup();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void backup() {
        Set<String> filenames = new HashSet<>();
        for (int key : fileLists.keySet()) {
            for (SDFSFile file : fileLists.get(key)) {
                filenames.add(file.getFilename());
            }
        }
        for (String filename : filenames) {
            ArrayList<Integer> keys = listFile(filename);
            if (keys.size() < 3 && keys.size() > 0) {
                int minKey = Integer.MAX_VALUE;
                for (int key : keys) {
                    if (key < minKey) {
                        minKey = key;
                    }
                }
                if (minKey == Utils.ipToInt(MP4Server.myIP)) {
                    Utils.println("Start backup file " + filename);
                    int toBackup = 3 - keys.size();
                    ArrayList<Integer> restHosts = new ArrayList<>();
                    for (int key : fileLists.keySet()) {
                        if (!keys.contains(key)) {
                            restHosts.add(key);
                        }
                    }
                    byte[] bytes = readFile(filename);
                    SDFSFile file = null;
                    for (SDFSFile temp : fileLists.get(Utils.ipToInt(MP4Server.myIP))) {
                        if (temp.getFilename().equals(filename)) {
                            file = new SDFSFile(temp, bytes);
                        }
                    }
                    if (file != null) {
                        for (String ip : randomPick(restHosts, toBackup)) {
                            Response response = sendPackage(ip, new Request("put", file));
                            if (response == null || !response.getAction().equals("ok")) {
                                Utils.println("Backup " + filename + "failed at host " + Utils.ipToInt(ip) + " - " + ip);
                            } else {
                                Utils.println("File " + filename + " successfully backup at host " + Utils.ipToInt(ip) + " - " + ip);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean toConfirmPut(String filename) {
        long latest = 0;
        for (int key : fileLists.keySet()) {
            for (SDFSFile file : fileLists.get(key)) {
                if (file.getFilename().equals(filename) && latest < file.getLastModificationTime()) {
                    latest = file.getLastModificationTime();
                }
            }
        }
        return System.currentTimeMillis() - latest < 60 * 1000;
    }

    public void putFile(String localFile, String filename) {
        int timestamp = 0;
        a: for (ArrayList<SDFSFile> list : fileLists.values()) {
            for (SDFSFile file : list) {
                if (file.getFilename().equals(filename)) {
                    Utils.println("The file " + filename + " already exists and will be updated.");
                    timestamp = file.getTimestamp() + 1;
                    break a;
                }
            }
        }
        SDFSFile file;
        try {
            file = new SDFSFile(filename, localFile, timestamp);
        } catch (Exception e) {
            System.out.println("XXX File " + localFile + " not found XXX");
            return;
        }
        if (timestamp > 0) {
            ArrayList<Integer> hosts = listFile(filename);
            for (int key : hosts) {
                Response response = sendPackage(Utils.intToIP(key), new Request("put", file));
                if (response == null || !response.getAction().equals("ok")) {
                    Utils.println("Storing " + filename + "failed at host " + key + " - " + Utils.intToIP(key));
                } else {
                    Utils.println("File " + filename + " successfully stored at host " + key + " - " + Utils.intToIP(key));
                }
            }
        } else {
            for (String ip : randomPick(fileLists.keySet(), 3)) {
                Response response = sendPackage(ip, new Request("put", file));
                if (response == null || !response.getAction().equals("ok")) {
                    Utils.println("Storing " + filename + "failed at host " + Utils.ipToInt(ip) + " - " + ip);
                } else {
                    Utils.println("File " + filename + " successfully stored at host " + Utils.ipToInt(ip) + " - " + ip);
                }
            }
        }
    }

    public void getFile(String localFile, String filename) {
        ArrayList<Integer> hosts = listFile(filename);
        if (hosts.size() == 0) {
            Utils.println("File not found");
        } else {
            int key = hosts.get((int) (Math.random() * hosts.size()));
            Utils.println("Fetching file from " + Utils.intToIP(key));
            Response response = sendPackage(Utils.intToIP(key), new Request("get", filename));
            if (response != null) {
                byte[] data = (byte[]) response.getData();
                try {
                    FileOutputStream fos = new FileOutputStream(localFile);
                    fos.write(data);
                    fos.close();
                    Utils.println("File " + filename + " is fetched successfully.");
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Utils.println("Failed to fetch file.");
            }
        }
    }

    public void deleteFile(String filename) {
        ArrayList<Integer> arrayList = listFile(filename);
        for (int key : arrayList) {
            String ip = Utils.intToIP(key);
            Response response = sendPackage(ip, new Request("delete", filename));
            if (response == null) {
                Utils.println("Deleting " + filename + " failed at " + ip);
            } else {
                if (response.getAction().equals("ok")) {
                    Utils.println("File " + filename + " successfully deleted at " + ip);
                } else {
                    Utils.println("File not found at " + ip);
                }
            }
        }
    }

    public Response sendPackage(String ip, Request request) {
        Utils.println("                                       Sending '" + request.getAction() + "' package to " + ip);
        try {
            Socket socket = new Socket(ip, SDFSServer.tcpPort);
            socket.getOutputStream().write(Utils.objectToByteArray(request));
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Response response = (Response) ois.readObject();
            socket.close();
            return response;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void spreadPackage(Request request) {
        for (int key : fileLists.keySet()) {
            new Thread(() -> {
                sendPackage(Utils.intToIP(key), request);
            }).start();
        }
    }

    private List<String> randomPick(Collection<Integer> list, int count) {
        ArrayList<Integer> arrayList = new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();
        arrayList.addAll(list);
        while (count > 0 && arrayList.size() > 0) {
            int random = (int)(Math.random() * arrayList.size());
            result.add(Utils.intToIP(arrayList.get(random)));
            arrayList.remove(random);
            count--;
        }
        return result;
    }

    public String getDescription() {
        StringBuilder result = new StringBuilder("File list:\n");
        for(int key : fileLists.keySet()) {
            if (Utils.ipToInt(MP4Server.myIP) == key) {
                result.append("[self]");
            } else {
                result.append("      ");
            }
            result.append(key).append(": ");
            for (SDFSFile file : fileLists.get(key)) {
                result.append(file.getFilename()).append(", ");
            }
            result.append("\n");
        }
        return result.toString();
    }

    public String getSelfDescription() {
        StringBuilder result = new StringBuilder("File list: ");
        for (SDFSFile file : fileLists.get(Utils.ipToInt(MP4Server.myIP))) {
            result.append(file.getFilename()).append(", ");
        }
        return result.toString();
    }

    public ArrayList<Integer> listFile(String filename) {
        ArrayList<Integer> result = new ArrayList<>();
        for (int key : fileLists.keySet()) {
            for (SDFSFile file : fileLists.get(key)) {
                if (file.getFilename().equals(filename)) {
                    result.add(key);
                    break;
                }
            }
        }
        return result;
    }

    public void sendFileList(int key) {
        sendPackage(Utils.intToIP(key), new Request("fileList", fileLists.get(Utils.ipToInt(MP4Server.myIP))));
    }
}
