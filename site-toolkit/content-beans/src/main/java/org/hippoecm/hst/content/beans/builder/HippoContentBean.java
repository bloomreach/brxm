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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoCompound;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.content.beans.standard.HippoGalleryImageSet;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.cms7.services.contenttype.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper of a {@link ContentType} object to use in Runtime Bean Generation.
 */
public class HippoContentBean {

    private static final Logger log = LoggerFactory.getLogger(HippoContentBean.class);
    private static final String GALLERY_IMAGESET_NODETYPE = "hippogallery:imageset";
    private final ContentType contentType;
    private final String namespace;
    private final String documentType;
    private final List<HippoContentProperty> properties = new ArrayList<>();
    private final List<HippoContentChildNode> children = new ArrayList<>();
    private final List<String> allowedNamespaces = new ArrayList<>();
    private String parentDocumentType;
    private Class<? extends HippoBean> parentBean;
    private boolean parentReloaded = false;

    public HippoContentBean(final String documentType, final ContentType contentType) {
        this(documentType, null, contentType);
    }

    public HippoContentBean(final String documentType, final Class<? extends HippoBean> parentBean, final ContentType contentType) {
        this.contentType = contentType;
        this.documentType = documentType;
        this.namespace = StringUtils.substringBefore(documentType, ":");

        if (contentType == null) {
            return;
        }

        if (parentBean == null) {
            processSuperTypes();
        } else {
            this.parentBean = parentBean;
        }

        processAllowedNamespaces();
        processProperties();
        processChildNodes();
    }

    public void setParentBean(final Class<? extends HippoBean> parentBean) {
        this.parentBean = parentBean;
    }

    /**
     * The Runtime Bean of this class should be regenareted regardless of other criterias. 
     */
    public void forceGeneration() {
        this.parentReloaded = true;
    }

    /**
     * Allowed namespaces stores the list of namespaces that the dynamic bean generation
     * should create the getters for their properties/childs. If a contentType is an aggregatedType,
     * then getter methods should be created for the fields of all aggregated document types.
     */
    private void processAllowedNamespaces() {
        if (CollectionUtils.isEmpty(contentType.getAggregatedTypes())) {
            allowedNamespaces.add(namespace);
        } else {
            allowedNamespaces.addAll(contentType.getAggregatedTypes().stream().map(type -> StringUtils.substringBefore(type, ":"))
                    .collect(Collectors.toList()));
        }
    }

    private void processSuperTypes() {
        final Set<String> superTypes = contentType.getSuperTypes()
                    .stream()
                    .filter(type -> namespace.equals(getNamespace(type)))
                    .collect(Collectors.toSet());

        if (!superTypes.isEmpty()) {
            // if document type is created from other custom document types, use them as parent document types
            log.trace("{} has supertypes {}.", documentType, superTypes);
            assignParentDocumentType(superTypes);
        } else if (contentType.getSuperTypes().contains(HippoNodeType.NT_DOCUMENT)) {
            // if document type has HippoNodeType.NT_DOCUMENT as super type, then mark this document as HippoDocument
            parentDocumentType = HippoNodeType.NT_DOCUMENT;
            parentBean = HippoDocument.class;
            log.trace("{} is a document type.", documentType);
        } else if (contentType.getSuperTypes().contains(HippoNodeType.NT_COMPOUND)) {
            // if document type has HippoNodeType.NT_COMPOUND as super type, then mark this document as HippoCompound
            parentDocumentType = HippoNodeType.NT_COMPOUND;
            parentBean = HippoCompound.class;
            log.trace("{} is a compound type.", documentType);
        } else if (contentType.getSuperTypes().contains(GALLERY_IMAGESET_NODETYPE)) {
            // if document type has hippogallery:imageset as super type, then mark this document as hippogallery:imageset
            parentDocumentType = GALLERY_IMAGESET_NODETYPE;
            parentBean = HippoGalleryImageSet.class;
            log.trace("{} is gallery image set.", documentType);
        } else {
            log.error("{} is an unknown document type with supertypes {}.", documentType, contentType.getSuperTypes());
        }
    }

    private void assignParentDocumentType(final Set<String> superTypes) {
        final String baseDocumentName = namespace + ":basedocument";

        // If there is a custom document type other than basedocument in project namespace, use that.
        // Otherwise, use basedocument as the parent document type.
        parentDocumentType = superTypes
                    .stream()
                    .filter(superType -> !superType.equals(baseDocumentName))
                    .findFirst()
                    .orElse(baseDocumentName);
    }

    private void processChildNodes() {
        if (MapUtils.isEmpty(contentType.getChildren())) {
            return;
        }

        contentType.getChildren().entrySet()
            .stream()
            .filter(entry -> allowedNamespaces.contains(getNamespace(entry.getKey())))
            .forEach(entry -> children.add(new HippoContentChildNode(entry.getKey(), entry.getValue())));
    }

    private void processProperties() {
        if (MapUtils.isEmpty(contentType.getProperties())) {
            return;
        }

        contentType.getProperties().entrySet()
            .stream()
            .filter(entry -> allowedNamespaces.contains(getNamespace(entry.getKey())))
            .forEach(entry -> properties.add(new HippoContentProperty(entry.getKey(), entry.getValue())));
    }

    private String getNamespace(final String documentType) {
        return StringUtils.substringBefore(documentType, ":");
    }

    public List<HippoContentProperty> getProperties() {
        return properties;
    }

    public List<HippoContentChildNode> getChildren() {
        return children;
    }

    public String getDocumentType() {
        return documentType;
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
        sb.append(", documentType='").append(documentType).append('\'');
        sb.append(", properties=").append(properties);
        sb.append(", children=").append(children);
        sb.append(", parentDocumentType=").append(parentDocumentType);
        sb.append(", parentBean=").append(parentBean);
        sb.append(", parentReloaded=").append(parentReloaded);
        sb.append('}');
        return sb.toString();
    }
}
