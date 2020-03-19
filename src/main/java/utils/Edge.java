package utils;

import com.google.gson.annotations.SerializedName;

public class Edge {

    @SerializedName("source")
    String source;

    @SerializedName("target")
    String target;


    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }
}

