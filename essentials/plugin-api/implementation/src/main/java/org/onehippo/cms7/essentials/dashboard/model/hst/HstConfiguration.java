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

import org.onehippo.cms7.essentials.dashboard.model.JcrModel;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@PersistentNode(type = "hst:configuration")
public class HstConfiguration extends BaseJcrModel {


    private JcrModel templates = new HstTemplates();

    public HstConfiguration() {
        addChild(templates);
    }

    public HstConfiguration(final String name, final String parentPath) {
        this();
        setName(name);
        setParentPath(parentPath);
        templates.setParentPath("/hst:hst/hst:configurations/" + getName());
    }


    public void addTemplate(final JcrModel template) {
        template.setParentPath(templates.getParentPath());
        templates.addChild(template);
    }


}
