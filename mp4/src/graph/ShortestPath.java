package graph;

import java.util.List;

public class ShortestPath implements Application{

    @Override
    public void compute(int step, Vertex vertex) {
        ShortestPath(step, vertex);
    }

    @Override
    public void aggregateResult(List<Vertex> vertices) {
        vertices.sort((Vertex a, Vertex b) -> {
            if (a.getValue() > b.getValue()) {
                return 1;
            } else if (a.getValue() < b.getValue()) {
                return -1;
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

    public void ShortestPath(int step, Vertex vertex) {
        int outDegree = vertex.getOutgoingVertex().size();
        if (step == 0) {
            if (vertex.getID() == 1) {
                vertex.setValue(0);
            }
            else {
                vertex.setValue(Integer.MAX_VALUE-1);
            }
        }
        else{
            // incoming
            if (vertex.getIncomingMesg() != null) {
                for (Message message : vertex.getIncomingMesg()) {
                    if (message.getValue() + 1 < vertex.getValue()) {
                        vertex.setValue(message.getValue() + 1);
                    }
                }
            }
        }

        // outgoing
        double value = vertex.getValue();
        for (int outgoingVertexID : vertex.getOutgoingVertex()) {
            Message message = new Message(outgoingVertexID, value);
            vertex.getOutgoingMesg().add(message);
        }

        if (step == 33) {
            vertex.setActive(false);
        }

    }



}
