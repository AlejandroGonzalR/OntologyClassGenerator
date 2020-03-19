import neo4j.BrowserReturnableEvaluator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Traverser;
import org.neo4j.graphdb.StopEvaluator;
import org.neo4j.graphdb.Direction;
import org.neo4j.kernel.EmbeddedGraphDatabase;

public class BrowserReturnableEvaluatorTest {
    private static final Object[][] QUADS = new Object[][] {
            new Object[] {"coke", RelTypes.GOES_WITH, 10.0F, "whopper"},
            new Object[] {"coke", RelTypes.GOES_WITH, 10.0F, "doubleWhopper"},
            new Object[] {"coke", RelTypes.GOES_WITH, 5.0F, "tripleWhopper"},
            new Object[] {"coke", RelTypes.HAS_INGREDIENTS, 10.0F, "water"},
            new Object[] {"coke", RelTypes.HAS_INGREDIENTS, 9.0F, "sugar"},
            new Object[] {"coke", RelTypes.HAS_INGREDIENTS, 2.0F, "carbonDioxide"},
            new Object[] {"coke", RelTypes.HAS_INGREDIENTS, 5.0F, "secretRecipe"}
    };

    private enum RelTypes implements RelationshipType {
        GOES_WITH,
        HAS_INGREDIENTS
    };

    private static GraphDatabaseService neoService;
    private static Node coke;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        neoService = new EmbeddedGraphDatabase("temp/neo4jTest");
        Transaction tx = neoService.beginTx();
        try {
            coke = neoService.createNode();
            coke.setProperty("name", "coke");
            for (Object[] quad : QUADS) {
                Node objectNode = neoService.createNode();
                objectNode.setProperty("name", (String) quad[3]);
                Relationship rel = coke.createRelationshipTo(objectNode, (RelationshipType) quad[1]);
                rel.setProperty("name", ((RelationshipType) quad[1]).name());
                rel.setProperty("weight", (Float) quad[2]);
            }
            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw e;
        } finally {
            tx.finish();
        }
    }

    @AfterClass
    public static void teardownAfterClass() throws Exception {
        if (neoService != null) {
            neoService.shutdown();
        }
    }

    @Test
    public void testCustomEvaluator() throws Exception {
        Transaction tx = neoService.beginTx();
        try {
            BrowserReturnableEvaluator customReturnEvaluator = new BrowserReturnableEvaluator(coke);
            Traverser traverser = coke.traverse(
                    Traverser.Order.BREADTH_FIRST,
                    StopEvaluator.DEPTH_ONE,
                    customReturnEvaluator,
                    RelTypes.GOES_WITH, Direction.OUTGOING,
                    RelTypes.HAS_INGREDIENTS, Direction.OUTGOING);

            for (Iterator<Node> it = traverser.iterator(); it.hasNext();) {
                it.next();
            }

            Map<String,List<Node>> neighbors = customReturnEvaluator.getNeighbors();

            for (String relName : neighbors.keySet()) {
                System.out.println("-- " + relName + " --");
                List<Node> relatedNodes = neighbors.get(relName);
                for (Node relatedNode : relatedNodes) {
                    System.out.println(relatedNode.getProperty("name"));
                }
            }
            tx.success();
        } catch (Exception e) {
            tx.failure();
            throw e;
        } finally {
            tx.finish();
        }
    }
}