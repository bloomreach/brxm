/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 */
package com.onehippo.cms7.crisp.api.resource;

import java.util.Iterator;

/**
 * Abstract {@link Resource} representation.
 */
public abstract class AbstractResource implements Resource {

    private static final long serialVersionUID = 1L;

    /**
     * Parent resource representation.
     */
    private final Resource parent;

    /**
     * Resource type name.
     */
    private final String resourceType;

    /**
     * Resource name.
     */
    private final String name;

    /**
     * Resource path.
     */
    private final String path;

    /**
     * Construct resource representation by using resource type name.
     * @param resourceType resource type name
     */
    public AbstractResource(String resourceType) {
        this(resourceType, null);
    }

    /**
     * Constructs resource representation by using resource type name and resource name.
     * @param resourceType resource type name
     * @param name resource name
     */
    public AbstractResource(String resourceType, String name) {
        this(null, resourceType, name);
    }

    /**
     * Constructs resource representation by using parent resoruce representation, resource type name and resource name.
     * @param parent parent resource representation
     * @param resourceType resource type name
     * @param name resource name
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String gerResourceType() {
        return resourceType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResourceType(String resourceType) {
        return this.resourceType != null && this.resourceType.equals(resourceType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Resource getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getChildCount() {
        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<Resource> getChildIterator() {
        return getChildIterator(0L, -1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<Resource> getChildren() {
        return getChildren(0L, -1L);
    }

}
