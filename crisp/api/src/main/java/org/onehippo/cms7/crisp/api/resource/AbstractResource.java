package org.onehippo.cms7.crisp.api.resource;

public abstract class AbstractResource implements Resource {

    private static final long serialVersionUID = 1L;

    private final Resource parent;
    private final String resourceType;
    private final String name;
    private final String path;

    public AbstractResource(String resourceType) {
        this(resourceType, null);
    }

    public AbstractResource(String resourceType, String name) {
        this(null, resourceType, name);
    }

    public AbstractResource(Resource parent, String resourceType, String name) {
        this.parent = parent;
        this.resourceType = resourceType;
        this.name = name;

        if (parent == null) {
            path = "/";
        } else {
            if (name == null) {
                throw new IllegalArgumentException("name must not be null.");
            }
            if ("/".equals(parent.getPath())) {
                path = "/" + name;
            } else {
                path = parent.getPath() + "/" + name;
            }
        }
    }

    @Override
    public String gerResourceType() {
        return resourceType;
    }

    @Override
    public boolean isResourceType(String resourceType) {
        return this.resourceType != null && this.resourceType.equals(resourceType);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Resource getParent() {
        return parent;
    }

}
