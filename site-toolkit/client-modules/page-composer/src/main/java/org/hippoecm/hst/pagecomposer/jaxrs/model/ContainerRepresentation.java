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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;

/**
 * A ContainerRepresentation extends {@link org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentRepresentation} with
 * a computed list of it's child items by their id.
 *
 * @version $Id$
 */
public class ContainerRepresentation extends ComponentRepresentation {

    private List<String> children;

    public ContainerRepresentation represent(HstComponentConfiguration componentConfiguration, Mount mount) {
        super.represent(componentConfiguration, mount);
        if (componentConfiguration.getLastModified() == null) {
            setLastModifiedTimestamp(0);
        } else {
            setLastModifiedTimestamp(componentConfiguration.getLastModified().getTimeInMillis());
        }
  
        Map<String, HstComponentConfiguration> childrenMap = componentConfiguration.getChildren();

        children = new LinkedList<>();
        if (!childrenMap.isEmpty()) {
            for (Map.Entry<String, HstComponentConfiguration> entry : componentConfiguration.getChildren().entrySet()) {
                HstComponentConfiguration cc = entry.getValue();
                children.add(cc.getCanonicalIdentifier());
            }
        }
        
        return this;
    }

    public List<String> getChildren() {
        return children;
    }

    public void setChildren(List<String> children) {
        if (children == null) {
            this.children = null;
        } else {
            this.children = new LinkedList<>(children);
        }
    }

}
