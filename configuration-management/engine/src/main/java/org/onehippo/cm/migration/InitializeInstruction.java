/*
 *  Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cm.migration;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;

import javax.jcr.PropertyType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitializeInstruction {

    public enum Type {
        NAMESPACE,
        NODETYPESRESOURCE,
        //      NODETYPES, // not supported from hippoecm-extension.xml, only internally
        CONTENTDELETE,
        CONTENTRESOURCE,
        CONTENTPROPDELETE,
        //        CONTENT, // no longer in use nor documented
        CONTENTPROPSET,
        CONTENTPROPADD,
        WEBFILEBUNDLE,
        RESOURCEBUNDLES;

        static {
            // configure combinable types, which only can be done statically with enum
            NAMESPACE.combinableWith = NODETYPESRESOURCE;
            NODETYPESRESOURCE.combinableWith = NAMESPACE;
            CONTENTDELETE.combinableWith = CONTENTRESOURCE;
            CONTENTRESOURCE.combinableWith = CONTENTDELETE;
        }

        public static Type get(final String propertyName) {
            for (Type type : values()) {
                if (type.getPropertyName().equals(propertyName)) {
                    return type;
                }
            }
            return null;
        }

        private final String propertyName;
        private Type combinableWith;

        Type() {
            this.propertyName = "hippo:" + name().toLowerCase();
        }

        public String getPropertyName() {
            return propertyName;
        }

        public Type getCombinableWith() {
            return combinableWith;
        }
    }

    public static final Comparator<InitializeInstruction> COMPARATOR = (o1, o2) -> {

        final int sequenceCompare = o1.getSequence().compareTo(o2.getSequence());

        if (o1.getType() == Type.NAMESPACE && o2.getType() == Type.NAMESPACE) {
            // namespaces ordered by seq, name
            return sequenceCompare != 0 ? sequenceCompare : o1.getName().compareTo(o2.getName());
        }
        if (o1.getType() == Type.NAMESPACE) {
            // namespaces before anything else
            return -1;
        }
        if (o2.getType() == Type.NAMESPACE) {
            // namespaces before anything else
            return 1;
        }

        if (o1.getType() == Type.NODETYPESRESOURCE && o2.getType() == Type.NODETYPESRESOURCE) {
            // cnds ordered by seq, name
            return sequenceCompare != 0 ? sequenceCompare : o1.getName().compareTo(o2.getName());
        }
        if (o1.getType() == Type.NODETYPESRESOURCE) {
            // cnds before anything else, after namespaces
            return -1;
        }
        if (o2.getType() == Type.NODETYPESRESOURCE) {
            // cnds before anything else, after namespaces
            return 1;
        }

        // anything else, not namespace or cnd
        if (sequenceCompare != 0) {
            // order by seq
            return sequenceCompare;
        }

        if (o1 == o2.getCombinedWith() || o2 == o1.getCombinedWith()) {
            // for combined contentdelete + contentresource, ensure *that* contentdelete comes before contentresource
            return o1.getType() == Type.CONTENTDELETE ? -1 : 1;
        }

        if (o1.isDeltaMerge()) {
            if (!o2.isDeltaMerge()) {
                // delta (combine, overlay) > non-delta
                return 1;
            }
        } else {
            if (o2.isDeltaMerge()) {
                // delta (combine, overlay) > non-delta
                return -1;
            }

            // non-delta instructions

            if (o2.isDownStream(o1)) {
                // downstream after upstream
                return -1;
            }

            if (o1.isDownStream(o2)) {
                // downstream after upstream
                return 1;
            }
        }
        // either delta+delta or neither downstream of each other
        return o1.getName().compareTo(o2.getName());
    };

    static final Logger log = LoggerFactory.getLogger(Esv2Yaml.class);

    private final String name;
    private final Type type;
    private InitializeInstruction combinedWith;
    private final EsvNode instructionNode;
    private final Double sequence;
    private String definitionPath;
    private String resourcePath;
    private File resource;
    private String contentPath;
    private String sourcePath;

    protected boolean isDownStream(final InitializeInstruction other) {
        if (other.getType() != Type.CONTENTRESOURCE) {
            // only contentresource upstream can have downstream instructions
            return false;
        }
        if (getContentPath() == null) {
            // not a content instruction (at this point, must be webfilesbundle)
            return false;
        }
        switch (getType()) {
            case CONTENTDELETE:
                if (getCombinedWith() != null) {
                    // for combined contentdelete + contentresource, compare against its contentresource (like existing bootstrap does)
                    // note: combinedWith != other (use-case already handled before in comparator)
                    return getCombinedWith().isDownStream(other);
                }
                return getContentPath().equals(other.getContentPath()) || getContentPath().startsWith(other.getContentPath() + "/");
            case CONTENTRESOURCE:
                if (getContentPath().equals(other.getContentPath())) {
                    // if paths equal and upstream is delta skip, better swap positions (note: *not* considered in existing bootstrap)
                    return !other.isDeltaSkip();
                }
                return getContentPath().startsWith(other.getContentPath() + "/");
            case CONTENTPROPADD:
            case CONTENTPROPSET:
            case CONTENTPROPDELETE:
                return getContentPath().startsWith(getContentPath() + "/");
            case RESOURCEBUNDLES:
                // for now (and probably fine to keep as is) treat resourcebundles as downstream
                // existing bootstrap uses *much* more complex logic, but bottom line it likely makes no difference
                return true;
        }
        return false;
    }

    public static void parse(final EsvNode node, final List<InitializeInstruction> instructions) throws EsvParseException {
        // cater for initializeitems having 2 instructions (namespace+cnd, contentdelete+contentresource)
        InitializeInstruction first = null;
        InitializeInstruction second = null;
        for (Type type : Type.values()) {
            if (node.getProperty(type.getPropertyName()) != null) {
                if (second != null ||
                        (first != null &&
                                (first.getType().getCombinableWith() == null || first.getType().getCombinableWith() != type))) {
                    throw new EsvParseException("Unsupported initialization type: " + type.getPropertyName() +
                            " for hippo:initializationitem: " + node.getName());
                }
                InitializeInstruction instruction;
                switch (type) {
                    case CONTENTRESOURCE:
                        instruction = new SourceInitializeInstruction(node, type, first);
                        break;
                    case RESOURCEBUNDLES:
                        instruction = new ResourcebundlesInitializeInstruction(node, type, first);
                        break;
                    case CONTENTDELETE:
                    case CONTENTPROPDELETE:
                    case CONTENTPROPSET:
                    case CONTENTPROPADD:
                        instruction = new ContentInitializeInstruction(node, type, first);
                        break;
                    default:
                        instruction = new InitializeInstruction(node, type, first);
                }
                if (first != null) {
                    second = instruction;
                    // link first to second (second already linked to first in constructor call above)
                    first.setCombinedWith(second);
                } else {
                    first = instruction;
                }
            }
        }
        if (first != null) {
            instructions.add(first);
            if (second != null) {
                instructions.add(second);
            }
        } else {
            throw new EsvParseException("Unsupported hippo:initializationitem: " + node.getName());
        }
    }

    protected InitializeInstruction(final EsvNode instructionNode, final Type type,
                                    final InitializeInstruction combinedWith) throws EsvParseException {
        this.instructionNode = instructionNode;
        this.name = instructionNode.getName();
        this.type = type;
        this.combinedWith = combinedWith;
        final String value = getPropertyValue("hippo:sequence", PropertyType.DOUBLE, false);
        this.sequence = value != null ? Double.valueOf(value) : -1.0;
    }

    protected void setCombinedWith(final InitializeInstruction combinedWith) {
        this.combinedWith = combinedWith;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public InitializeInstruction getCombinedWith() {
        return combinedWith;
    }

    public EsvNode getInstructionNode() {
        return instructionNode;
    }

    public String getDefinitionPath() {
        return definitionPath;
    }

    public void setDefinitionPath(final String definitionPath) {
        this.definitionPath = definitionPath;
    }

    public EsvProperty getTypeProperty() {
        return instructionNode.getProperty(type.getPropertyName());
    }

    public String getTypePropertyValue() throws EsvParseException {
        return getPropertyValue(getType().getPropertyName(), PropertyType.STRING, true);
    }

    public Double getSequence() {
        return sequence;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public File getResource() {
        return resource;
    }

    public void setContentPath(final String contentPath) {
        this.contentPath = "/" + normalizePath(contentPath);
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setSourcePath(final String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public boolean isDeltaMerge() {
        return false;
    }

    public boolean isDeltaSkip() {
        return false;
    }

    public void prepareResource(final File basedir, final boolean file) throws IOException, EsvParseException {
        final String path = getTypePropertyValue();
        this.resourcePath = normalizePath(path);
        this.resource = new File(basedir, resourcePath);
        if (!resource.exists() || !(file && resource.isFile())) {
            throw new EsvParseException("Resource " + path + " not found or not a " + (file ? "file" : "directory"));
        }
    }

    public String getPropertyValue(final String name, final int type, final boolean required) throws EsvParseException {
        final EsvProperty prop = instructionNode.getProperty(name);
        if (prop == null) {
            if (required) {
                throw new EsvParseException("Missing required property: " + name + " for initialization item: " + getName());
            }
            return null;
        } else {
            if (type != prop.getType()) {
                throw new EsvParseException("Property: " + name + " is not of type String for initialization item: " + getName());
            }
            if (prop.isMultiple() || prop.getValues().size() != 1) {
                throw new EsvParseException("Property: " + name + " requires a single value for initialization item: " + getName());
            }
            final String value = prop.getValues().get(0).getString();
            if (required && (value == null || value.isEmpty())) {
                throw new EsvParseException("Empty or null property value: " + name + " for initialization item: " + getName());
            }
            return value;
        }
    }

    public static String normalizePath(String path) {
        if (path == null) {
            return null;
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
