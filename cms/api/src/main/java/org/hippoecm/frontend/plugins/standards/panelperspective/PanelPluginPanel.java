/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.standards.panelperspective;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbModel;
import org.apache.wicket.extensions.breadcrumb.IBreadCrumbParticipant;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugins.standards.image.CachingImage;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbLink;
import org.hippoecm.frontend.plugins.standards.panelperspective.breadcrumb.PanelPluginBreadCrumbPanel;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.widgets.AbstractView;

public class PanelPluginPanel extends PanelPluginBreadCrumbPanel {
    private static final long serialVersionUID = 1L;

    static class PanelPluginProvider implements IDataProvider<PanelPlugin> {
        private final IPluginContext context;
        private String panelServiceId;
        private transient List<PanelPlugin> services;

        public PanelPluginProvider(final IPluginContext context, final String panelServiceId) {
            this.context = context;
            this.panelServiceId = panelServiceId;
        }

        private void load() {
            if (services == null) {
                services = context.getServices(this.panelServiceId, PanelPlugin.class);
            }
        }

        @Override
        public Iterator<PanelPlugin> iterator(final long first, final long count) {
            load();

            return services.subList((int) first, (int) (first + count)).iterator();
        }

        @Override
        public long size() {
            load();
            return services.size();
        }

        @Override
        public IModel<PanelPlugin> model(final PanelPlugin object) {
            return new Model<PanelPlugin>(object);
        }

        @Override
        public void detach() {
            services = null;
        }
    }

    public PanelPluginPanel(final String id, final IPluginContext context, final IBreadCrumbModel breadCrumbModel, final String panelServiceId) {
        super(id, breadCrumbModel);

        context.registerTracker(new ServiceTracker<PanelPlugin>(PanelPlugin.class) {

            @Override
            protected void onRemoveService(final PanelPlugin service, final String name) {
                redraw();
            }

            @Override
            protected void onServiceAdded(final PanelPlugin service, final String name) {
                redraw();
            }
        }, panelServiceId);

        setOutputMarkupId(true);

        add(new AbstractView<PanelPlugin>("panels", new PanelPluginProvider(context, panelServiceId)) {

            @Override
            protected void populateItem(final Item<PanelPlugin> item) {
                final PanelPlugin service = item.getModelObject();
                PanelPluginBreadCrumbLink link = new PanelPluginBreadCrumbLink("link", getBreadCrumbModel()) {
                    @Override
                    protected IBreadCrumbParticipant getParticipant(final String componentId) {
                        return service.create(componentId, getBreadCrumbModel());
                    }
                };
                link.add(new CachingImage("img", service.getImage()));
                link.add(new Label("title", service.getTitle()));
                link.add(new Label("help", service.getHelp()));
                item.add(link);
            }
        });
    }

    protected void redraw() {
        AjaxRequestTarget target = getRequestCycle().find(AjaxRequestTarget.class);
        if (target != null) {
            target.add(this);
        }
    }

    public IModel<String> getTitle(Component component) {
        return new StringResourceModel("panel-plugin-panel-title", component, null);
    }
}
