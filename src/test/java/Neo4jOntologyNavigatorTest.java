
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.graphdb.Node;
import neo4j.Neo4jOntologyNavigator;
import neo4j.OntologyRelationshipType;
import java.util.List;
import java.util.Map;

public class Neo4jOntologyNavigatorTest {

    private static final Logger log = Logger.getLogger(Neo4jOntologyNavigator.class);
    private static final String NEODB_PATH = "temp/neo4j";
    private static Neo4jOntologyNavigator navigator;

    @BeforeClass
    public static void setupBeforeClass() throws Exception {
        navigator = new Neo4jOntologyNavigator(NEODB_PATH);
        navigator.init();
    }

    @AfterClass
    public static void teardownAfterClass() throws Exception {
        navigator.destroy();
    }

    @Test
    public void testWhereIsLoireRegion() throws Exception {
        log.info("Query: Where is BourgogneRegion?");
        Node loireRegionNode = navigator.getByName("BourgogneRegion");
        if (loireRegionNode != null) {
            List<Node> locations = navigator.getNeighborsRelatedBy(loireRegionNode, OntologyRelationshipType.LOCATED_IN);
            for (Node location : locations) {
                log.info(location.getProperty(Neo4jOntologyNavigator.ENTITY_NAME));
            }
        }
    }

    @Test
    public void testWhatRegionsAreInUsRegion() throws Exception {
        log.info("Query: What regions are in USRegion?");
        Node usRegion = navigator.getByName("USRegion");
        if (usRegion != null) {
            List<Node> locations = navigator.getNeighborsRelatedBy(usRegion, OntologyRelationshipType.REGION_CONTAINS);
            for (Node location : locations) {
                log.info(location.getProperty(Neo4jOntologyNavigator.ENTITY_NAME));
            }
        }
    }

    @Test
    public void testWhatAreSweetWines() throws Exception {
        log.info("Query: What are Sweet wines?");
        Node sweetNode = navigator.getByName("Sweet");
        if (sweetNode != null) {
            List<Node> sweetWines = navigator.getNeighborsRelatedBy(sweetNode, OntologyRelationshipType.IS_SUGAR_CONTENT_OF);
            for (Node sweetWine : sweetWines) {
                log.info(sweetWine.getProperty(Neo4jOntologyNavigator.ENTITY_NAME));
            }
        }
    }

    @Test
    public void testShowNeighborsForAReislingWine() throws Exception {
        log.info("Query: Show neighbors for ItalianRegion");
        Node rieslingNode = navigator.getByName("ItalianRegion");
        Map<String,List<Node>> neighbors = navigator.getAllNeighbors(rieslingNode);
        for (String relType : neighbors.keySet()) {
            log.info("--- " + relType + " ---");
            List<Node> relatedNodes = neighbors.get(relType);
            for (Node relatedNode : relatedNodes) {
                log.info(relatedNode.getProperty(Neo4jOntologyNavigator.ENTITY_NAME));
            }
        }
    }

}