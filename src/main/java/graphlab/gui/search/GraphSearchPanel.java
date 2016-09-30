package graphlab.gui.search;

import graphlab.algorithms.Search;
import graphlab.algorithms.Algorithm;
import graphlab.datastructures.AdjacencyListGraph;
import graphlab.datastructures.Edge;
import graphlab.datastructures.Node;
import graphlab.gui.GraphContainerPanel;
import graphlab.gui.GraphPanel;
import graphlab.utils.ConsumerWithException;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GraphSearchPanel extends GraphPanel {

    private GraphSearchWorker searchWorker;

    public GraphSearchPanel(Algorithm algorithm, GraphContainerPanel parentPanel, AdjacencyListGraph graph) {
        super(algorithm, parentPanel, graph, true);
        this.algorithm = algorithm;
        this.parentPanel = parentPanel;
        this.graph = graph;
        setBorder(WORKING_BORDER);
        setBackground(new Color(200, 200, 200));

        JMenuItem menuItem = new JMenuItem(SET_SEARCHED_NODE_LABEL);
        menuItem.addActionListener(this);
        popupMenu.add(menuItem);

        MouseListener popupListener = new PopupListener();
        this.addMouseListener(popupListener);
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void startOperation() {
        setBorder(WORKING_BORDER);
        visitedEdges = new ArrayList<>();
        visitedNodes = new ArrayList<>();
        processedNodes = new ArrayList<>();
        repaint();
        this.isFinished = false;
        searchWorker = new GraphSearchWorker(visitedNodes, visitedEdges, processedNodes);
        searchWorker.execute();
    }

    public void setOperationAsFinished() {
        this.isFinished = true;
        setBorder(FINISHED_BORDER);
        if (parentPanel.graphPanels.stream().allMatch(panel -> panel.isFinished)) {
            parentPanel.setOperationAsFinished();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dimension = parentPanel.getSize();
        panelSide = dimension.width < dimension.height ? dimension.width /2 - X_SHIFT : dimension.height/2 - Y_SHIFT;
        return new Dimension(panelSide, panelSide);
    }


    public void stopOperation() {
        setBorder(WORKING_BORDER);
        searchWorker.cancel(true);
    }

    class GraphSearchWorker extends SwingWorker<Void, Void> {

        List<Node> visitedNodes;
        List<Edge> visitedEdges;
        List<Node> processedNodes;

        public GraphSearchWorker(List<Node> visitedNodes, List<Edge> visitedEdges, List<Node> processedNodes) {
            this.visitedNodes = visitedNodes;
            this.visitedEdges = visitedEdges;
            this.processedNodes = processedNodes;
        }

        @Override
        protected Void doInBackground() throws Exception {

            Boolean isCanceled = new Boolean(false);

            Consumer<Node> visitNode = node -> {
                visitedNodes.add(node);
                setProgressBar((int) ((visitedNodes.size() / (float) graph.getNodes().size()) * 100));
            };
            Consumer<Node> processNode = node -> processedNodes.add(node);
            ConsumerWithException<Edge> visitEdge = edge -> {
                visitedEdges.add(edge);
                updateGraph();
            };

            switch (algorithm) {
                case BFS:
                    Search.bfs(graph, visitNode, visitEdge, processNode, isCanceled, true);
                    break;
                case DFS:
                    Search.dfs(graph, visitNode, visitEdge, processNode, isCanceled, true);
                    break;
                case UCS:
                    Search.ucs(graph, visitNode, visitEdge, processNode, isCanceled);
                    break;
                case ASTAR:
                    Search.astar(graph, visitNode, visitEdge, processNode, isCanceled);
                    break;
            }

            setProgressBar(0);
            setOperationAsFinished();
            return null;
        }

    }
}