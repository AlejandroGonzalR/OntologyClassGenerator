package neo4j;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.ReturnableEvaluator;
import org.neo4j.graphdb.TraversalPosition;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Collections;

public class BrowserReturnableEvaluator implements ReturnableEvaluator {

    private Node startNode;
    private TreeMap<String, ArrayList<WeightedNode>> neighbors;

    private static class WeightedNode implements Comparable<WeightedNode> {
        public Node node;
        public Float weight;

        public WeightedNode(Node node, Float weight) {
            this.node = node;
            this.weight = weight;
        }

        public int compareTo(WeightedNode that) {
            return (that.weight.compareTo(this.weight));
        }
    };

    public BrowserReturnableEvaluator(Node startNode) {
        this.startNode = startNode;
        this.neighbors = new TreeMap<String, ArrayList<WeightedNode>>();
    }

    public boolean isReturnableNode(TraversalPosition pos) {
        Node currentNode = pos.currentNode();

        if (startNode.getProperty(Neo4jOntologyNavigator.ENTITY_NAME).equals(currentNode.getProperty(Neo4jOntologyNavigator.ENTITY_NAME))) {
            return false;
        }

        Relationship lastRel = pos.lastRelationshipTraversed();
        Float relWeight = (Float) lastRel.getProperty(Neo4jOntologyNavigator.RELATIONSHIP_WEIGHT);

        if (relWeight <= 0.0F) {
            return false;
        }

        String relName = (String) lastRel.getProperty(Neo4jOntologyNavigator.RELATIONSHIP_NAME);
        ArrayList<WeightedNode> nodes;

        if (neighbors.containsKey(relName)) {
            nodes = neighbors.get(relName);
        } else {
            nodes = new ArrayList<WeightedNode>();
        }

        nodes.add(new WeightedNode(currentNode, relWeight));
        neighbors.put(relName, nodes);
        return true;
    }

    public Map<String, List<Node>> getNeighbors() {
        Map<String, List<Node>> neighborsMap = new LinkedHashMap<String, List<Node>>();
        for (String relName : neighbors.keySet()) {
            List<WeightedNode> weightedNodes = neighbors.get(relName);
            Collections.sort(weightedNodes);
            List<Node> relatedNodes = new ArrayList<Node>();
            for (WeightedNode weightedNode : weightedNodes) {
                relatedNodes.add(weightedNode.node);
            }
            neighborsMap.put(relName, relatedNodes);
        }
        return neighborsMap;
    }
}
