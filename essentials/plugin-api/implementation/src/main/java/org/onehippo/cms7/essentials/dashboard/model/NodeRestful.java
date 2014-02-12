/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.dashboard.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.rest.Restful;

import com.sun.corba.se.impl.orbutil.ObjectWriter;

/**
 * @version "$Id$"
 */
@XmlRootElement(name = "node")
public class NodeRestful implements Restful {

    private static final long serialVersionUID = 1L;
    private List<PropertyRestful<?>> properties;
    private List<NodeRestful> nodes;
    /**
     * flag which indicates node is in "loaded" state (e.g. all data is retrieved)
     * so no additional requests needs to be done, unless explicitly requested
     */
    private boolean loaded;
    /**
     * Indicates node is wrapper node, which means node does
     * not exists but wraps a result (or list of results.)
     */
    private boolean wrapper;

    /**
     * indicates if we should load more than one node level
     */
    private int depth;
    private String name;
    private String path;

    public NodeRestful(final String name, final String path) {
        this.name = name;
        this.path = path;
    }

    public NodeRestful(final boolean wrapper) {
        this.wrapper = wrapper;
    }

    public NodeRestful() {
    }

    public void addProperty(final PropertyRestful<?> property) {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        properties.add(property);
    }


    public void addNode(final NodeRestful node) {
        if (nodes == null) {
            nodes = new ArrayList<>();
        }
        nodes.add(node);
    }

    public List<PropertyRestful<?>> getProperties() {
        return properties;
    }

    public void setProperties(final List<PropertyRestful<?>> properties) {
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    public List<NodeRestful> getNodes() {
        return nodes;
    }

    public void setNodes(final List<NodeRestful> nodes) {
        this.nodes = nodes;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(final boolean loaded) {
        this.loaded = loaded;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(final int depth) {
        this.depth = depth;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NodeRestful{");
        sb.append("properties=").append(properties);
        sb.append(", nodes=").append(nodes);
        sb.append(", loaded=").append(loaded);
        sb.append(", wrapper=").append(wrapper);
        sb.append(", depth=").append(depth);
        sb.append(", name='").append(name).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public boolean isWrapper() {
        return wrapper;
    }

    public void setWrapper(final boolean wrapper) {
        this.wrapper = wrapper;
    }


}
