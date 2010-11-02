/*
 *  Copyright 2010 Hippo.
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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.SiteMount;
import org.hippoecm.hst.configuration.site.HstSite;

import java.util.ArrayList;
import java.util.List;

/**
 * The Toolkit is a special node of type hst:containercomponent that hosts the prototype hst:containeritems that
 * can be added to a page running in composermode.
 *
 * @version $Id$
 */
public class ToolkitRepresentation {

    List<ComponentRepresentation> components = new ArrayList<ComponentRepresentation>();

    /**
     * This method returns all the container items for this <code>mount</code>
     * @param mount the current mount
     * @return ToolkitRepresentation containing all the containeritem's for this <code>mount</code>
     */
    public ToolkitRepresentation represent(SiteMount mount) {
        HstSite site = mount.getHstSite();
        List<HstComponentConfiguration> allUniqueContainerItems = site.getComponentsConfiguration().getUniqueContainerItems();
        for (HstComponentConfiguration child : allUniqueContainerItems) {
            components.add(new ComponentRepresentation().represent(child));
        }
        return this;
    }
    
    /**
     * This method returns all the container items directly below the 'toolkit' component. This is a configurable component.
     * @param mount the current mount
     * @param toolkitId the id of the root component containing all containeritems to be shown
     * @return ToolkitRepresentation containing all the containeritem's for this <code>mount</code> and <code>toolkitId</code>
     */
    public ToolkitRepresentation represent(SiteMount mount, String toolkitId) {
        HstSite site = mount.getHstSite();
        HstComponentConfiguration root = null;
        for (HstComponentConfiguration config : site.getComponentsConfiguration().getComponentConfigurations().values()) {
            if (config.getCanonicalIdentifier().equals(toolkitId)) {
                root = config;
                break;
            }
        }
        if (root == null) {
            throw new RuntimeException("Cannot find component configuration for root id '" + toolkitId + "'");
        }
        for (HstComponentConfiguration child : root.getChildren().values()) {
            if (child.getComponentType() == HstComponentConfiguration.Type.CONTAINER_ITEM_COMPONENT) {
                components.add(new ComponentRepresentation().represent(child));
            }
        }
        return this;
    }
    
    public List<ComponentRepresentation> getComponents() {
        return components;
    }

    public void setComponents(List<ComponentRepresentation> components) {
        this.components = components;
    }

}
