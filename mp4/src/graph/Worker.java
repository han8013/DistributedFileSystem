package graph;
import java.util.*;

public class Worker {
    private int workerID;
    private String IP;
//    private Map<Integer,Integer> vertexIDtoWorkerID;
    private int superStep;
    private Application application;
    private Map<Integer,Vertex> vertexMap;
    private Map<Integer,Map<Integer, List<Message>>> mesgListMap;
    private Graph graph;
    private final Object computingMutex = new Object();

    public Worker(int id, String ip) {
        workerID = id;
        IP = ip;
        vertexMap = new HashMap<>(50000);
        mesgListMap = new HashMap<>();
    }

    public void loadData(List<Vertex> data) {
        for (Vertex vertex : data) {
            vertexMap.put(vertex.getID(), vertex);
        }
    }

    public void loadInComingMessage(Map<Integer, List<Message>> messageMap) {
        synchronized (computingMutex) {
            for (int nodeID : messageMap.keySet()) {
                for (Message message : messageMap.get(nodeID)) {
                    Vertex targetVertex = vertexMap.get(message.getTargetNode());
                    if (targetVertex != null) {
                        targetVertex.getIncomingMesg().add(message);
                    }
                }
            }
        }
    }

    public void compute(){
        synchronized (computingMutex) {
            // using message from incoming queue to update vertex value
            for (Vertex vertex : vertexMap.values()) {
                vertex.setOutgoingMesg(new LinkedList<>());
            }
            for (Vertex vertex : vertexMap.values()) {
                application.compute(superStep, vertex);
            }
//            int maxID = 0;
//            double max = 0;
//            for (Vertex vertex : vertexMap.values()) {
//                if (vertex.getValue() > max) {
//                    max = vertex.getValue();
//                    maxID = vertex.getID();
//                }
//            }
//            System.out.println("==== Largest value: " + max + " from id " + maxID + " ====");
            for (Vertex vertex : vertexMap.values()) {
                vertex.setIncomingMesg(new LinkedList<>());
            }
            // send all outgoing message of each vertex
            mesgListMap = new HashMap<>();
            for (Vertex vertex : vertexMap.values()) {
                processOutgoingMesg(vertex);
            }
        }
    }

    public void processOutgoingMesg(Vertex vertex) {

        List<Integer> workerIDList = graph.getWorkerIDList();
        int workerNumber = workerIDList.size();

        for (Message message : vertex.getOutgoingMesg()) {
            int workerIndex = message.getTargetNode() % workerNumber;
            int targetWorkerID = workerIDList.get(workerIndex);
            if (targetWorkerID == workerID) {
                Vertex targetVertex = vertexMap.get(message.getTargetNode());
                targetVertex.getIncomingMesg().add(message);
            }
            else{
                if (!mesgListMap.containsKey(targetWorkerID)) {
                    mesgListMap.put(targetWorkerID, new HashMap<>(50000));
                }
                Map<Integer, List<Message>> workerMap = mesgListMap.get(targetWorkerID);
                if (!workerMap.containsKey(message.getTargetNode())) {
                    workerMap.put(message.getTargetNode(), new ArrayList<>());
                }
                workerMap.get(message.getTargetNode()).add(message);
            }
        }
    }

    public void initApplication(String applicationName) {
        if (applicationName.equals("PageRank")) {
            this.application = new PageRank();
        } else if (applicationName.equals("ShortestPath")) {
            this.application = new ShortestPath();
        }
    }

    public Map<Integer,Map<Integer, List<Message>>> getMesgListMap() {
        return mesgListMap;
    }

    public void setMesgListMap(Map<Integer,Map<Integer, List<Message>>> mesgListMap) {
        this.mesgListMap = mesgListMap;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Map<Integer, Vertex> getVertexMap() {
        return vertexMap;
    }

    public void setVertexMap(Map<Integer, Vertex> vertexMap) {
        this.vertexMap = vertexMap;
    }

//    public Map<Integer, Integer> getVertexIDtoWorkerID() {
//        return vertexIDtoWorkerID;
//    }
//
//    public void setVertexIDtoWorkerID(Map<Integer, Integer> vertexIDtoWorkerID) {
//        this.vertexIDtoWorkerID = vertexIDtoWorkerID;
//    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public int getWorkerID() {
        return workerID;
    }

    public void setWorkerID(int workerID) {
        this.workerID = workerID;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public int getSuperStep() {
        return superStep;
    }

    public void setSuperStep(int superStep) {
        this.superStep = superStep;
    }
}
