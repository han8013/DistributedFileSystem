package graph;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Vertex implements Serializable{
    private int ID;
    private double value;
    private List<Integer> outgoingVertex;
    private List<Message> incomingMesg;
    private List<Message> outgoingMesg;
    private boolean isActive;
    private int superStep;

    public Vertex(int ID) {
        this.ID = ID;
        this.value = 0.0;
        outgoingVertex = new ArrayList<>();
        outgoingMesg = new LinkedList<>();
        incomingMesg = new LinkedList<>();
        isActive = true;
    }

    public Vertex(int ID, int value) {
        this.ID = ID;
        this.value = value;
        outgoingVertex = new ArrayList<>();
        outgoingMesg = new LinkedList<>();
        incomingMesg = new LinkedList<>();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeInt(ID);
        out.writeDouble(value);
        out.writeObject(outgoingVertex);
        out.writeObject(incomingMesg);
        out.writeObject(outgoingMesg);
        out.writeBoolean(isActive);
        out.writeInt(superStep);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        ID = in.readInt();
        value = in.readDouble();
        outgoingVertex = (List<Integer>)in.readObject();
        incomingMesg = (List<Message>)in.readObject();
        outgoingMesg = (List<Message>)in.readObject();
        isActive = in.readBoolean();
        superStep = in.readInt();
    }

    public List<Message> getIncomingMesg() {
        return incomingMesg;
    }

    public void setIncomingMesg(List<Message> incomingMesg) {
        this.incomingMesg = incomingMesg;
    }

    public List<Message> getOutgoingMesg() {
        return outgoingMesg;
    }

    public void setOutgoingMesg(List<Message> outgoingMesg) {
        this.outgoingMesg = outgoingMesg;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setOutgoingVertex(List<Integer> outgoingVertex) {
        this.outgoingVertex = outgoingVertex;
    }


    public void setActive(boolean active) {
        isActive = active;
    }

    public void setSuperStep(int superStep) {
        this.superStep = superStep;
    }

    public int getID() {
        return ID;
    }

    public double getValue() {
        return value;
    }

    public List<Integer> getOutgoingVertex() {
        return outgoingVertex;
    }

    public boolean isActive() {
        return isActive;
    }

    public int getSuperStep() {
        return superStep;
    }

}
