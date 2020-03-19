package neo4j;

import org.neo4j.graphdb.RelationshipType;

/**
 * Relationships exposed by the taxonomy
 * based on principal Owl examples: Wine.rdf and food.rdf
 */

public enum OntologyRelationshipType implements RelationshipType {
    CATEGORIZED_AS(null, null),
    CONTAINS(null, null),
    ADJACENT_REGION("adjacentRegion", "adjacentRegion"),
    HAS_VINTAGE_YEAR("hasVintageYear", "isVintageYearOf"),
    LOCATED_IN("locatedIn", "regionContains"),
    MADE_FROM_GRAPE("madeFromGrape", "mainIngredient"),
    HAS_FLAVOR("hasFlavor", "isFlavorOf"),
    HAS_COLOR("hasColor", "isColorOf"),
    HAS_SUGAR("hasSugar", "isSugarContentOf"),
    HAS_BODY("hasBody", "isBodyOf"),
    HAS_MAKER("hasMaker", "madeBy"),
    IS_INSTANCE_OF("type", "hasInstance"),
    SUBCLASS_OF("subClassOf", "superClassOf"),
    DISJOINT_WITH("disjointWith", "disjointWith"),
    DIFFERENT_FROM("differentFrom", "differentFrom"),
    DOMAIN("domain", null),
    IS_VINTAGE_YEAR_OF("isVintageYearOf", "hasVintageYear"),
    REGION_CONTAINS("regionContains", "locatedIn"),
    MAIN_INGREDIENT("mainIngredient", "madeFromGrape"),
    IS_FLAVOR_OF("isFlavorOf", "hasFlavor"),
    IS_COLOR_OF("isColorOf", "hasColor"),
    IS_SUGAR_CONTENT_OF("isSugarContentOf", "hasSugar"),
    IS_BODY_OF("isBodyOf", "hasBody"),
    MADE_BY("madeBy", "hasMaker"),
    HAS_INSTANCE("hasInstance", "type"),
    SUPERCLASS_OF("superClassOf", "subClassOf");

    private String name;
    private String inverseName;

    OntologyRelationshipType(String name, String inverseName) {
        this.name = name;
        this.inverseName = inverseName;
    }

    public static OntologyRelationshipType fromName(String name) {
        for (OntologyRelationshipType type : values()) {
            if (name.equals(type.name)) {
                return type;
            }
        }
        return null;
    }

    public static OntologyRelationshipType inverseOf(String name) {
        OntologyRelationshipType rel = fromName(name);
        if (rel != null && rel.inverseName != null) {
            return fromName(rel.inverseName);
        } else {
            return null;
        }
    }
}
