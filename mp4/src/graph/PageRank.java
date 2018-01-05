package graph;

import java.util.List;

public class PageRank implements Application {
//    public Graph graph;
    @Override
    public void compute(int step, Vertex vertex) {
        pageRank(step, vertex);
    }

    @Override
    public void aggregateResult(List<Vertex> vertices) {
        vertices.sort((Vertex a, Vertex b) -> {
            if (a.getValue() > b.getValue()) {
                return -1;
            } else if (a.getValue() < b.getValue()) {
                return 1;
            } else {
                return 0;
            }
        });
        if (vertices.size() >= 25) {
            for (int i = 0; i < 25; i++) {
                System.out.println("#" + (i + 1) + " " + vertices.get(i).getID() + ": " + vertices.get(i).getValue());
            }
        }
    }

    public void pageRank(int step, Vertex vertex) {

        int outDegree = vertex.getOutgoingVertex().size();
        if (step == 0) {
            vertex.setValue(1.0);
        }
        else{
            // incoming
            double newValue = 0.15;
            if (vertex.getIncomingMesg() != null) {
                for (Message message : vertex.getIncomingMesg()) {
                    newValue += message.getValue() * 0.85;
                }
            }
            vertex.setValue(newValue);
        }

        // outgoing
        double currValue = vertex.getValue();
        double value = currValue/outDegree;
        for (int outgoingVertexID : vertex.getOutgoingVertex()) {
            Message message = new Message(outgoingVertexID, value);
            vertex.getOutgoingMesg().add(message);
        }

        if (step == 20) { // hardcode for test 20 itertions
            vertex.setActive(false);
        }
    }

}
