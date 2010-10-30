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

import java.util.Map;

/**
 * A ContainerRepresentation extends {@link org.hippoecm.hst.pagecomposer.jaxrs.model.ComponentRepresentation} with
 * a computed list of it's child items by their id.
 *
 * @version $Id$
 */
public class ContainerRepresentation extends ComponentRepresentation {

    private String[] children;

    @Override
    public ComponentRepresentation represent(HstComponentConfiguration componentConfiguration) {
        super.represent(componentConfiguration);
        String value = "";
        for (Map.Entry<String, HstComponentConfiguration> entry : componentConfiguration.getChildren().entrySet()) {
            HstComponentConfiguration cc = entry.getValue();
            if (value.length() > 0) {
                value += ",";
            }
            value += cc.getCanonicalIdentifier();
        }
        if (value != null) {
            this.children = value.split(",");
        }
        return this;
    }

    public String[] getChildren() {
        return children;
    }

    public void setChildren(String[] children) {
        this.children = children;
    }

}
