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

package org.onehippo.cms7.essentials.installer;

import java.util.List;

import org.apache.wicket.extensions.ajax.markup.html.tabs.AjaxTabbedPanel;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class EssentialsTabPanel<T extends ITab> extends AjaxTabbedPanel<T> {

    private static final long serialVersionUID = -7050312674160727307L;
    private static Logger log = LoggerFactory.getLogger(EssentialsTabPanel.class);

    public EssentialsTabPanel(final String id, final List<T> tabs) {
        super(id, tabs);
    }

    public EssentialsTabPanel(final String id, final List<T> tabs, final IModel<Integer> model) {
        super(id, tabs, model);
    }

    @Override
    protected String getTabContainerCssClass() {
        return "nav nav-tabs";
    }

    @Override
    protected String getSelectedTabCssClass() {
        return "active";
    }
}
