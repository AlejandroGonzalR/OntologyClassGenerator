package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxXmlUtils;
import com.mxgraph.view.mxGraph;
import com.sun.codemodel.JCodeModel;
import neo4j.OntologyDatabaseLoader;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.SourceType;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.rules.RuleFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static utils.Constants.*;

public class Utilities {

    JsonLdConverter jsonLdConverter = new JsonLdConverter(JsonLdConverter.Format.RDF_XML);
    private static final String ROOT_NAME = "ShapesGraph";
    private static final String[][] SUB_ONTOLOGY = new String[][]{new String[] {"graph.rdf", "Graph"}};
    String packageName = "model";
    File inputJson = new File(ONTOLOGY_JSON_ROUTE);
    File outputPojoDirectory = new File(GENERATED_ROUTE);

    private static Logger log = LoggerFactory.getLogger(OntologyDatabaseLoader.class);

    public static final String VERTICES_IRI = "http://schema.org/vertex";
    public static final String TYPE_IRI = "http://schema.org/type";
    public static final String ID_IRI = "http://schema.org/id";
    public static final String NAME_IRI = "http://schema.org/name";
    public static final String EDGES_IRI = "http://schema.org/edge";
    public static final String SOURCE_IRI = "http://schema.org/source";
    public static final String TARGET_IRI = "http://schema.org/target";


    public void manageApp(String xml) throws Exception {
        Collection<Vertex> model = generateModel(getGraph(xml));
        JsonArray jsonElements = generateContext(model);
        convertJsonToPojo(jsonElements);
        rdfToDatabase();
    }

    protected String xmlToJson(String xml){
        int PRETTY_PRINT_INDENT_FACTOR = 4;
        String json = "";

        try {
            JSONObject xmlJSONObj = XML.toJSONObject(xml);
            json = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
        } catch (JSONException je) {
            log.error("onJsonCast:xmlToJson", je);
            je.printStackTrace();
        }
        return json;
    }

    protected mxGraph getGraph(String xml){
        mxGraph graph = new mxGraph();

        Document document = mxXmlUtils.parseXml(xml);
        mxCodec codec = new mxCodec(document);
        codec.decode(document.getDocumentElement(), graph.getModel());

        return graph;
    }

    protected Collection<Vertex> generateModel(mxGraph graph) {
        mxGraphModel model = (mxGraphModel) graph.getModel();
        Map<String, Object> cellsMap = model.getCells();
        Map<String, Vertex> vertexList = new HashMap<>();
        ArrayList<Edge> edgesList = new ArrayList<>();

        for (Map.Entry<String, Object> entry : cellsMap.entrySet()) {
            mxCell cell = (com.mxgraph.model.mxCell) entry.getValue();

            if (cell.isVertex()) {
                vertexList.put(cell.getId(), getValuesFromFields(cell));
            } else if (cell.isEdge()) {
                edgesList.add(getRelationsFromModel(cell));
            }
        }

        for (Edge edge : edgesList) {
            if (vertexList.containsKey(edge.getSource())) {
                Vertex vertex = vertexList.get(edge.getSource());
                vertex.addEdge(edge);
                vertexList.put(vertex.getId(), vertex);
            }
            if (vertexList.containsKey(edge.getTarget())) {
                Vertex vertex = vertexList.get(edge.getTarget());
                vertex.addEdge(edge);
                vertexList.put(vertex.getId(), vertex);
            }
        }
        //generateContext(vertexList.values());

        return vertexList.values();
    }

    protected JsonArray generateContext(Collection<Vertex> verticesValues){
        GraphModelContext modelContext = new GraphModelContext();
        ArrayList<Vertex> verticesList = new ArrayList(verticesValues);

        modelContext.setVertices(verticesList);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(gson.toJson(modelContext)).getAsJsonObject();
        jsonObject.add("@context", addContextProperties());

        // System.out.println(gson.toJson(jsonObject));
        // System.out.println(jsonLdConverter.toRdf(gson.toJson(jsonObject)));

        JsonArray objectAux = (JsonArray) jsonObject.get("vertices");

        return objectAux;
    }

    private JsonObject addContextProperties(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("vertices", VERTICES_IRI);
        jsonObject.addProperty("type", TYPE_IRI);
        jsonObject.addProperty("id", ID_IRI);
        jsonObject.addProperty("name", NAME_IRI);
        jsonObject.addProperty("edges", EDGES_IRI);
        jsonObject.addProperty("source", SOURCE_IRI);
        jsonObject.addProperty("target", TARGET_IRI);

        return jsonObject;
    }

    protected Vertex getValuesFromFields(mxCell vertexObj) {
        Vertex vertex = new Vertex();
        String[] fields = {"name", "label", "id", "type"};

        for (String name : fields) {
            Object value = vertexObj.getAttribute(name);

            switch (name) {
                case "id":
                    vertex.setId(vertexObj.getId());
                    break;
                case "name":
                    vertex.setName((String) value);
                    break;
                case "type":
                    vertex.setType(getTypeFromModel(vertexObj));
                    break;
            }
        }
        return vertex;
    }

    protected Edge getRelationsFromModel(mxCell edgeObj) {
        Edge edge = new Edge();
        String[] fields = {"source", "target"};

        for (String name : fields) {
            if (name.equals("source")) {
                edge.setSource(edgeObj.getSource().getId());
            } else {
                edge.setTarget(edgeObj.getTarget().getId());
            }
        }
        return edge;
    }

    protected String getTypeFromModel(mxCell cell){
        String type = "";
        switch (cell.getStyle()){
            case "rounded=0;whiteSpace=wrap;html=1;":
                type = "Rectangle";
                break;
            case "rounded=1;whiteSpace=wrap;html=1;":
                type = "Rounded Rectangle";
                break;
            case "ellipse;whiteSpace=wrap;html=1;":
                type = "Ellipse";
                break;
            case "whiteSpace=wrap;html=1;aspect=fixed;":
                type = "Square";
                break;
            case "ellipse;whiteSpace=wrap;html=1;aspect=fixed;":
                type = "Circle";
                break;
            case "rhombus;whiteSpace=wrap;html=1;":
                type = "Diamond";
                break;
        }
        return type;
    }

    protected void convertJsonToPojo(JsonArray jsonElements) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        for (JsonElement element : jsonElements) {
            String outJson = gson.toJson(element);
            FileOutputStream outputStream = new FileOutputStream(inputJson);
            byte[] contentInBytes = outJson.getBytes();
            outputStream.write(contentInBytes);
            outputStream.flush();
            outputStream.close();

            JsonObject vertexObj = element.getAsJsonObject();
            String className = vertexObj.get("type").getAsString();

            outputPojoDirectory.mkdirs();
            JCodeModel codeModel = new JCodeModel();
            URL source = inputJson.toURI().toURL();

            GenerationConfig config = new DefaultGenerationConfig() {
                @Override
                public boolean isGenerateBuilders() {
                    return true;
                }
                public SourceType getSourceType(){
                    return SourceType.JSON;
                }
            };

            SchemaMapper mapper = new SchemaMapper(new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()), new SchemaGenerator());
            mapper.generate(codeModel, className, packageName, source);
            codeModel.build(outputPojoDirectory);
        }
    }

    protected void rdfToDatabase() throws Exception {
        for (String[] subOntology : SUB_ONTOLOGY) {
            OntologyDatabaseLoader loader = new OntologyDatabaseLoader();
            loader.setRefNodeName(ROOT_NAME);
            loader.setFilePath(FilenameUtils.concat(GENERATED_ROUTE, subOntology[0]));
            loader.setDatabasePath(NEO4J_TEMP_PATH);
            loader.setOntologyName(subOntology[1]);
            loader.loadFile();
        }
    }
}
