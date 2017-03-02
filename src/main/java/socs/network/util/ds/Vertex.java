package socs.network.util.ds;

import java.util.Objects;

/**
 * Created by ericschaal on 2017-03-01.
 */
public class Vertex {
    final private String id;
    final private String name;


    public Vertex(String id, String name) {
        this.id = id;
        this.name = name;
    }
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if (Objects.isNull(obj) || !(obj instanceof Vertex))
            return false;
        if (this == obj)
            return true;

        Vertex other = (Vertex) obj;

        return (other.getId().equals(this.getId()));

    }

    @Override
    public String toString() {
        return name;
    }

}