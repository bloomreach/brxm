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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
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
    private static final String GALLERY_IMAGESET_NODETYPE = "hippogallery:imageset";
    public static final Set<String> ACCEPTED_PROPERTIES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("hippotaxonomy:keys", "relateddocs:reldoc", "hippostd:tags")));
    private final ContentType contentType;
    private final String prefix;
    private final String name;
    private final List<HippoContentProperty> properties = new ArrayList<>();
    private final List<HippoContentChildNode> children = new ArrayList<>();
    private final Set<String> superTypes = new HashSet<>();
    private String parentDocumentType;
    private Class<? extends HippoBean> parentBean;
    private boolean parentReloaded = false;

    public HippoContentBean(final String name, final ContentType contentType) {
        this(name, null, contentType);
    }

    public HippoContentBean(final String name, final Class<? extends HippoBean> parentBean, final ContentType contentType) {
        this.contentType = contentType;
        this.name = name;
        this.prefix = StringUtils.substringBefore(name, ":");

        if (contentType == null) {
            return;
        }

        if (parentBean == null) {
            processSuperTypes();
        } else {
            this.parentBean = parentBean;
        }

        processProperties();
        processSubNodes();
    }

    public void setParentBean(final Class<? extends HippoBean> parentBean) {
        this.parentBean = parentBean;
    }

    /**
     * The Runtime Bean of this class should be regenareted regardless of other criteria. 
     */
    public void forceGeneration() {
        this.parentReloaded = true;
    }

    private void processSuperTypes() {
        final Set<String> superTypes = contentType.getSuperTypes()
                    .stream()
                    .filter(type -> type.startsWith(prefix))
                    .collect(Collectors.toSet());

        if (!superTypes.isEmpty()) {
            // if document type is created from other custom document types, use them as parent document types
            log.trace("{} has supertypes {}.", name, superTypes);
            processParentBean(superTypes);
        } else if (contentType.getSuperTypes().contains(HippoNodeType.NT_DOCUMENT)) {
            // if document type has HippoNodeType.NT_DOCUMENT as super type, then mark this document as HippoDocument
            parentDocumentType = HippoNodeType.NT_DOCUMENT;
            parentBean = HippoDocument.class;
            log.trace("{} is a document type.", name);
        } else if (contentType.getSuperTypes().contains(HippoNodeType.NT_COMPOUND)) {
            // if document type has HippoNodeType.NT_COMPOUND as super type, then mark this document as HippoCompound
            parentDocumentType = HippoNodeType.NT_COMPOUND;
            parentBean = HippoCompound.class;
            log.trace("{} is a compound type.", name);
        } else { 
            log.error("{} is an unknown document type with supertypes {}.", name, contentType.getSuperTypes());
        }
    }

    private void processParentBean(final Set<String> superTypes) {
        final String baseDocumentName = prefix + ":basedocument";

        // if there is a custom document type other than basedocument in project namespace, use that
        parentDocumentType = superTypes
                    .stream()
                    .filter(superType -> !superType.equals(baseDocumentName))
                    .findFirst()
                    .orElse(null);

        if (parentDocumentType == null) {
            // if there is a basedocument in project namespace, use that
            parentDocumentType = superTypes
                    .stream()
                    .filter(superType -> superType.equals(baseDocumentName))
                    .findFirst()
                    .orElse(null);
        }

        // if the document type is extended from hippogallery:imageset, use that as parent
        if (parentDocumentType == null && superTypes.stream().anyMatch(superType -> superType.equals(GALLERY_IMAGESET_NODETYPE))) {
            parentDocumentType = GALLERY_IMAGESET_NODETYPE;
            parentBean = HippoGalleryImageSet.class;
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

    public String getParentDocumentType() {
        return parentDocumentType;
    }

    public Class<? extends HippoBean> getParentBean() {
        return parentBean;
    }

    public boolean isParentReloaded() {
        return parentReloaded;
    }

    public boolean hasContentType() {
        return contentType != null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HippoContentBean{");
        sb.append(", name='").append(name).append('\'');
        sb.append(", properties=").append(properties);
        sb.append(", children=").append(children);
        sb.append(", parentDocumentType=").append(parentDocumentType);
        sb.append(", parentBean=").append(parentBean);
        sb.append(", parentReloaded=").append(parentReloaded);
        sb.append('}');
        return sb.toString();
    }
}
