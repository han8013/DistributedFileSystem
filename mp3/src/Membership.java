import entity.Member;
import entity.Request;

import java.io.IOException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Membership {
    public TreeMap<Integer, Member> membershipList = new TreeMap<>();
    public TreeMap<Integer, Integer> previousState = new TreeMap<>();
    public ArrayList<Integer> leavedNodes = new ArrayList<>();

    private static final Logger log = Logger.getLogger(Membership.class.getName());

    private static final int MEMBERSHIP_SPREAD_INTERVAL = 4000;
    private static final int HEARTBEAT_INTERVAL = 500;
    private static final int DETECTION_INTERVAL = 2000;
    private static final int CLEANUP_INTERVAL = 3000;
    private static int TTL = 2;
    private static int HEARTBEAT_TARGETS = 5;
    private static int MAX_NODES = 1000;

    public Membership() {
        FileHandler fh = MP3Server.fh;
        fh.setFormatter(new SimpleFormatter());
        log.setUseParentHandlers(false);
        log.addHandler(fh);
    }

    public void startMembershipSpread() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(MEMBERSHIP_SPREAD_INTERVAL);
                    //gossip membership
                    if (MP3Server.flag) {
                        sendMessage(new Request("update", membershipList, 0), MP3Server.introduceIP);
                        gossipSend(new Request("update", membershipList, TTL));
                    }
                } catch (InterruptedException e) {
                    log.info("start membership spread thread error");
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void startHeartbeat() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);
                    //heartbeats
                    if (MP3Server.flag) {
                        Member ownMember = membershipList.get(MP3Server.id);
                        ArrayList<Integer> targets = getHeartbeatTargets();
                        for (Integer target : targets) {
                            log.info("Heartbeat from " + MP3Server.id + " to " + target);
                            sendMessage(new Request("heartbeat", ownMember, 0), membershipList.get(target).getIp());
                        }
                    }
                } catch (InterruptedException e) {
                    log.info("start heartbeat thread error");
//                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void startDetection() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(DETECTION_INTERVAL);
                    if (MP3Server.flag) {
                        log.info("failure detector running");
                        boolean hasFailure = false;
                        ArrayList<Integer> senders = getHeartbeatSenders();
                        for (Integer sender : senders) {
                            Member member = membershipList.get(sender);
                            if (member != null) {
                                if (isFailure(member)) {
                                    log.info("found failure: " + member.getId());
                                    member.setAlive(false);
                                    hasFailure = true;
                                    cleanup(member.getId());
                                }
                            }
                        }

                        if (hasFailure) {
                            gossipSend(new Request("update", membershipList, TTL));
                            log.info("send failure message");
                            log.info("now membership list " + membershipList.toString());
                        }

                        // reload previous state
                        previousState = new TreeMap<>();
                        for (Integer sender : senders) {
                            Member member = membershipList.get(sender);
                            if (member != null) {
                                previousState.put(sender, member.getCount());
                            }
                        }

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void cleanup(Integer id) {
        new Thread(() -> {
            try {
                Thread.sleep(CLEANUP_INTERVAL);
                if (membershipList.get(id) != null && !membershipList.get(id).isAlive()) {
                    membershipList.remove(id);
                    SDFSServer.fileManager.updateFileList(membershipList);
                    log.info("remove failure node: " + id);
                }
            } catch (InterruptedException e) {
                log.info("start failure detection thread error");
                e.printStackTrace();
            }
        }).start();
    }

//    private void setFailure(Member member) {
//        if (member.getId() != MP2Server.id) {
//            member.setAlive(false);
//            membershipList.put(member.getId(), member);
//        }
//    }

    private boolean isFailure(Member member) {
        Integer previous = previousState.get(member.getId());
        if (previous == null) {
            return false;
        } else {
            if (member.getCount() <= previous) {
                log.info("Failure found for member " + member.getId() + ", previous:" + previous + ", current:" + member.getCount());
                return true;
            }
            return false;
        }
    }

    private ArrayList<Integer> getHeartbeatSenders() {
        int size = membershipList.size();
        if (size < 2) {
            return new ArrayList<>();
        }
        ArrayList<Member> list = copyMembershipList(membershipList);
        int targetCount = Math.min(list.size() - 1, HEARTBEAT_TARGETS);
        int[] array = new int[list.size() - 1];
        int index = 0;
        for (Member member : list) {
            if (index >= array.length) {
                break;
            }
            if (member.getId() > MP3Server.id) {
                array[index] = member.getId() - MAX_NODES;
                index++;
            } else if (member.getId() < MP3Server.id) {
                array[index] = member.getId();
                index++;
            }
        }
        Arrays.sort(array);
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < targetCount; i++) {
            if (array[array.length - i - 1] < 0) {
                result.add(array[array.length - i - 1] + MAX_NODES);
            } else {
                result.add(array[array.length - i - 1]);
            }
        }
        log.info("Listening heartbeat from " + result.toString());
        return result;
    }

    private ArrayList<Integer> getHeartbeatTargets() {
        int size = membershipList.size();
        if (size < 2) {
            return new ArrayList<>();
        }
        ArrayList<Member> list = copyMembershipList(membershipList);
        int targetCount = Math.min(list.size() - 1, HEARTBEAT_TARGETS);
        int[] array = new int[list.size() - 1];
        int index = 0;
        for (Member member : list) {
            if (index >= array.length) {
                break;
            }
            if (member.getId() < MP3Server.id) {
                array[index] = member.getId() + MAX_NODES;
                index++;
            } else if (member.getId() > MP3Server.id) {
                array[index] = member.getId();
                index++;
            }
        }
        Arrays.sort(array);
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < targetCount; i++) {
            if (array[i] > MAX_NODES) {
                result.add(array[i] - MAX_NODES);
            } else {
                result.add(array[i]);
            }
        }
        return result;
    }

    public void responseJoin(Request request) {
        Member member = (Member) request.getData();
        if (!membershipList.containsKey(member.getId())) {
            member.setLatestUpdateTime(new Date());
            membershipList.put(member.getId(), member);
            SDFSServer.fileManager.updateFileList(membershipList);
//            if (removeList.containsKey(member.getId()))
//                removeList.remove(member.getId());
            log.info("response join node: " + member.getId());
            Request newRequest = new Request("update", membershipList, TTL);
            gossipSend(newRequest);
        }
    }

    public void responseLeave(Request request) {
        Member member = (Member) request.getData();
        if (membershipList.containsKey(member.getId())) {
            membershipList.remove(member.getId());
            SDFSServer.fileManager.updateFileList(membershipList);
        }
        leaveNode(member.getId());
        log.info("response leave node: " + member.getId());
        gossipSend(request);
    }

    private void leaveNode(Integer id) {
        leavedNodes.add(id);
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                leavedNodes.remove(id);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void updateHeartbeat(Member member) {
        Member needUpdate = membershipList.get(member.getId());
        if (needUpdate != null) {
            needUpdate.setCount(needUpdate.getCount() + 1);

            // update if the member back to alive
            if (!needUpdate.isAlive()) {
                needUpdate.setAlive(true);
                Request newRequest = new Request("update", membershipList, TTL);
                gossipSend(newRequest);
            }
            membershipList.put(needUpdate.getId(), needUpdate);
            log.info("update heartbeat count: " + needUpdate.getCount() + "id: " + needUpdate.getId());
        }
    }

    public void updateMembershipList(TreeMap<Integer, Member> incoming) {
        int size = membershipList.size();
        for (int i = 1; i <= 10; i++) {
            Member incomingMember = incoming.get(i);
            Member originalMember = membershipList.get(i);
            if (incomingMember != null) {
                if (originalMember == null) {
                    if (!leavedNodes.contains(incomingMember.getId())) {
                        if (incomingMember.isAlive()) {
                            incomingMember.setLatestUpdateTime(new Date());
                            membershipList.put(i, incomingMember);
                        }
                    }
                } else {
                    if (incomingMember.getCount() > originalMember.getCount()) {
                        incomingMember.setLatestUpdateTime(new Date());
                        membershipList.put(i, incomingMember);
                        if (!membershipList.get(i).isAlive()) {
                            cleanup(i);
                        }
                    } else if (incomingMember.getCount() == originalMember.getCount() && !incomingMember.isAlive()) {
                        incomingMember.setLatestUpdateTime(new Date());
                        membershipList.put(i, incomingMember);
                        if (!membershipList.get(i).isAlive()) {
                            cleanup(i);
                        }
                    }
                }
            }
        }
        SDFSServer.fileManager.updateFileList(membershipList);
        if (size == 1 && incoming.size() > 2 && MP3Server.myIP.equals(MP3Server.introduceIP)) {
            gossipSend(new Request("update", membershipList, TTL));
        }
    }

    public void leaveCluster() {
        Request request = new Request("leave", membershipList.get(MP3Server.id), TTL);
        gossipSend(request);
        membershipList.clear();
        SDFSServer.fileManager.updateFileList(membershipList);
    }

    private ArrayList<Member> copyMembershipList(TreeMap<Integer, Member> membershipList) {
        ArrayList<Member> list = new ArrayList<Member>();
        for (Integer key : membershipList.keySet()) {
            list.add(membershipList.get(key));
        }
        return list;
    }

    public void gossipSend(Request request) {
        //random select k node to send with TTL
        request.setTTL(request.getTTL() - 1);
        if (request.getTTL() < 0) {
            return;
        }
        ArrayList<Member> list = copyMembershipList(membershipList);
        list.remove(membershipList.get(MP3Server.id));
        if (list.size() >= 1) {
            log.info("gossipSend method run.");
            for (int i = 0; i < 4 && list.size() > 0; i++) {
                int index = (int) (Math.random() * list.size());
                log.info("gossip send from" + MP3Server.id + "to" + list.get(index).getId() + "action: " + request.getAction());
                sendMessage(request, list.get(index).getIp());
                list.remove(index);
            }
        }
    }

    public void sendMessage(Request request, String ip) {
        try {
            Thread send = new Thread(new SendThread(Utils.objectToByteArray(request), ip));
            send.start();
        } catch (IOException e) {
            System.out.println("send failed");
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Membership List:\n");
        for (Integer key : membershipList.keySet()) {
            s.append("id:");
            s.append(key);
            s.append(";");
            s.append(membershipList.get(key).toString());
        }
        return s.toString();
    }


}
