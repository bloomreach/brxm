/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logic for creating a bean based on document types.
 */
public abstract class AbstractBeanBuilderService {
    private static final Logger log = LoggerFactory.getLogger(AbstractBeanBuilderService.class);
    private static final String DOCBASE = "Docbase";

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
        HIPPO_RESOURCE("hippo:resource"),
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
            final String name = property.getName();
            final boolean multiple = property.isMultiple();
            final String cmsType = property.getCmsType();

            boolean hasChange = hasChange(name, multiple, builder);
            if (!hasChange) {
                continue;
            }

            String type = property.getType();
            log.debug("processing missing property, BEAN: {}, PROPERTY: {}", bean.getName(), property.getName());

            if (type == null) {
                log.error("Missing type for property, cannot create method {}", property.getName());
                continue;
            }

            DocumentType documentType = DocumentType.getDocumentType(type);
            if (documentType == DocumentType.STRING && DOCBASE.equals(cmsType)) {
                documentType = DocumentType.DOCBASE;
            }

            switch (documentType) {
            case STRING:
            case HTML:
            case PASSWORD:
            case TEXT:
                addBeanMethodString(name, multiple, builder);
                break;
            case DATE:
                addBeanMethodCalendar(name, multiple, builder);
                break;
            case BOOLEAN:
                addBeanMethodBoolean(name, multiple, builder);
                break;
            case LONG:
                addBeanMethodLong(name, multiple, builder);
                break;
            case DOUBLE:
                addBeanMethodDouble(name, multiple, builder);
                break;
            case DOCBASE:
                addBeanMethodDocbase(name, multiple, builder);
                break;
            default:
                addCustomPropertyType(name, multiple, type, builder);
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
    protected void generateMethodsByNodes(final HippoContentBean bean, final DynamicBeanBuilder builder) {
        for (final HippoContentChildNode child : bean.getChildren()) {
            final String name = child.getName();
            final boolean multiple = child.isMultiple();

            final boolean hasChange = hasChange(name, multiple, builder);
            if (!hasChange) {
                continue;
            }

            final String type = child.getType();
            log.debug("processing missing node, BEAN: {}, CHILD: {}", bean.getName(), child.getName());

            if (type == null) {
                log.error("Missing type for node, cannot create method {}", child.getName());
                continue;
            }

            final DocumentType documentType = DocumentType.getDocumentType(type);

            switch (documentType) {
            case HIPPO_HTML:
                addBeanMethodHippoHtml(name, multiple, builder);
                break;
            case HIPPO_IMAGELINK:
                addBeanMethodImageLink(name, multiple, builder);
                break;
            case HIPPO_MIRROR:
                addBeanMethodHippoMirror(name, multiple, builder);
                break;
            case HIPPO_IMAGE:
                addBeanMethodHippoImage(name, multiple, builder);
                break;
            case HIPPO_RESOURCE:
                addBeanMethodHippoResource(name, multiple, builder);
                break;
            default:
                addCustomNodeType(name, multiple, type, builder);
                break;
            }
        }
    }

    /**
     * Checks whether a property/node should be generated as a method for the bean or not
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     * @return true if the property/node has to be generated as method
     */
    protected abstract boolean hasChange(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link String} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodString(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link java.util.Calendar} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodCalendar(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link Boolean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodBoolean(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link Long} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodLong(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link Double} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodDouble(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodDocbase(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a custom implementation if there isn't any matching object type for the property
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param type of the document property
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addCustomPropertyType(String name, boolean multiple, String type, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoHtmlBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodHippoHtml(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoGalleryImageSetBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodImageLink(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodHippoMirror(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoGalleryImageBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodHippoImage(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a method to the bean which returns {@link org.hippoecm.hst.content.beans.standard.HippoResourceBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addBeanMethodHippoResource(String name, boolean multiple, DynamicBeanBuilder builder);

    /**
     * Adds a custom implementation if there isn't any matching object type for the child node
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param type of the node
     * @param builder {@link org.hippoecm.hst.content.beans.dynamic.DynamicBeanBuilder}
     */
    protected abstract void addCustomNodeType(String name, boolean multiple, String type, DynamicBeanBuilder builder);

}
