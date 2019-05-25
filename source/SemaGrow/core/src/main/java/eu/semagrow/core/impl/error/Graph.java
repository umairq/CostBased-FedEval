package eu.semagrow.core.impl.error;
public class Graph {

    String name;

    public Graph(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Graph)
            return this.name.equals(((Graph) object).name);
        return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    public String toString() {
        return this.name;
    }

    public String getNamespaceURL() {
        // TODO: implement proper hamespace handling
        return this.name.replaceFirst("http://lodse.west.uni-koblenz.de:8080/openrdf-sesame/repositories/", "west:");
    }

}