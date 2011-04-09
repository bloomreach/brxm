/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.cms.admin;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.cms.admin.crumbs.AdminBreadCrumbPanel;
import org.hippoecm.frontend.plugins.cms.admin.widgets.AjaxBreadCrumbPanelLink;
import org.hippoecm.frontend.widgets.AbstractView;

public class AdminPanel extends AdminBreadCrumbPanel {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";
    private static final long serialVersionUID = 1L;

    static class AdminPluginProvider implements IDataProvider<AdminPlugin> {
        private final IPluginContext context;

        private transient List<AdminPlugin> services;

        public AdminPluginProvider(final IPluginContext context) {
            this.context = context;
        }

        private void load() {
            if (services == null) {
                services = context.getServices(AdminPlugin.ADMIN_PANEL_ID, AdminPlugin.class);
            }
        }

        @Override
        public Iterator<AdminPlugin> iterator(final int first, final int count) {
            load();

            return services.subList(first, first + count).iterator();
        }

        @Override
        public int size() {
            load();
            return services.size();
        }

        @Override
        public IModel<AdminPlugin> model(final AdminPlugin object) {
            return new Model(object);
        }

        @Override
        public void detach() {
            services = null;
        }
    }

    public AdminPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel) {
        super(id, breadCrumbModel);

        add(new AbstractView<AdminPlugin>("panels", new AdminPluginProvider(context)) {

            @Override
            protected void populateItem(final Item<AdminPlugin> item) {
                AdminPlugin service = item.getModelObject();
                AjaxBreadCrumbPanelLink link = new AjaxBreadCrumbPanelLink("link", getBreadCrumbModel(), service);
                link.add(new Image("img", service.getImage()));
                link.add(new Label("title", service.getTitle()).setRenderBodyOnly(true));
                link.add(new Label("help", service.getHelp()));
                item.add(link);
            }
        });
    }

    public IModel getTitle(Component component) {
        return new StringResourceModel("admin-title", component, null);
    }
}
