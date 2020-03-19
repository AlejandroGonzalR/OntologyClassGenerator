package utils;

import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.jena.JenaTripleCallback;
import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.rdf.model.Model;
import neo4j.OntologyDatabaseLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;

public class JsonLdConverter {

    private static Logger log = LoggerFactory.getLogger(OntologyDatabaseLoader.class);
    private final Format format;

    public  enum Format {
        RDF_XML("RDF/XML"), RDF_XML_ABBREV("RDF/XML-ABBREV"), N_TRIPLE("N-TRIPLE"), N3("N3"), TURTLE("TURTLE");

        private final String name;

        Format(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public JsonLdConverter(final Format format) {
        this.format = format;
    }

    public String toRdf(final String jsonLd) {
        try {
            final Object jsonObject = JsonUtils.fromString(jsonLd);
            final JenaTripleCallback callback = new JenaTripleCallback();
            final Model model = (Model) JsonLdProcessor.toRDF(jsonObject, callback);
            final StringWriter writer = new StringWriter();
            model.write(writer, format.getName());

            FileOutputStream output = new FileOutputStream("generated/graph.rdf");
            model.write(output);

            return writer.toString();
        } catch (IOException | JsonLdError e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }
}
