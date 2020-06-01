/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.edit;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.tabs.TabbedPanel;
import org.hippoecm.frontend.plugins.standards.tabs.TabsPlugin;
import org.hippoecm.frontend.service.EditorException;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorTabsPlugin extends TabsPlugin {

    private static final Logger log = LoggerFactory.getLogger(EditorTabsPlugin.class);
    
    public EditorTabsPlugin(final IPluginContext context, final IPluginConfig properties) {
        super(context, properties);
    }

    @Override
    protected TabbedPanel newTabbedPanel(String id, List<TabsPlugin.Tab> tabs, MarkupContainer tabsContainer) {
        return new TabbedPanel(id, EditorTabsPlugin.this, tabs, tabsContainer) {

            @Override
            protected Form getPanelContainerForm() {
                return (Form) get("panel-container");
            }

            @Override
            protected WebMarkupContainer newPanelContainer(final String id) {
                final Form form = new Form(id);
                // prevent the form submit when hitting enter key at input fields
                form.add(new PreventDefaultFormSubmitBehavior());
                return form;
            }
        };
    }

    @Override
    protected void onTabDeactivated(final Tab tab) {
        super.onTabDeactivated(tab);
        savePendingChanges(tab);
    }

    private void savePendingChanges(final Tab tab) {
        final Session session = UserSession.get().getJcrSession();
        try {
            session.save();
        } catch (RepositoryException e) {
            final String user = session.getUserID();
            final String tabName = tab.getTitle().getObject();
            log.warn("User '{}' failed to save session when leaving editor tab '{}', discarding changes. Cause:", user, tabName, e);
            discardChanges(tab);
        }
    }

    private void discardChanges(final Tab tab) {
        try {
            tab.discard();
        } catch (EditorException e) {
            log.warn("Also failed to discard changes", e);
        }
    }

    private static class PreventDefaultFormSubmitBehavior extends AjaxEventBehavior {

        PreventDefaultFormSubmitBehavior() {
            super("submit");
        }

        @Override
        protected void onEvent(final AjaxRequestTarget target) {
            // do nothing
        }
    }
}
