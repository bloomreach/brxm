/*
 *  Copyright 2010-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;

/**
 * This class can probably be removed once new items are added through the in-memory model instead of created
 * through jcr.
 *
 */

public class ContainerItemRepresentation extends ComponentRepresentation {

    /**
     * For the legacy container items (not backreferencing to the catalog item) the {@code templateConfig} is {@code null},
     * then properties like classname are fetched from {@code node} directly, otherwise from the {@code templateConfig}
     *
     * @param templateConfig The template config for this {@code node} component item which CAN BE {@code null}, typically
     *                       in the old-style legacy component items which do not maintain a link back to the catalog
     *                       item
     */
    public ContainerItemRepresentation represent(final Node node, final HstComponentConfiguration templateConfig,
                                                 final long newLastModifiedTimestamp) throws RepositoryException {
        setId(node.getIdentifier());
        setName(node.getName());
        setPath(node.getPath());
        setParentId(node.getParent().getIdentifier());

        if (templateConfig == null) {
            // legacy style component item which is not linked back to the catalog item
            if (node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE)) {
                setTemplate(node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_TEMPLATE).getString());
            }
            if (node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_LABEL)) {
                setLabel(node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_LABEL).getString());
            }
            setType(HstComponentConfiguration.Type.CONTAINER_ITEM_COMPONENT.toString());
            if (node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE)) {
                setXtype(node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_XTYPE).getString());
            }

            setComponentClassName(node.hasProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME) ?
                    node.getProperty(HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME).getString() : "");

        } else {
            setTemplate(templateConfig.getHstTemplate());
            setLabel(templateConfig.getLabel());
            setXtype(templateConfig.getXType());
            setComponentClassName(templateConfig.getComponentClassName());
        }

        setLastModifiedTimestamp(newLastModifiedTimestamp);
        setType(HstComponentConfiguration.Type.CONTAINER_ITEM_COMPONENT.toString());
        return this;
    }

}
