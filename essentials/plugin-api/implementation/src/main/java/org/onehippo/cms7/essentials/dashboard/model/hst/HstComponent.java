/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.model.hst;

import java.util.ArrayList;
import java.util.List;

import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentCollection;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
@PersistentNode(type = "hst:component")
public class HstComponent extends BaseJcrModel{

    private static Logger log = LoggerFactory.getLogger(HstComponent.class);

    @PersistentProperty(name = "hst:template")
    private String hstTemplate;


    @PersistentCollection
    private List<HstComponent> components = new ArrayList<>();

    public void addComponent(final HstComponent component){
        components.add(component);
    }

    public List<HstComponent> getComponents() {
        return components;
    }

    public void setComponents(final List<HstComponent> components) {
        this.components = components;
    }
}
