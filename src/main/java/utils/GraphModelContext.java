package utils;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class GraphModelContext {

    @SerializedName("@context")
    String context;

    @SerializedName("vertices")
    List<Vertex> vertices = new ArrayList<>();


    public List<Vertex> getVertices() {
        return vertices;
    }

    public void setVertices(ArrayList<Vertex> vertices) {
        this.vertices = vertices;
    }

    public void addVertex(Vertex vertex){
        vertices.add(vertex);
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
