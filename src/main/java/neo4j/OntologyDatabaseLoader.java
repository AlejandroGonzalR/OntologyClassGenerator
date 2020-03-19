package neo4j;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Direction;
import org.neo4j.index.IndexService;
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Node_URI;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Parses an Ontology file with RDF serialization and get into a graph database directly (Neo4j).
 */

public class OntologyDatabaseLoader {
    private static final String ENTITY_NAME = "name";
    private static final String ENTITY_TYPE = "type";
    private static final String RELATIONSHIP_NAME = "name";
    private static final String RELATIONSHIP_WEIGHT = "weight";

    private static Logger log = LoggerFactory.getLogger(OntologyDatabaseLoader.class);

    private String filePath;
    private String databasePath;
    private String ontologyName;
    private String refNodeName;

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }

    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
    }

    public void setRefNodeName(String refNodeName) {
        this.refNodeName = refNodeName;
    }

    public void loadFile() throws Exception {
        File file = new File(filePath);
        GraphDatabaseService neoService = null;
        IndexService indexService = null;

        try {
            neoService = new EmbeddedGraphDatabase(databasePath);
            indexService = new LuceneIndexService(neoService);
            org.neo4j.graphdb.Node refNode = getReferenceNode(neoService);
            org.neo4j.graphdb.Node fileNode = getFileNode(neoService, refNode);
            Model model = ModelFactory.createDefaultModel();
            model.read(file.toURI().toString());
            StmtIterator iterator = model.listStatements();

            while (iterator.hasNext()) {
                Statement statement = iterator.next();
                Triple triple = statement.asTriple();
                insertIntoDatabase(neoService, indexService, fileNode, triple);
            }
        } finally {
            if (indexService != null) {
                indexService.shutdown();
            }
            if (neoService != null) {
                neoService.shutdown();
            }
        }
    }

    /**
     * Get the reference node if already available, otherwise create it.
     *
     * @param neoService the reference to the Neo service.
     * @return a Neo4j Node object reference to the reference node.
     */

    private org.neo4j.graphdb.Node getReferenceNode(GraphDatabaseService neoService) throws Exception {
        org.neo4j.graphdb.Node refNode = null;
        Transaction transaction = neoService.beginTx();

        try {
            refNode = neoService.getReferenceNode();
            if (! refNode.hasProperty(ENTITY_NAME)) {
                refNode.setProperty(ENTITY_NAME, refNodeName);
                refNode.setProperty(ENTITY_TYPE, "Thing");
            }
            transaction.success();
        } catch (NotFoundException e) {
            transaction.failure();
            throw e;
        } finally {
            transaction.finish();
        }
        return refNode;
    }

    /**
     * Creates a single node for the file. Once the node is created, it is connected to the reference node.
     *
     * @param neoService the reference to the Neo service.
     * @param refNode the reference to the reference node.
     * @return the "file" node representing the entry-point into the entities described by the current OWL file.
     */

    private org.neo4j.graphdb.Node getFileNode(GraphDatabaseService neoService, org.neo4j.graphdb.Node refNode) throws Exception {
        org.neo4j.graphdb.Node fileNode = null;
        Transaction transaction = neoService.beginTx();

        try {
            fileNode = neoService.createNode();
            fileNode.setProperty(ENTITY_NAME, ontologyName);
            fileNode.setProperty(ENTITY_TYPE, "Class");
            Relationship relationship = refNode.createRelationshipTo(fileNode, OntologyRelationshipType.CATEGORIZED_AS);
            logTriple(refNode, OntologyRelationshipType.CATEGORIZED_AS, fileNode);
            relationship.setProperty(RELATIONSHIP_NAME, OntologyRelationshipType.CATEGORIZED_AS.name());
            relationship.setProperty(RELATIONSHIP_WEIGHT, 0.0F);
            transaction.success();
        } catch (Exception e) {
            transaction.failure();
            throw e;
        } finally {
            transaction.finish();
        }
        return fileNode;
    }

    /**
     * Inserts selected entities and relationships from Triples extracted from the OWL document by the Jena parser.
     * Only entities which have a non-blank node for the subject and object are used. Further, only relationship types
     * listed in OntologyRelationshipTypes enum are considered. In addition, if the enum specifies that certain relationship
     * types have an inverse, the inverse relation is also created here.
     *
     * @param neoService a reference to the Neo service.
     * @param indexService a reference to the Index service (for looking up Nodes by name).
     * @param fileNode a reference to the Node that is an entry point into this ontology. This node will connect to both
     *                 the subject and object nodes of the selected triples via a CONTAINS relationship.
     * @param triple a reference to the Triple extracted by the Jena parser.
     */

    private void insertIntoDatabase(GraphDatabaseService neoService, IndexService indexService, org.neo4j.graphdb.Node fileNode, Triple triple) throws Exception {
        Node subject = triple.getSubject();
        Node predicate = triple.getPredicate();
        Node object = triple.getObject();

        if ((subject instanceof Node_URI) && (object instanceof Node_URI)) {
            org.neo4j.graphdb.Node subjectNode = getEntityNode(neoService, indexService, subject);
            org.neo4j.graphdb.Node objectNode = getEntityNode(neoService, indexService, object);
            if (subjectNode == null || objectNode == null) {
                return;
            }

            Transaction transaction = neoService.beginTx();

            try {
                transactionProcess(neoService, fileNode, subjectNode, objectNode, predicate);
                transaction.success();
            } catch (Exception e) {
                transaction.failure();
                throw e;
            } finally {
                transaction.finish();
            }
        }
    }

    public void transactionProcess(GraphDatabaseService neoService, org.neo4j.graphdb.Node fileNode, org.neo4j.graphdb.Node subjectNode, org.neo4j.graphdb.Node objectNode, Node predicate) throws Exception {
        OntologyRelationshipType type = OntologyRelationshipType.fromName(predicate.getLocalName());
        OntologyRelationshipType inverseType = OntologyRelationshipType.inverseOf(predicate.getLocalName());

        setConnectedNodeProps(neoService, fileNode, subjectNode);

        setConnectedNodeProps(neoService, fileNode, objectNode);

        if (type != null) {
            logTriple(subjectNode, type, objectNode);
            Relationship relationship = subjectNode.createRelationshipTo(objectNode, type);
            relationship.setProperty(RELATIONSHIP_NAME, type.name());
            relationship.setProperty(RELATIONSHIP_WEIGHT, 1.0F);
        }

        if (inverseType != null) {
            logTriple(objectNode, inverseType, subjectNode);
            Relationship inverseRel = objectNode.createRelationshipTo(subjectNode, inverseType);
            inverseRel.setProperty(RELATIONSHIP_NAME, inverseType.name());
            inverseRel.setProperty(RELATIONSHIP_WEIGHT, 1.0F);
        }
    }

    private void setConnectedNodeProps(GraphDatabaseService neoService, org.neo4j.graphdb.Node fileNode, org.neo4j.graphdb.Node node) throws Exception {
        if (isConnected(neoService, fileNode, OntologyRelationshipType.CONTAINS, Direction.OUTGOING, node)) {
            logTriple(fileNode, OntologyRelationshipType.CONTAINS, node);
            Relationship relationship = fileNode.createRelationshipTo(node, OntologyRelationshipType.CONTAINS);
            relationship.setProperty(RELATIONSHIP_NAME, OntologyRelationshipType.CONTAINS.name());
            relationship.setProperty(RELATIONSHIP_WEIGHT, 0.0F);
        }
    }

    /**
     * Loops through the relationships and returns true if the source and target nodes are connected using the specified
     * relationship type and direction.
     *
     * @param neoService a reference to the NeoService.
     * @param sourceNode the source Node object.
     * @param relationshipType the type of relationship.
     * @param direction the direction of the relationship.
     * @param targetNode the target Node object.
     */

    private boolean isConnected(GraphDatabaseService neoService, org.neo4j.graphdb.Node sourceNode, OntologyRelationshipType relationshipType, Direction direction, org.neo4j.graphdb.Node targetNode) throws Exception {
        boolean isConnected = false;
        Transaction transaction = neoService.beginTx();

        try {
            for (Relationship relationship : sourceNode.getRelationships(relationshipType, direction)) {
                org.neo4j.graphdb.Node endNode = relationship.getEndNode();
                if (endNode.getProperty(ENTITY_NAME).equals(targetNode.getProperty(ENTITY_NAME))) {
                    isConnected = true;
                    break;
                }
            }
            transaction.success();
        } catch (Exception e) {
            transaction.failure();
            throw e;
        } finally {
            transaction.finish();
        }
        return !isConnected;
    }

    private org.neo4j.graphdb.Node getEntityNode(GraphDatabaseService neoService, IndexService indexService, Node entity) throws Exception {
        String uri = entity.getURI();

        if (uri.indexOf('#') == -1) {
            return null;
        }

        String[] parts = StringUtils.split(uri, "#");
        String type = parts[0].substring(0, parts[0].lastIndexOf('/'));
        Transaction transaction = neoService.beginTx();

        try {
            org.neo4j.graphdb.Node entityNode = indexService.getSingleNode(ENTITY_NAME, parts[1]);
            if (entityNode == null) {
                entityNode = neoService.createNode();
                entityNode.setProperty(ENTITY_NAME, parts[1]);
                entityNode.setProperty(ENTITY_TYPE, type);
                indexService.index(entityNode, ENTITY_NAME, parts[1]);
            }
            transaction.success();
            return entityNode;
        } catch (Exception e) {
            transaction.failure();
            throw e;
        } finally {
            transaction.finish();
        }
    }

    /**
     * Convenience method to log the triple when it is inserted into the database.
     *
     * @param sourceNode the subject of the triple.
     * @param ontologyRelationshipType the predicate of the triple.
     * @param targetNode the object of the triple.
     */

    private void logTriple(org.neo4j.graphdb.Node sourceNode, OntologyRelationshipType ontologyRelationshipType, org.neo4j.graphdb.Node targetNode) {
        log.info("(" + sourceNode.getProperty(ENTITY_NAME) + "," + ontologyRelationshipType.name() + "," + targetNode.getProperty(ENTITY_NAME) + ")");
    }
}
