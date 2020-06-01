/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model.treepicker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTreePickerRepresentation {

    private static final Logger log = LoggerFactory.getLogger(AbstractTreePickerRepresentation.class);

    public static final TreePickerRepresentationComparator comparator = new TreePickerRepresentationComparator();

    public enum PickerType {
        DOCUMENTS("documents"),
        PAGES("pages");

        private final String name;

        PickerType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static PickerType fromName(String name) {
            if ("pages".equals(name)) {
                return PickerType.PAGES;
            } else {
                return PickerType.DOCUMENTS;
            }
        }
    }

    public enum Type {
        FOLDER("folder"),
        DOCUMENT("document"),
        PAGE("page");
        private final String name;

        Type(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Type fromName(String name) {
            if ("page".equals(name)) {
                return Type.PAGE;
            } else if ("document".equals(name)) {
                return Type.DOCUMENT;
            } else {
                return Type.FOLDER;
            }
        }
    }

    private PickerType pickerType = PickerType.DOCUMENTS;
    private String id;
    private String nodeName;
    private String displayName;
    private String nodePath;
    private String pathInfo;
    private boolean selectable;
    private boolean selected;
    private boolean collapsed = true;
    private boolean leaf;
    private Type type = Type.FOLDER;
    private String state;

    private boolean expandable;
    private List<AbstractTreePickerRepresentation> items = new ArrayList<>();



    public String getPickerType() {
        return pickerType.getName();
    }

    public void setPickerType(final String pickerTypeName) {
        this.pickerType = PickerType.fromName(pickerTypeName);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(final String nodeName) {
        this.nodeName = nodeName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public String getNodePath() {
        return nodePath;
    }

    public void setNodePath(final String nodePath) {
        this.nodePath = nodePath;
    }

    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(final String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(final boolean collapsed) {
        this.collapsed = collapsed;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public void setLeaf(final boolean leaf) {
        this.leaf = leaf;
    }

    public void setSelectable(final boolean selectable) {
        this.selectable = selectable;
    }

    public String getType() {
        return type.getName();
    }

    public void setType(final String type) {
        this.type = Type.fromName(type);
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public boolean isExpandable() {
        return expandable;
    }

    public void setExpandable(final boolean expandable) {
        this.expandable = expandable;
    }

    public List<AbstractTreePickerRepresentation> getItems() {
        return items;
    }

    public void setItems(final List<AbstractTreePickerRepresentation> items) {
        this.items = items;
    }

    public static class TreePickerRepresentationComparator implements Comparator<AbstractTreePickerRepresentation> {
        @Override
        public int compare(final AbstractTreePickerRepresentation o1, final AbstractTreePickerRepresentation o2) {
            if (o1.type == Type.FOLDER) {
                if (o2.type != Type.FOLDER) {
                    // folders are ordered first
                    return -1;
                }
            }
            if (o2.type == Type.FOLDER) {
                if (o1.type != Type.FOLDER) {
                    // folders are ordered first
                    return 1;
                }
            }
            // both are a folder or both are a document. Return lexical sorting on displayname
            return o1.getDisplayName().compareToIgnoreCase(o2.getDisplayName());
        }
    }


}
