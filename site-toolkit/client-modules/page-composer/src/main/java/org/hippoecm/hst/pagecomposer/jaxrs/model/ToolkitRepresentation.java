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

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.site.HstSite;

/**
 * The Toolkit represents all unique container items found in the provided {@link org.hippoecm.hst.configuration.hosting.Mount}
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
    public ToolkitRepresentation represent(Mount mount) {
        HstSite site = mount.getHstSite();
        List<HstComponentConfiguration> allUniqueContainerItems = site.getComponentsConfiguration().getAvailableContainerItems();
        for (HstComponentConfiguration child : allUniqueContainerItems) {
            components.add(new ComponentRepresentation().represent(child, mount));
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
