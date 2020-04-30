package com.nvidia.grcuda.gpu;

import com.nvidia.grcuda.gpu.computation.ComputationArgumentWithValue;
import com.nvidia.grcuda.gpu.computation.GrCUDAComputationalElement;
import com.oracle.truffle.api.interop.TruffleObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Directed Acyclic Graph (DAG) that represents the execution flow of GrCUDA kernels and other
 * computations. Each vertex is a computation, and an edge between vertices represents a dependency
 * such that the end vertex must wait for the start vertex to finish before starting.
 */
public class ExecutionDAG implements TruffleObject {

    private final List<DAGVertex> vertices = new ArrayList<>();
    private final List<DAGEdge> edges = new ArrayList<>();

    /**
     * Current frontier of the DAG, i.e. vertices with no children.
     */
    private List<DAGVertex> frontier = new ArrayList<>();

    /**
     * Add a new computation to the graph, and compute its dependencies.
     * @param kernel a kernel computation, containing kernel configuration and input arguments
     * @return the new vertex that has been appended to the DAG
     */
    public DAGVertex append(GrCUDAComputationalElement kernel) {
        // Add it to the list of vertices;
        DAGVertex newVertex = new DAGVertex(kernel);

        //////////////////////////////
        // Compute dependencies with other vertices in the DAG frontier, and create edges;
        //////////////////////////////

        // For each vertex in the frontier, compute dependencies of the vertex;
        for (DAGVertex frontierVertex : frontier) {
            List<ComputationArgumentWithValue> dependencies = computeDependencies(frontierVertex, newVertex);
            if (dependencies.size() > 0) {
                // Create a new edge between the two vertices (book-keeping is automatic);
                new DAGEdge(frontierVertex, newVertex, dependencies);
            }
        }
        // Remove from the frontier vertices that no longer belong to it;
        frontier = frontier.stream().filter(DAGVertex::isFrontier).collect(Collectors.toList());
        // Add the new vertex to the frontier if it has no children;
        if (newVertex.isFrontier()) {
            frontier.add(newVertex);
        }
        return newVertex;
    }

    private List<ComputationArgumentWithValue> computeDependencies(DAGVertex startVertex, DAGVertex endVertex) {
        return startVertex.getComputation().computeDependencies(endVertex.getComputation());
    }

    public List<DAGVertex> getVertices() {
        return vertices;
    }

    public List<DAGEdge> getEdges() {
        return edges;
    }

    public int getNumVertices() {
        return vertices.size();
    }

    public int getNumEdges() {
        return edges.size();
    }

    public List<DAGVertex> getFrontier() {
        return frontier;
    }

    @Override
    public String toString() {
        return "DAG(" +
                "|V|=" + vertices.size() +
                ", |E|=" + edges.size() +
                "\nvertices=\n" + vertices.stream().map(Object::toString).collect(Collectors.joining(",\n")) +
                ')';
    }

    /**
     * Simple vertex class used to encapsulate {@link GrCUDAComputationalElement}.
     */
    public class DAGVertex {

        private final GrCUDAComputationalElement computation;
        private final int id;

        /**
         * False only if the vertex has parent vertices.
         */
        private boolean isStart = true;
        /**
         * List of edges that connect this vertex to its parents (they are the start of each edge).
         */
        private final List<DAGEdge> parents = new ArrayList<>();
        /**
         * List of edges that connect this vertex to its children (they are the end of each edge).
         */
        private final List<DAGEdge> children = new ArrayList<>();

        DAGVertex(GrCUDAComputationalElement computation) {
            this.computation = computation;
            this.id = getNumVertices();
            vertices.add(this);
        }

        public GrCUDAComputationalElement getComputation() {
            return computation;
        }

        int getId() {
            return id;
        }

        public boolean isStart() {
            return isStart;
        }

        /**
         * A vertex is considered part of the DAG frontier if it could lead to dependencies.
         * In general, a vertex is not part of the frontier only if it has no arguments, it has already been executed,
         * or all its arguments have already been superseded by the arguments of computations that depends on this one;
         * @return if this vertex is part of the DAG frontier
         */
        public boolean isFrontier() {
            return computation.hasPossibleDependencies() && !computation.isComputationFinished();
        }

        /**
         * Check if this vertex corresponds to a computation that can be immediately executed.
         * This usually happens if the computations has no parents, or all the parents have already completed their execution;
         * @return if the computation can be started immediately
         */
        public boolean isExecutable() {
            return !computation.isComputationStarted() && (parents.isEmpty() || allParentsHaveFinishedComputation());
        }

        private boolean allParentsHaveFinishedComputation() {
            for (DAGEdge e : parents) {
                if (!e.getStart().getComputation().isComputationFinished()) return false;
            }
            return true;
        }

        public List<DAGEdge> getParents() {
            return parents;
        }

        public List<DAGEdge> getChildren() {
            return children;
        }

        public List<DAGVertex> getParentVertices() { return parents.stream().map(DAGEdge::getStart).collect(Collectors.toList()); }

        public List<DAGVertex> getChildVertices() { return children.stream().map(DAGEdge::getEnd).collect(Collectors.toList()); }

        public List<GrCUDAComputationalElement> getParentComputations() {
            return parents.stream().map(e -> e.getStart().getComputation()).collect(Collectors.toList());
        }

        public List<GrCUDAComputationalElement> getChildComputations() {
            return children.stream().map(e -> e.getEnd().getComputation()).collect(Collectors.toList());
        }

        public void setStart(boolean start) {
            isStart = start;
        }

        public void addParent(DAGEdge edge) {
            parents.add(edge);
            isStart = false;
        }

        public void addChild(DAGEdge edge) {
            children.add(edge);
        }

        @Override
        public String toString() {
            return "V(" +
                    "id=" + id +
                    ", isStart=" + isStart +
                    ", isFrontier=" + this.isFrontier() +
                    ", parents=" + parents +
                    ", children=" + children +
                    ')';
        }
    }

    /**
     * Simple edge class used to connect {@link DAGVertex} with dependencies.
     * An edge from a source to a destination means that the destination computation must wait
     * for the start computation to finish before starting.
     */
    public class DAGEdge {

        final private DAGVertex start;
        final private DAGVertex end;
        final private int id;
        /**
         * List of objects that represents depenencies between the two vertices;
         */
        private List<ComputationArgumentWithValue> dependencies;

        DAGEdge(DAGVertex start, DAGVertex end) {
            this.start = start;
            this.end = end;
            this.id = getNumEdges();

            // Update parents and children of the two vertices;
            start.addChild(this);
            end.addParent(this);
            // Book-keeping of the edge;
            edges.add(this);
        }

        DAGEdge(DAGVertex start, DAGVertex end, List<ComputationArgumentWithValue> dependencies) {
            this(start, end);
            this.dependencies = dependencies;
        }

        public DAGVertex getStart() {
            return start;
        }

        public DAGVertex getEnd() {
            return end;
        }

        public int getId() {
            return id;
        }

        public List<ComputationArgumentWithValue> getDependencies() {
            return dependencies;
        }

        @Override
        public String toString() {
            return "E(" +
                    "start=" + start.getId() +
                    ", end=" + end.getId() +
                    ')';
        }
    }
}
