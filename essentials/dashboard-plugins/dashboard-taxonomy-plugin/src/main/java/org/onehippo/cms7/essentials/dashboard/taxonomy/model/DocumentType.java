package org.onehippo.cms7.essentials.dashboard.taxonomy.model;

import org.apache.wicket.util.io.IClusterable;

/**
 * @author Jeroen Reijn
 */
public class DocumentType implements IClusterable {

    private String path;
    private String name;
    private boolean classifiable;

    public DocumentType(final String path, final String name, final boolean classifiable) {
        this.path = path;
        this.name = name;
        this.classifiable = classifiable;
    }

    public boolean getClassifiable() {
        return classifiable;
    }

    public void setClassifiable(final boolean classifiable) {
        this.classifiable = classifiable;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "DocumentType{" +
                "path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", classifiable=" + classifiable +
                '}';
    }
}
