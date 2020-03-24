package utils;

import java.awt.image.BufferedImage;

public class Constants {

    public static BufferedImage EMPTY_IMAGE;

    static {
        try {
            EMPTY_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        } catch (Exception e) {
            // ignore
        }
    }

    public static final int MAX_REQUEST_SIZE = 10485760;

    public static final int MAX_AREA = 10000 * 10000;

    public static final String GENERATED_ROUTE = "generated";

    public static final String GRAPH_RDF_ROUTE = GENERATED_ROUTE + "/graph.rdf";

    public static final String ONTOLOGY_JSON_ROUTE = GENERATED_ROUTE + "/ontology.json";

    public static final String NEO4J_TEMP_PATH = "temp/neo4j";

    public static final String NEO4J_TEST_TEMP_PATH = "temp/neo4jTest";
}
