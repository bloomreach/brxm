/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper of a {@link ContentType} object to use in Runtime Bean Generation.
 */
public class HippoContentBean {

    private static final Logger log = LoggerFactory.getLogger(HippoContentBean.class);

    public static final Set<String> ACCEPTED_PROPERTIES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("hippotaxonomy:keys", "relateddocs:reldoc", "hippostd:tags")));
    private static final Pattern PREFIX_SPLITTER = Pattern.compile(":");
    private static final Pattern COMMA_SPLITTER = Pattern.compile(",");
    private final ContentType contentType;
    private final String prefix;
    private final String name;
    private final List<HippoContentProperty> properties = new ArrayList<>();
    private final List<HippoContentChildNode> children = new ArrayList<>();
    private final Set<String> superTypes = new HashSet<>();

    public HippoContentBean(final ContentType contentType) {
        this.contentType = contentType;

        final String originalName = contentType.getName();
        name = extractName(originalName);
        if (name.indexOf(':') != -1) {
            final String[] fullName = PREFIX_SPLITTER.split(name);
            this.prefix = fullName[0];
        } else {
            this.prefix = null;
        }
        processProperties();
        processSubNodes();
        processSupertypes();
    }

    private String extractName(final String originalName) {
        String myName = originalName;
        if (originalName.indexOf(',') != -1) {
            final String[] names = COMMA_SPLITTER.split(originalName);
            myName = names[0];
        }
        return myName;
    }

    private void processSupertypes() {
        if (name.endsWith("basedocument")) {
            addSuperType(HippoNodeType.NT_DOCUMENT);
        }
        boolean documentType = false;
        boolean compoundType = false;
        final SortedSet<String> mySupertypes = contentType.getSuperTypes();
        for (String mySupertype : mySupertypes) {
            if (mySupertype.equals(HippoNodeType.NT_DOCUMENT)) {
                documentType = true;
            } else if (mySupertype.equals(HippoNodeType.NT_COMPOUND)) {
                compoundType = true;
            }
            if (mySupertype.startsWith(prefix)) {
                addSuperType(mySupertype);
            }
        }
        if (superTypes.size() == 0) {
            if (documentType) {
                addSuperType(HippoNodeType.NT_DOCUMENT);
            } else if (compoundType) {
                addSuperType(HippoNodeType.NT_COMPOUND);
            } else {
                log.warn("Unknown supertype for {}", this);
            }
        }
    }

    private void processSubNodes() {
        final Map<String, ContentTypeChild> myChildren = contentType.getChildren();
        for (Map.Entry<String, ContentTypeChild> entry : myChildren.entrySet()) {
            final String key = entry.getKey();
            if (key.startsWith(prefix)) {
                final ContentTypeChild value = entry.getValue();
                addChild(new HippoContentChildNode(value));
            }
        }
    }

    private void processProperties() {
        final Map<String, ContentTypeProperty> myProperties = contentType.getProperties();
        for (Map.Entry<String, ContentTypeProperty> entry : myProperties.entrySet()) {
            final String key = entry.getKey();
            if (key.startsWith(prefix)) {
                addProperty(new HippoContentProperty(entry.getValue()));
            } else if (ACCEPTED_PROPERTIES.contains(key)) {
                addProperty(new HippoContentProperty(entry.getValue()));
            }

        }
    }

    public boolean hasProperty(final String name) {
        for (HippoContentProperty property : properties) {
            if (property.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public void addChild(final HippoContentChildNode child) {
        children.add(child);
    }

    public void addSuperType(final String superType) {
        superTypes.add(superType);
    }

    public void addProperty(final HippoContentProperty property) {
        properties.add(property);
    }

    public List<HippoContentProperty> getProperties() {
        return properties;
    }

    public List<HippoContentChildNode> getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HippoContentBean{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", properties=").append(properties);
        sb.append(", children=").append(children);
        sb.append('}');
        return sb.toString();
    }
}
