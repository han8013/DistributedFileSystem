package graph;

import java.util.List;

interface Application {

    void compute(int step, Vertex vertex);

    void aggregateResult(List<Vertex> vertices);
}
