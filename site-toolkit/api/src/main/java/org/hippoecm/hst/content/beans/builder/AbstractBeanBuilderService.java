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

        private DocumentType(String type) {
            this.type = type;
        }

        public static DocumentType getDocumentType(String type) {
            return Arrays.asList(DocumentType.values())
                .stream()
                .filter(doc -> doc.getDocumentType().equals(type))
                .findFirst()
                .orElse(DocumentType.UNKNOWN);
        }
    }

    /**
     * Generates bean method by its properties
     * 
     * @param bean
     * @param builderParameters
     */
    protected void generateMethodsByProperties(HippoContentBean bean, BeanBuilderServiceParameters builderParameters) {
        for (HippoContentProperty property : bean.getProperties()) {
            final String name = property.getName();
            final boolean multiple = property.isMultiple();
            final String cmsType = property.getCmsType();

            boolean hasChange = hasChange(name, multiple, builderParameters);
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
                addBeanMethodString(name, multiple, builderParameters);
                break;
            case DATE:
                addBeanMethodCalendar(name, multiple, builderParameters);
                break;
            case BOOLEAN:
                addBeanMethodBoolean(name, multiple, builderParameters);
                break;
            case LONG:
                addBeanMethodLong(name, multiple, builderParameters);
                break;
            case DOUBLE:
                addBeanMethodDouble(name, multiple, builderParameters);
                break;
            case DOCBASE:
                addBeanMethodDocbase(name, multiple, builderParameters);
                break;
            default:
                addCustomPropertyType(name, multiple, type, builderParameters);
                break;
            }
        }
    }

    /**
     * Generated bean methods by its child nodes
     * 
     * @param bean
     * @param builderParameters
     */
    protected void generateMethodsByNodes(HippoContentBean bean, BeanBuilderServiceParameters builderParameters) {
        for (HippoContentChildNode child : bean.getChildren()) {
            final String name = child.getName();
            final boolean multiple = child.isMultiple();

            boolean hasChange = hasChange(name, multiple, builderParameters);
            if (!hasChange) {
                continue;
            }

            final String type = child.getType();
            log.debug("processing missing node, BEAN: {}, CHILD: {}", bean.getName(), child.getName());

            if (type == null) {
                log.error("Missing type for node, cannot create method {}", child.getName());
                continue;
            }

            DocumentType documentType = DocumentType.getDocumentType(type);

            switch (documentType) {
            case HIPPO_HTML:
                addBeanMethodHippoHtml(name, multiple, builderParameters);
                break;
            case HIPPO_IMAGELINK:
                addBeanMethodImageLink(name, multiple, builderParameters);
                break;
            case HIPPO_MIRROR:
                addBeanMethodHippoMirror(name, multiple, builderParameters);
                break;
            case HIPPO_IMAGE:
                addBeanMethodHippoImage(name, multiple, builderParameters);
                break;
            case HIPPO_RESOURCE:
                addBeanMethodHippoResource(name, multiple, builderParameters);
                break;
            default:
                addCustomNodeType(name, multiple, type, builderParameters);
                break;
            }
        }
    }

    /**
     * Checks whether a property/node should be generated as a method for the bean or not
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     * @return true if the property/node has to be generated as method
     */
    public abstract boolean hasChange(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link String} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodString(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link Calendar} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodCalendar(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link Boolean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodBoolean(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link Long} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodLong(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link Double} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodDouble(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link HippoBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodDocbase(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a custom implementation if there isn't any matching object type for the property
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param type of the document property
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addCustomPropertyType(String name, boolean multiple, String type, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link HippoHtml} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodHippoHtml(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link HippoGalleryImageSet} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodImageLink(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link HippoBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodHippoMirror(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link HippoGalleryImageBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodHippoImage(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a method to the bean which returns {@link HippoResourceBean} object type
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addBeanMethodHippoResource(String name, boolean multiple, BeanBuilderServiceParameters builderParameters);

    /**
     * Adds a custom implementation if there isn't any matching object type for the child node
     * 
     * @param name of the method
     * @param multiple whether a document property keeps multiple values or not
     * @param type of the document property
     * @param builderParameters additional parameters for builder implementation
     */
    public abstract void addCustomNodeType(String name, boolean multiple, String type, BeanBuilderServiceParameters builderParameters);

}
