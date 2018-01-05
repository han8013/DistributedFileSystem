package old;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MembershipManagement implements Serializable {

    public static TreeMap<Integer, Member> membershipList = new TreeMap<Integer, Member>();
    // stupid way ,need to clear after long time
    public static TreeMap<Integer, Member> removeList = new TreeMap<Integer, Member>();
    public static TreeMap<Integer, Integer> previousState = new TreeMap<Integer, Integer>();
    public static int size = membershipList.size();
    private static final Logger log = Logger.getLogger(MembershipManagement.class.getName());
    //    FileHandler fh = new FileHandler("log.txt", true);   // true forces append mode
    SimpleFormatter sf = new SimpleFormatter();

//    public MembershipManagement() throws IOException {
//        FileHandler fh = UDPSever.fh;
//        fh.setFormatter(sf);
//        log.setUseParentHandlers(false);
//        log.addHandler(fh);
//    }


//    public void startMembershipSpread() {
//
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(4000);
//                    //goosip membership
//                    gossipSend(new Request("update", membershipList, 3));
//
//                } catch (InterruptedException e) {
//                    log.info("start membership spread thread error");
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

//    public void startHeartbeat() {
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(500);
//                    //heartbeats
//                    Member ownMember = membershipList.get(UDPSever.id);
//                    sendHeartbeat(new Request("heartbeat", ownMember, 0));
//
//                } catch (InterruptedException e) {
//                    log.info("start heartbeat thread error");
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }

//    public void startDetection() {
//        new Thread(() -> {
//            while (true) {
//                try {
//                    Thread.sleep(3000);
//
//                    log.info("failure detector running");
//                    //heartbeats count check
//                    boolean hasFailure = false;
//
//                    ArrayList<Integer> heartbeatSender = getHeartbeatsender();
////                        System.out.println(heartbeatSender.size());
//                    for (int i = 0; i < heartbeatSender.size(); i++) {
//                        Member member = membershipList.get(heartbeatSender.get(i));
//                        if (isFailure(member)) {
//                            log.info("found failure: " + member.getId());
//                            setFailure(member);
//                            hasFailure = true;
//                            new Thread(() -> {
//                                try {
//                                    Thread.sleep(10000);
//                                    if (membershipList.get(member.getId()) != null && !membershipList.get(member.getId()).isAlive()) {
//                                        membershipList.remove(member.getId());
//                                        removeList.put(member.getId(), member);
//                                        log.info("remove failure node: " + member.getId());
//
//                                    }
//
//                                } catch (InterruptedException e) {
//                                    log.info("start failure detection thread error");
//                                    e.printStackTrace();
//                                }
//                            }).start();
//
//                        }
//                    }
//                    //if failure happen, membership updates and send
//                    if (hasFailure) {
//                        gossipSend(new Request("update failure", membershipList, 3));
//                        log.info("send failure message");
//                        log.info("now membership list " + membershipList.toString());
//                    }
//                    //update previous heartbeat sender count
//                    updatePreviousState();
//
//
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
//    }


//    public boolean isFailure(Member member) {
//        Integer previous = previousState.get(member.getId());
//        if (previous == null) {
//            return false;
//        } else {
//            if (member.getCount() - previous > 0)
//                return false;
//            else
//                return true;
//        }
//    }

//    public void setFailure(Member member) {
//        if (member.getId() != UDPSever.id) {
//            member.setAlive(false);
//            membershipList.put(member.getId(), member);
//        }
//    }

//    public void updateHeartbeat(Member member) {
//        Member needUpdate = membershipList.get(member.getId());
//        if (needUpdate != null) {
//            needUpdate.setCount(member.getCount() + 1);
//            needUpdate.setAlive(true);
//            membershipList.put(needUpdate.getId(), needUpdate);
//            log.info("update heartbeat count: " + needUpdate.getCount() + "id: " + needUpdate.getId());
//        }
//    }

//    public void updatePreviousState() {
//        TreeMap<Integer, Integer> newPreviousState = new TreeMap<Integer, Integer>();
//
//        ArrayList<Integer> heartbeatSender = getHeartbeatsender();
//        for (int i = 0; i < heartbeatSender.size(); i++) {
//            Member member = membershipList.get(heartbeatSender.get(i));
//            newPreviousState.put(member.getId(), member.getCount());
//        }
//        previousState = newPreviousState;
//        log.info("failure update previous heartbeat count record: " + previousState.toString());
//
//    }

//    public void gossipSend(Request request) {
//        //random select k node to send with TTL
//        if (request.getTTL() <= 0) {
//            return;
//        }
//        ArrayList<Member> list = getMemberList(membershipList);
//        if (list.size() >= 2) {
//            log.info("gossipSend method run.");
//            for (int i = 0; i < 4; i++) {
//
//            }
//            int k = 4;
//            while (k > 0) {
//                Random rand = new Random();
//                int n = rand.nextInt(list.size());
//                if (list.get(n).getId() != UDPSever.id) {
//                    log.info("gossip send from" + UDPSever.id + "to" + list.get(n).getId() + "action: " + request.getAction());
//                    sendMessage(request, list.get(n).getIp());
//                    k--;
//                }
//            }
//        }
//
//    }

//    public void sendHeartbeat(Request request) {
//        ArrayList<Member> list = getMemberList(membershipList);
//        if (list.size() >= 2) {
//            log.info("sendheartbeat method run.");
//
//            ArrayList<Integer> targets = getHeartbeatTargets();
//            log.info("heartbeats target number:" + targets.size());
//            log.info("heartbeats target are:" + targets.toString());
//            for (int i = 0; i < targets.size(); i++) {
//                if (UDPSever.id != targets.get(i) && membershipList.get(targets.get(i)) != null) {
//                    log.info("send heartbeat from " + UDPSever.id + " to " + targets.get(i));
//                    sendMessage(request, membershipList.get(targets.get(i)).getIp());
//                }
//            }
//            // select previous two and next three from virtual ring
//
//        }
//
//    }

//    public ArrayList<Integer> getHeartbeatsender() {
//        if (getMemberList(membershipList).size() > 1) {
//            ArrayList<Member> list = getMemberList(membershipList);
//            int indexOfOwn = list.indexOf(membershipList.get(UDPSever.id));
//            ArrayList<Integer> senders = new ArrayList<Integer>();
////            senders.add (list.get(Math.max(0, indexOfOwn - 3)).getId());
////            senders.add (list.get(Math.max(0, indexOfOwn - 2)).getId());
////            senders.add (list.get(Math.max(0, indexOfOwn - 1)).getId());
////            senders.add (list.get(Math.min(indexOfOwn + 1, list.size() - 1)).getId());
////            senders.add (list.get(Math.min(indexOfOwn + 2, list.size() - 1)).getId());
//
//
//            Collections.reverse(list);
//            int count = 1;
//            while (count < 6) {
//                int heartbeatTargetIndex = indexOfOwn + count;
//                if (heartbeatTargetIndex < list.size())
//                    senders.add(list.get(heartbeatTargetIndex).getId());
//                else {
//                    while (heartbeatTargetIndex >= list.size()) {
//                        heartbeatTargetIndex = heartbeatTargetIndex % list.size();
//                    }
//                    senders.add(list.get(heartbeatTargetIndex).getId());
//                }
//                count++;
//            }
//            ArrayList<Integer> result = new ArrayList<Integer>();
//
//            for (Integer send : senders) {
//                if (send != UDPSever.id) {
//                    result.add(send);
//                }
//            }
//            //remove duplicate
//            result = new ArrayList<Integer>(new LinkedHashSet<Integer>(result));
//            return result;
//        } else
//            return new ArrayList<Integer>();
//
//    }
//
////    public ArrayList<Integer> getHeartbeatTargets() {
////        if (getMemberList(membershipList).size() > 1) {
////            log.info("gettargets");
////
////            ArrayList<Member> list = getMemberList(membershipList);
////            int indexOfOwn = list.indexOf(membershipList.get(UDPSever.id));
////            ArrayList<Integer> targets = new ArrayList<Integer>();
////            int count = 1;
////            while (count < 6) {
////                int heartbeatTargetIndex = indexOfOwn + count;
////                if (heartbeatTargetIndex < list.size())
////                    targets.add(list.get(heartbeatTargetIndex).getId());
////                else {
////                    while (heartbeatTargetIndex >= list.size()) {
////                        heartbeatTargetIndex = heartbeatTargetIndex % list.size();
////                    }
////                    targets.add(list.get(heartbeatTargetIndex).getId());
////                }
////                count++;
////
////            }
//////            targets.add (list.get(Math.max(0, indexOfOwn - 2)).getId());
//////            targets.add (list.get(Math.max(0, indexOfOwn - 1)).getId());
//////            targets.add (list.get(Math.min(indexOfOwn + 1, list.size() - 1)).getId());
//////            targets.add (list.get(Math.min(indexOfOwn + 2, list.size() - 1)).getId());
//////            targets.add (list.get(Math.min(indexOfOwn + 3, list.size() - 1)).getId());
////            ArrayList<Integer> result = new ArrayList<Integer>();
////
////            for (Integer send : targets) {
////                log.info("targetsNode" + send);
////                if (send != UDPSever.id) {
////                    result.add(send);
////                }
////            }
////            //remove duplicate
////            result = new ArrayList<Integer>(new LinkedHashSet<Integer>(result));
////            return result;
////        } else
////            return new ArrayList<Integer>();
////
////    }

//    public void leaveCluster() {
//        Request request = new Request("leave", membershipList.get(UDPSever.id), 3);
//        gossipSend(request);
//        membershipList.clear();
//    }

//    public void responseJoin(Request request) {
//        Member member = (Member) request.getData();
//        if (!membershipList.containsKey(member.getId())) {
//            member.setLatestUpdateTime(new Date());
//            membershipList.put(member.getId(), member);
//            if (removeList.containsKey(member.getId()))
//                removeList.remove(member.getId());
//        }
//        log.info("response join node: " + member.getId());
//        if (request.getTTL() > 0) {
//            Request newRequest = new Request("update", request.getData(), request.getTTL() - 1);
//            gossipSend(newRequest);
//        }
//    }
//
//    public void responseLeave(Request request) {
//        Member member = (Member) request.getData();
//        if (membershipList.containsKey(member.getId()))
//            membershipList.remove(member.getId());
//        log.info("response leave node: " + member.getId());
//        if (request.getTTL() > 0) {
//            Request newRequest = new Request("leave", request.getData(), request.getTTL() - 1);
//            gossipSend(newRequest);
//        }
//    }

    public void updateMembershipList(TreeMap<Integer, Member> incoming) {
        // merge two list, select max count entry
        ArrayList<Member> original = getMemberList(membershipList);
        ArrayList<Member> changes = getMemberList(incoming);
        for (int i = 1; i <= 10; i++) {
            Member memberFromOriginal = membershipList.get(i);
            Member memberFromChanges = incoming.get(i);
            if (memberFromOriginal == null && memberFromChanges != null) {
                if (!removeList.containsKey(memberFromChanges.getId()))
                    membershipList.put(i, memberFromChanges);
            } else if (memberFromOriginal != null && memberFromChanges != null) {
                if (memberFromChanges.getCount() > memberFromOriginal.getCount()) {
                    memberFromOriginal.setCount(memberFromChanges.getCount());
                    if (memberFromChanges.getId() != UDPSever.id)
                        memberFromOriginal.setAlive(memberFromChanges.isAlive());
                    membershipList.put(i, memberFromOriginal);
                }
            }
        }
        log.info("membership list update: " + membershipList.toString());
    }


//    public ArrayList<Member> getMemberList(TreeMap<Integer, Member> membershipList) {
//        ArrayList<Member> list = new ArrayList<Member>();
//        for (Integer key : membershipList.keySet()) {
//            list.add(0, membershipList.get(key));
//        }
//        return list;
//    }


//    public void sendMessage(Request request, String ip) {
//        try {
//            Thread send = new Thread(new SendThread(Utils.objectToByteArray(request), ip));
//            send.start();
//        } catch (IOException e) {
//            //todo: log
//
//            System.out.println("send failed");
//            e.printStackTrace();
//        }
//    }


//    @Override
//    public String toString() {
//        StringBuilder s = new StringBuilder("Membership List:\n");
//        for (Integer key : membershipList.keySet()) {
//            s.append("id:");
//            s.append(key);
//            s.append(";");
//            s.append(membershipList.get(key).toString());
//        }
//        return s.toString();
//    }
}
