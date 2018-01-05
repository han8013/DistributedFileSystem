package graph;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class Graph implements Serializable{
    private int nodeNumber;
    private int edgeNumber;
    private List<Integer> workerIDList;
    private String applicationName;

    public Graph() {
        nodeNumber = 0;
        edgeNumber = 0;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(nodeNumber);
        out.writeInt(edgeNumber);
        out.writeObject(workerIDList);
        out.writeObject(applicationName);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        nodeNumber = in.readInt();
        edgeNumber = in.readInt();
        workerIDList = (List<Integer>) in.readObject();
        applicationName = (String) in.readObject();
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public List<Integer> getWorkerIDList() {
        return workerIDList;
    }

    public void setWorkerIDList(List<Integer> workerIDList) {
        this.workerIDList = workerIDList;
    }

    public int getNodeNumber() {
        return nodeNumber;
    }

    public void setNodeNumber(int nodeNumber) {
        this.nodeNumber = nodeNumber;
    }

    public int getEdgeNumber() {
        return edgeNumber;
    }

    public void setEdgeNumber(int edgeNumber) {
        this.edgeNumber = edgeNumber;
    }
}
