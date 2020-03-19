package neo4j;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.Traverser.Order;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Provides methods to locate nodes and find neighbors in the Neo4j graph database.
 */

public class Neo4jOntologyNavigator {
    public static final String ENTITY_NAME = "name";
    public static final String RELATIONSHIP_NAME = "name";
    public static final String RELATIONSHIP_WEIGHT = "weight";

    private String neo4jPath;
    private GraphDatabaseService neoService;
    private IndexService indexService;

    public Neo4jOntologyNavigator(String databasePath) {
        super();
        this.neo4jPath = databasePath;
    }

    /**
     * The init() method should be called by client after instantiation.
     */

    public void init() {
        this.neoService = new EmbeddedGraphDatabase(neo4jPath);
        this.indexService = new LuceneIndexService(neoService);
    }

    /**
     * The destroy() method should be called by client on shutdown.
     */

    public void destroy() {
        indexService.shutdown();
        neoService.shutdown();
    }

    /**
     * Gets the reference to the named Node. Returns null if the node is not found in the database.
     *
     * @param nodeName the name of the node to lookup.
     * @return the reference to the Node, or null if not found.
     */

    public Node getByName(String nodeName) {
        Transaction transaction = neoService.beginTx();
        try {
            Node node = indexService.getSingleNode(ENTITY_NAME, nodeName);
            transaction.success();
            return node;
        } catch (Exception e) {
            transaction.failure();
            throw(e);
        } finally {
            transaction.finish();
        }
    }

    /**
     * Return a Map of relationship names to a List of nodes connected by that relationship. The keys are sorted by name,
     * and the list of node values are sorted by the incoming relation weights.
     *
     * @param node the root Node.
     * @return a Map of String to Node List of neighbors.
     */

    public Map<String, List<Node>> getAllNeighbors(Node node) throws Exception {
        BrowserReturnableEvaluator browserReturnableEvaluator = new BrowserReturnableEvaluator(node);
        Transaction transaction = neoService.beginTx();

        try {
            OntologyRelationshipType[] relationshipTypes = OntologyRelationshipType.values();
            Object[] typeAndDirection = new Object[relationshipTypes.length * 2];

            for (int i = 0; i < typeAndDirection.length; i++) {
                if (i % 2 == 0) {
                    typeAndDirection[i] = relationshipTypes[i / 2];
                } else {
                    typeAndDirection[i] = Direction.OUTGOING;
                }
            }

            Traverser traverser = node.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE, browserReturnableEvaluator, typeAndDirection);
            for (Iterator<Node> iterator = traverser.iterator(); iterator.hasNext();) {
                iterator.next();
            }
            transaction.success();
            return browserReturnableEvaluator.getNeighbors();
        } catch (Exception e) {
            transaction.failure();
            throw e;
        } finally {
            transaction.finish();
        }
    }

    /**
     * Returns a List of neighbor nodes that is reachable from the specified Node. No ordering is done (since the Traverser
     * framework does not seem to allow this type of traversal, and we want to use the Traverser here).
     *
     * @param node reference to the base node.
     * @param type the relationship type.
     * @return a List of neighbor nodes.
     */

    public List<Node> getNeighborsRelatedBy(Node node, OntologyRelationshipType type) throws Exception {
        List<Node> neighbors = new ArrayList<>();
        Transaction transaction = neoService.beginTx();
        try {
            Traverser traverser = node.traverse(Order.BREADTH_FIRST, StopEvaluator.DEPTH_ONE, ReturnableEvaluator.ALL_BUT_START_NODE, type, Direction.OUTGOING);
            for (Node neighbor : traverser) {
                neighbors.add(neighbor);
            }
            transaction.success();
        } catch (Exception e) {
            transaction.failure();
            throw(e);
        } finally {
            transaction.success();
        }
        return neighbors;
    }
}