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

import org.onehippo.cms7.essentials.dashboard.model.JcrModel;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.Persistent;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentCollection;
import org.onehippo.cms7.essentials.dashboard.utils.annotations.PersistentNode;

/**
 * @version "$Id$"
 */
@PersistentNode(type = "hst:configuration")
public class HstConfiguration extends BaseJcrModel {



    @Persistent
    private HstSiteMenus siteMenus = new HstSiteMenus();

    @Persistent
    private HstTemplates templates = new HstTemplates();


    public HstConfiguration(final String name, final String parentPath) {
        setName(name);
        setParentPath(parentPath);
        final String myPath = "/hst:hst/hst:configurations/" + getName();
        templates.setParentPath(myPath);
        siteMenus.setParentPath(myPath);
    }

    public HstTemplate addTemplate(final String name) {
        final HstTemplate template = new HstTemplate(name);
        templates.addTemplate(template);
        return template;
    }
    public HstTemplate addTemplate(final String name, final String renderPath) {
        final HstTemplate template = new HstTemplate(name, renderPath);
        templates.addTemplate(template);
        return template;
    }



    public HstSiteMenu addMenu(final String name){
        final HstSiteMenu hstSiteMenu = new HstSiteMenu(name);
        siteMenus.addMenu(hstSiteMenu);
        return hstSiteMenu;
    }

}
