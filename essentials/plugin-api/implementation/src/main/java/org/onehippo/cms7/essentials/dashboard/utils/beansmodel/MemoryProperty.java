package org.onehippo.cms7.essentials.dashboard.utils.beansmodel;

/**
 * @version "$Id: MemoryProperty.java 172944 2013-08-06 16:37:37Z mmilicevic $"
 */
public class MemoryProperty {


    private String name;
    private String type;
    private boolean multiple;
    private final MemoryBean parent;

    public MemoryProperty(final MemoryBean parent) {
        this.parent = parent;
    }

    public MemoryBean getParent() {
        return parent;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public void setMultiple(final boolean multiple) {
        this.multiple = multiple;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }


    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MemoryProperty{");
        sb.append("name='").append(name).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", multiple=").append(multiple);
        sb.append(", parent=").append(parent);
        sb.append('}');
        return sb.toString();
    }
}
