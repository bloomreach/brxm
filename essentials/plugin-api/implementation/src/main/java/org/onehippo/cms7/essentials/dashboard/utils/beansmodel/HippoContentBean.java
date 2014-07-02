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

package org.onehippo.cms7.essentials.dashboard.utils.beansmodel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.onehippo.cms7.services.contenttype.ContentTypeChild;
import org.onehippo.cms7.services.contenttype.ContentTypeProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

/**
 * @version "$Id$"
 */
public class HippoContentBean {

    private static final Logger log = LoggerFactory.getLogger(HippoContentBean.class);

    public static final Set<String> ACCEPTED_PROPERTIES = new ImmutableSet.Builder<String>()
            .add("hippotaxonomy:keys")
            .add("relateddocs:reldoc")
            .add("hippostd:tags")
            .build();
    private static final Pattern PREFIX_SPLITTER = Pattern.compile(":");
    private static final Pattern COMMA_SPLITTER = Pattern.compile(",");
    private final ContentType contentType;
    private final PluginContext context;
    private final String prefix;
    private final String shortName;
    private final String name;
    private final List<HippoContentProperty> properties = new ArrayList<>();
    private final List<HippoContentChildNode> children = new ArrayList<>();
    private final Set<String> superTypes = new HashSet<>();
    private static final String BASE_TYPE = "hippo:document";
    private static final String BASE_COMPOUND_TYPE = "hippo:compound";


    public HippoContentBean(final PluginContext context, final ContentType contentType) {
        this.context = context;
        this.contentType = contentType;

        final String originalName = contentType.getName();
        if( originalName.indexOf(',') !=-1){
            name = COMMA_SPLITTER.split(originalName)[0];
        }else{
            name = originalName;
        }

        if (name.indexOf(':') != -1) {
            final String[] fullName = PREFIX_SPLITTER.split(name);
            this.shortName = fullName[1];
            this.prefix = fullName[0];
        } else {
            this.shortName = name;
            this.prefix = null;
        }
        processProperties();
        processSubNodes();
        processSupertypes();
    }

    private void processSupertypes() {
        if (name.endsWith("basedocument")) {
            addSuperType(BASE_TYPE);
        }
        boolean documentType = false;
        boolean compoundType = false;
        final SortedSet<String> mySupertypes = contentType.getSuperTypes();
        for (String mySupertype : mySupertypes) {
            if (mySupertype.equals(BASE_TYPE)) {
                documentType = true;
            } else if (mySupertype.equals(BASE_COMPOUND_TYPE)) {
                compoundType = true;
            }
            if (mySupertype.startsWith(prefix)) {
                addSuperType(mySupertype);
            }
        }
        if (superTypes.size() == 0) {
            if (documentType) {
                addSuperType(BASE_TYPE);
            } else if (compoundType) {
                addSuperType(BASE_COMPOUND_TYPE);
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

    public ContentType getContentType() {
        return contentType;
    }

    public String getPrefix() {
        return prefix;
    }

    public List<HippoContentChildNode> getChildren() {
        return children;
    }

    public Set<String> getSuperTypes() {
        return superTypes;
    }

    public String getShortName() {
        return shortName;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HippoContentBean{");
        sb.append(", context=").append(context);
        sb.append(", prefix='").append(prefix).append('\'');
        sb.append(", shortName='").append(shortName).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", properties=").append(properties);
        sb.append(", children=").append(children);
        sb.append(", superTypes=").append(superTypes);
        sb.append('}');
        return sb.toString();
    }
}
