package graph;

import entity.Member;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Master implements Serializable {
    private List<Member> workerList;
    private List<Integer> workerIDList;
    private Map<Integer, List<Vertex>> partitionMap;
    private int superStep;
    private Map<Integer, Vertex> vertexMap;
    private Map<Integer, Integer> workerStepMap;
    private Map<Integer, Boolean> finishMap;
    private Graph graph;
    private Application application;
    private long startTime;
    private boolean active;
    private String inputFilename;

    public Master(int id, String ip) {
        vertexMap = new HashMap<>(250000);
        finishMap = new HashMap<>();
        superStep = 0;
        startTime = System.currentTimeMillis();
        active = true;
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(workerList);
        out.writeObject(workerIDList);
        out.writeInt(superStep);
        out.writeObject(workerStepMap);
        out.writeObject(finishMap);
        out.writeObject(graph);
        out.writeLong(startTime);
        out.writeBoolean(active);
        out.writeObject(inputFilename);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        workerList = (List<Member>) in.readObject();
        workerIDList = (List<Integer>) in.readObject();
        superStep = in.readInt();
        workerStepMap = (Map<Integer, Integer>) in.readObject();
        finishMap = (Map<Integer, Boolean>) in.readObject();
        graph = (Graph) in.readObject();
        startTime = in.readLong();
        active = in.readBoolean();
        inputFilename = (String) in.readObject();

        active = false;
        superStep += 1;
        for (int id : workerStepMap.keySet()) {
            workerStepMap.put(id, workerStepMap.get(id) + 1);
        }
        initApplication(graph.getApplicationName());
    }

    public double getRuntime() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }

    public void printRuntime() {
        System.out.println("Current runtime: " + getRuntime() + "s");
    }

    public void loadData(String fileName) {
        try {
            File file = new File(fileName);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            graph = new Graph();
            int edgeNumber = 0;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] nodes = line.replace("\n", "").split("\t");
                edgeNumber++;

                int fromID = Integer.valueOf(nodes[0].trim());
                int toID = Integer.valueOf(nodes[1].trim());

                if (vertexMap.get(fromID) != null) {
                    Vertex fromNode = vertexMap.get(fromID);
                    fromNode.getOutgoingVertex().add(toID);
                } else {
                    Vertex fromNode = new Vertex(fromID);
                    fromNode.getOutgoingVertex().add(toID);
                    vertexMap.put(fromID, fromNode);
                }

                if (vertexMap.get(toID) == null) {
                    Vertex toNode = new Vertex(toID);
                    vertexMap.put(toID, toNode);
                }

            }
            fileReader.close();
            System.out.println("Finish Vertex Construction!");
            //set Graph property
            graph.setEdgeNumber(edgeNumber);
            graph.setNodeNumber(vertexMap.size());
            graph.setWorkerIDList(workerIDList);

            // call partition function
            partitionVertex();
            workerStepMap = new HashMap<>();
            initWorkerStepMap();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initApplication(String applicationName) {
        if (applicationName.equals("PageRank")) {
            this.application = new PageRank();
        } else if (applicationName.equals("ShortestPath")) {
            this.application = new ShortestPath();
        }
    }

    public void initWorkerStepMap() {
        for (int workerID : workerIDList) {
            workerStepMap.put(workerID, -1);
        }
    }


    public void partitionVertex() {
        int workerNumber = workerIDList.size();
        partitionMap = new HashMap<>();
        // hardcode for worker id 1 to 7. VM 1-7 ARE WORKERS
        for (Integer id : workerIDList) {
            List<Vertex> vertexList = new ArrayList<>(50000);
            partitionMap.put(id, vertexList);
        }

        for (Integer id : vertexMap.keySet()) {
            int workerID = workerIDList.get(id % workerNumber);
            Vertex node = vertexMap.get(id);
            partitionMap.get(workerID).add(node);
        }

    }

//    public void updateWorkStep(int workerSteps, List<Vertex> data) {
//        boolean isFinish = true;
//        // need all vertice's isActive are false
//        for (Vertex vertex : data) {
//            if (vertex.isActive()) {
//                isFinish = false;
//            }
//        }
//        if (isFinish) {
//            finishFlag--;
//        }
//    }

    public boolean finishPrevStep() {
        boolean allFinishPrevStep = true;
        for (int workerID : workerStepMap.keySet()) {
            if (superStep > workerStepMap.get(workerID)) {
                allFinishPrevStep = false;
            }
        }
        return allFinishPrevStep;
    }

    public boolean isFinish() {
        for (boolean b : finishMap.values()) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public void aggregateResult(List<Vertex> vertices) {
        application.aggregateResult(vertices);
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public Map<Integer, Integer> getWorkerStepMap() {
        return workerStepMap;
    }

    public void setWorkerStepMap(Map<Integer, Integer> workerStepMap) {
        this.workerStepMap = workerStepMap;
    }

    public List<Integer> getWorkerIDList() {
        return workerIDList;
    }

    public void setWorkerIDList(List<Integer> workerIDList) {
        this.workerIDList = workerIDList;
    }

    public Map<Integer, Boolean> getFinishMap() {
        return finishMap;
    }

    public void setFinishMap(Map<Integer, Boolean> finishMap) {
        this.finishMap = finishMap;
    }

    public String getInputFilename() {
        return inputFilename;
    }

    public void setInputFilename(String inputFilename) {
        this.inputFilename = inputFilename;
    }

    public Map<Integer, List<Vertex>> getPartitionMap() {
        return partitionMap;
    }

    public void setPartitionMap(Map<Integer, List<Vertex>> partitionMap) {
        this.partitionMap = partitionMap;
    }

    public int getSuperStep() {
        return superStep;
    }

    public void setSuperStep(int superStep) {
        this.superStep = superStep;
    }

    public Map<Integer, Vertex> getVertexMap() {
        return vertexMap;
    }

    public void setVertexMap(Map<Integer, Vertex> vertexMap) {
        this.vertexMap = vertexMap;
    }

    public List<Member> getWorkerList() {
        return workerList;
    }

    public void setWorkerList(List<Member> workerList) {
        this.workerList = workerList;
    }
}
