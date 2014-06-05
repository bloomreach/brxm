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

package org.onehippo.cms7.essentials.rest.picker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

import org.onehippo.cms7.essentials.dashboard.model.Restful;

import com.google.common.collect.ImmutableSet;


/**
 * @version "$Id$"
 */
@XmlRootElement(name = "jcrNode")
public class JcrNode implements Restful {

    private static final long serialVersionUID = 1L;
    private List<JcrProperty<?>> properties;
    public static final Set<String> SYSTEM_NODE_TYPES  = new ImmutableSet.Builder<String>()
            .add("rep:system")
            .build();
    private List<JcrNode> items;

    /**
     * flag which indicates node is in "loaded" state (e.g. all data is retrieved)
     * so no additional requests needs to be done, unless explicitly requested
     */
    private boolean loaded;

    /**
     * indicates if we should load more than one node level
     */
    private int depth;
    private String name;
    private String path;
    private String id;
    private String title;


    public JcrNode(final String name, final String path) {
        this.name = name;
        this.path = path;
        this.id = path;
        this.title = name;

    }

    public JcrNode() {
    }


    public String getTitle() {
        if (title == null) {
            return name;
        }
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getId() {
        if (id == null) {
            return path;
        }
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void addProperty(final JcrProperty<?> property) {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        properties.add(property);
    }


    public void addNode(final JcrNode node) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(node);
    }

    public List<JcrProperty<?>> getProperties() {
        return properties;
    }

    public void setProperties(final List<JcrProperty<?>> properties) {
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

    public List<JcrNode> getItems() {
        if (items == null) {
            return Collections.emptyList();
        }
        return items;
    }

    public void setItems(final List<JcrNode> items) {
        this.items = items;
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
        final StringBuilder sb = new StringBuilder("JcrNode{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", path='").append(path).append('\'');
        sb.append('}');
        return sb.toString();
    }


}
