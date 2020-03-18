/*
 *  Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.content.beans.builder;

import java.util.Arrays;

import org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder;
import org.hippoecm.hst.content.beans.dynamic.DynamicBeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logic for creating a bean based on document types.
 */
public abstract class AbstractBeanBuilderService {

    private static final Logger log = LoggerFactory.getLogger(AbstractBeanBuilderService.class);

    private enum DocumentType {
        STRING("String"), //
        HTML("Html"), //
        PASSWORD("Password"), //
        TEXT("Text"), //
        DATE("Date"), //
        BOOLEAN("Boolean"), //
        LONG("Long"), //
        DOUBLE("Double"), //
        DOCBASE("Docbase"), //
        HIPPO_HTML("hippostd:html"), //
        HIPPO_IMAGELINK("hippogallerypicker:imagelink"), //
        HIPPO_MIRROR("hippo:mirror"), //
        HIPPO_IMAGE("hippogallery:image"), //
        HIPPO_RESOURCE("hippo:resource"), //
        HIPPO_COMPOUND("hippo:compound"), //
        CONTENT_BLOCKS("content:blocks"), //
        UNKNOWN("Unknown");

        private String type;

        public String getDocumentType() {
            return this.type;
        }

        DocumentType(String type) {
            this.type = type;
        }

        public static DocumentType getDocumentType(String type) {
            return Arrays.stream(DocumentType.values())
                .filter(doc -> doc.getDocumentType().equals(type))
                .findFirst()
                .orElse(DocumentType.UNKNOWN);
        }
    }

    /**
     * Generates bean method by its properties
     * 
     * @param bean {@link HippoContentBean}
     * @param builder {@link DynamicBeanBuilder}
     */
    protected void generateMethodsByProperties(HippoContentBean bean, DynamicBeanBuilder builder) {
        for (HippoContentProperty property : bean.getProperties()) {
            final String propertyName = property.getName();
            final boolean multiple = property.isMultiple();

            final String methodName = DynamicBeanUtils.createMethodName(propertyName);
            boolean hasChange = hasChange(methodName, multiple, builder);
            if (!hasChange) {
                continue;
            }

            log.trace("Adding property {} to the bean {}.", property.getName(), bean.getDocumentType());

            if (property.getType() == null) {
                log.error("Missing type for property, cannot create method {} on bean {}.", property.getName(), bean.getDocumentType());
                continue;
            }

            final DocumentType documentType = getPropertyDocumentType(property.getType(), property.getCmsType());

            switch (documentType) {
            case STRING:
            case HTML:
            case PASSWORD:
            case TEXT:
                addBeanMethodString(propertyName, methodName, multiple, builder);
                break;
            case DATE:
                addBeanMethodCalendar(propertyName, methodName, multiple, builder);
                break;
            case BOOLEAN:
                addBeanMethodBoolean(propertyName, methodName, multiple, builder);
                break;
            case LONG:
                addBeanMethodLong(propertyName, methodName, multiple, builder);
                break;
            case DOUBLE:
                addBeanMethodDouble(propertyName, methodName, multiple, builder);
                break;
            case DOCBASE:
                addBeanMethodDocbase(propertyName, methodName, multiple, builder);
                break;
            default:
                addCustomPropertyType(propertyName, methodName, multiple, bean.getDocumentType(), property.getCmsType(), builder);
                break;
            }
        }
    }

    /**
     * Generated bean methods by its child nodes
     * 
     * @param bean {@link HippoContentBean}
     * @param builder {@link DynamicBeanBuilder}
     */
    protected void generateMethodsByChildNodes(final HippoContentBean bean, final DynamicBeanBuilder builder) {
        for (final HippoContentChildNode childNode : bean.getChildren()) {
            final String propertyName = childNode.getName();
            final boolean multiple = childNode.isMultiple();

            final String methodName = DynamicBeanUtils.createMethodName(propertyName);
            final boolean hasChange = hasChange(methodName, multiple, builder);
            if (!hasChange) {
                continue;
            }

            log.trace("Adding property {} to the bean {}.", childNode.getName(), bean.getDocumentType());

            if (childNode.getType() == null) {
                log.error("Missing type for node, cannot create method {} on bean {}.", childNode.getName(), bean.getDocumentType());
                continue;
            }

            final DocumentType documentType = getChildNodeDocumentType(childNode.getType(), childNode.hasContentBlocks());

            switch (documentType) {
            case HIPPO_HTML:
                addBeanMethodHippoHtml(propertyName, methodName, multiple, builder);
                break;
            case HIPPO_MIRROR:
                addBeanMethodHippoMirror(propertyName, methodName, multiple, builder);
                break;
            case HIPPO_IMAGELINK:
                addBeanMethodImageLink(propertyName, methodName, multiple, builder);
                break;
            case HIPPO_IMAGE:
                addBeanMethodHippoImage(propertyName, methodName, multiple, builder);
                break;
            case HIPPO_RESOURCE:
                addBeanMethodHippoResource(propertyName, methodName, multiple, builder);
                break;
            case CONTENT_BLOCKS:
                addBeanMethodContentBlocks(propertyName, methodName, multiple, builder);
                break;
            case HIPPO_COMPOUND:
                addBeanMethodCompoundType(propertyName, methodName, multiple, childNode.getName(), builder);
                break;
            default:
                addCustomNodeType(propertyName, methodName, multiple, childNode.getType(), builder);
                break;
            }
        }
    }

    /**
     * Gets the corresponding document type enum value of a given document type by checking
     * a possible docbase definition
     * 
     * @param type of the document
     * @param cmsType itemType of the {@link org.onehippo.cms7.services.contenttype.ContentTypeProperty}
     * @return the corresponding document type
     */
    private DocumentType getPropertyDocumentType(final String type, final String cmsType) {
        DocumentType documentType = DocumentType.getDocumentType(type);

        // if a document type is a string type, then the cms/item type check
        // should be made to figure out the actual type of the document
        if (documentType == DocumentType.STRING) {
            documentType = DocumentType.getDocumentType(cmsType);
        }

        return documentType;
    }

    /**
     * Gets the corresponding document type enum value of a given document type by checking
     * a possible content block definition
     * 
     * @param type of the document
     * @param hasContentBlocks whether a contentType has any content blocks or not
     * @return the corresponding document type
     */
    private DocumentType getChildNodeDocumentType(final String type, final boolean hasContentBlocks) {
        final DocumentType documentType = DocumentType.getDocumentType(type);

        // if a document type doesn't match with any predefined document type, then
        // a content block definition check must be made to figure out whether the
        // document type is a content block
        if (DocumentType.UNKNOWN == documentType) {
            if (hasContentBlocks) {
                return DocumentType.CONTENT_BLOCKS;
            }
        }

        return documentType;
    }

    /**
     * Checks whether a property/node should be generated as a method for the bean or not
     * 
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     * @return true if the property/node has to be generated as method
     */
    protected abstract boolean hasChange(String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link String} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodString(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link java.util.Calendar} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodCalendar(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link Boolean} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodBoolean(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link Long} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodLong(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link Double} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodDouble(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoBean} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodDocbase(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a custom implementation if there isn't any matching object type for the property
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param documentType of the document
     * @param cmsType cms type of the document property
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addCustomPropertyType(String propertyName, String methodName, boolean multiple, String documentType, String cmsType, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoHtmlBean} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodHippoHtml(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodImageLink(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoBean} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodHippoMirror(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodHippoImage(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoResourceBean} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodHippoResource(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.onehippo.cms7.essentials.plugins.contentblocks.model.ContentBlocksField} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodContentBlocks(String propertyName, String methodName, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoCompound} object type
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param type of the node
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodCompoundType(String propertyName, String methodName, boolean multiple, String type, DynamicBeanBuilder builder);

    /**
     * Adds a custom implementation if there isn't any matching object type for the child node
     * 
     * @param propertyName of the property type
     * @param methodName of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param type of the node
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addCustomNodeType(String propertyName, String methodName, boolean multiple, String type, DynamicBeanBuilder builder);

}
