package graph;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public class Message implements Serializable{
    private int targetNode;
//    private String action;
    private double value;

    public Message(int targetNode, double value) {
        this.targetNode = targetNode;
        this.value = value;
//        this.action = null;
    }

    public int getTargetNode() {
        return targetNode;
    }
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(targetNode);
//        out.writeObject(action);
        out.writeDouble(value);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        targetNode = in.readInt();
//        action = (String) in.readObject();
        value = in.readDouble();
    }

    public void setTargetNode(int targetNode) {
        this.targetNode = targetNode;
    }

    public double getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
//
//    public String getAction() {
//        return action;
//    }
//
//    public void setAction(String action) {
//        this.action = action;
//    }
}
