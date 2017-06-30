/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.onehippo.cms7.crisp.api.resource;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract {@link Resource} representation.
 */
public abstract class AbstractResource implements Resource {

    private static final long serialVersionUID = 1L;

    /**
     * Array index notation regex pattern.
     */
    private static final Pattern INDEXED_NAME_PATTERN = Pattern.compile("^(.+)\\[(\\d+)\\]$");

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
    public String getResourceType() {
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
    public boolean isAnyChildContained() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isArray() {
        return false;
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
    public ResourceCollection getChildren() {
        return getChildren(0L, -1L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(String relPath) {
        if (!relPath.contains("/")) {
            return getValueMap().get(relPath);
        }

        String [] paths = relPath.split("/");

        Resource subject = this;
        String pathSegment;
        String name;
        int valueIndex;
        Matcher matcher;
        Object arrayContainer;
        Object value = null;
        ResourceCollection childCollection;

        for (int i = 0; i < paths.length; i++) {
            pathSegment = paths[i];
            matcher = INDEXED_NAME_PATTERN.matcher(pathSegment);

            if (matcher.matches()) {
                name = matcher.group(1);
                valueIndex = Integer.parseInt(matcher.group(2));

                if (valueIndex < 1) {
                    throw new IllegalArgumentException("Index must be greater than zero.");
                }

                arrayContainer = subject.getValueMap().get(name);

                if (arrayContainer instanceof Resource && ((Resource) arrayContainer).isArray()) {
                    childCollection = ((Resource) arrayContainer).getChildren();
                    value = childCollection.get(valueIndex - 1);
                } else {
                    value = null;
                }
            } else {
                name = pathSegment;
                value = subject.getValueMap().get(name);
            }

            if (value instanceof Resource) {
                subject = (Resource) value;
            } else if (i < paths.length - 1){
                value = null;
            }

            if (value == null) {
                break;
            }
        }

        return value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getValue(String relPath, Class<T> type) {
        final Object value = getValue(relPath);

        if (value != null && type != null) {
            if (value instanceof String) {
                if (type == Integer.class) {
                    return (T) Integer.valueOf((String) value);
                } else if (type == Long.class) {
                    return (T) Long.valueOf((String) value);
                } else if (type == Double.class) {
                    return (T) Double.valueOf((String) value);
                } else if (type == Boolean.class) {
                    return (T) Boolean.valueOf((String) value);
                } else if (type == BigDecimal.class) {
                    return (T) new BigDecimal((String) value);
                }
            }

            if (!type.isAssignableFrom(value.getClass())) {
                throw new IllegalArgumentException("The type doesn't match with the value type: " + value.getClass());
            }
        }

        return (T) value;
    }

    /**
     * {@inheritDoc}
     * <P>
     * This default implementation is equivalent to <code>getValueMap().get("")</code> if not overriden.
     * </P>
     */
    @Override
    public Object getDefaultValue() {
        return getValueMap().get("");
    }

    /**
     * {@inheritDoc}
     * <P>
     * This default implementation is equivalent to <code>getValueMap().get("", type)</code> if not overriden.
     * </P>
     */
    @Override
    public <T> T getDefaultValue(Class<T> type) {
        return getValueMap().get("", type);
    }

}
