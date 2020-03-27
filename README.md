# Ontology POJO Class Generator

Dynamic Java POJO class generator based on an ontology drawn from the MxGraph editor storing entities and relationships on embedded neo4j database.

## Getting Started

This project is built with maven, to start import the changes of the POM file and add the libraries found in the [lib](https://github.com/AlejandroGonzalR/OntologyClassGenerator/tree/master/src/main/resources/lib) package, now in the web package of the java source package run the [Editor Server](https://github.com/AlejandroGonzalR/OntologyClassGenerator/blob/master/src/main/java/web/EditorServer.java) file, the application will be available at [http://localhost:8090/src/main/www/index.html](http://localhost:8090/src/main/www/index.html) 

![editor screenshot](https://github.com/AlejandroGonzalR/OntologyClassGenerator/blob/master/src/main/resources/images/Editor%Screenshot.png)

You can draw the ontology you need. Then, following the order of the MxGraph library, you can save your graph in the **save** option of the **file** menu, here the ontology will be generated and the POJO classes based on the graph, an example of the generated files can be found following [this link](https://github.com/AlejandroGonzalR/OntologyClassGenerator/tree/master/generated)

**Note**: The validated shapes are Rectangle, Rounded Rectangle, Ellipse, Square, Circle and Rhombus. If you want to validate more, add the web query of the shape in **getTypeFromModel** Method in **Utilities** Java Class.

## Running the tests

The tests are focused on the implementation of ontologies in the neo4j database, for this the basic examples of ontologies [wine.rdf](https://github.com/AlejandroGonzalR/OntologyClassGenerator/blob/master/src/main/resources/owl/wine.rdf) and [food.rdf](https://github.com/AlejandroGonzalR/OntologyClassGenerator/blob/master/src/main/resources/owl/food.rdf) were used.

In the test directory you can run each test, these are classified in data load to the database, relationships and queries.

## Built With

* [Maven](https://maven.apache.org/) - Dependency Management

## Authors

* **Alejandro González Rodríguez** - *Initial work*

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details
