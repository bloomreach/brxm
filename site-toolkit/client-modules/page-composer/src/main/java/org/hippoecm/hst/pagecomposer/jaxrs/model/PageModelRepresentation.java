/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;

/**
 * Representation of an entire in-memory pageModel for a given pageUUID
 *
 * @version $Id$
 */

@XmlRootElement(name = "data")
public class PageModelRepresentation {

    List<ComponentRepresentation> components = new ArrayList<ComponentRepresentation>();

    public PageModelRepresentation represent(HstSite site, String rootComponentId, Mount mount) {
        HstComponentConfiguration rootComponentConfig = null;
        for (HstComponentConfiguration config : site.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (config.getCanonicalIdentifier().equals(rootComponentId)) {
                rootComponentConfig = config;
                break;
            }
        }

        if (rootComponentConfig == null) {
            throw new RuntimeException("Cannot find component configuration for root id '" + rootComponentId + "'");
        }

        populateContainersAndItems(rootComponentConfig, mount);
        return this;
    }

    public List<ComponentRepresentation> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentRepresentation> components) {
        this.components = components;
    }

    private void populateContainersAndItems(HstComponentConfiguration component, Mount mount) {
        for (HstComponentConfiguration child : component.getChildren().values()) {
            if (child.getComponentType() == Type.CONTAINER_COMPONENT) {
                components.add(new ContainerRepresentation().represent(child, mount));
            } else if (child.getComponentType() == Type.CONTAINER_ITEM_COMPONENT) {
                components.add(new ContainerItemRepresentation().represent(child, mount));
            }
            populateContainersAndItems(child, mount);
        }
    }


}
