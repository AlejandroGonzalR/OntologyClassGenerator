import org.apache.commons.io.FilenameUtils;
import org.junit.Test;
import neo4j.OntologyDatabaseLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OntologyDatabaseLoaderTest {
    private static final String ROOT_NAME = "ConsumableThing";

    private final Logger log = LoggerFactory.getLogger(OntologyDatabaseLoader.class);

    private static final String[][] SUB_ONTOLOGIES = new String[][] {
            // Test based on principal Owl examples
            new String[] {"wine.rdf", "Wine"},
            new String[] {"food.rdf", "EdibleThing"}
    };

    @Test
    public void testLoading() throws Exception {
        for (String[] subOntology : SUB_ONTOLOGIES) {
            log.info("Processing file test: " + subOntology[0]);
            OntologyDatabaseLoader loader = new OntologyDatabaseLoader();
            loader.setRefNodeName(ROOT_NAME);
            loader.setFilePath(FilenameUtils.concat("src/main/resources/owl", subOntology[0]));
            loader.setDatabasePath("temp/neo4j");
            loader.setOntologyName(subOntology[1]);
            loader.loadFile();
        }
    }
}