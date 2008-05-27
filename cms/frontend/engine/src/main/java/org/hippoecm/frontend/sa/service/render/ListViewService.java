/*
 * Copyright 2008 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.frontend.sa.service.render;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.frontend.sa.plugin.IPluginContext;
import org.hippoecm.frontend.sa.plugin.config.IPluginConfig;
import org.hippoecm.frontend.sa.service.IRenderService;
import org.hippoecm.frontend.sa.service.PluginRequestTarget;
import org.hippoecm.frontend.sa.service.ServiceTracker;
import org.hippoecm.frontend.sa.widgets.AbstractView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListViewService extends RenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ListViewService.class);

    public static final String ITEM = "item";

    private List<IRenderService> services;
    private ServiceTracker<IRenderService> tracker;

    public ListViewService(IPluginContext context, IPluginConfig properties) {
        super(context, properties);
        services = new LinkedList<IRenderService>();

        final AbstractView view = new AbstractView("view", new ListDataProvider(services)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                IRenderService renderer = (IRenderService) item.getModelObject();
                renderer.bind(ListViewService.this, "item");
                item.add((Component) renderer);
                ListViewService.this.onAddRenderService(item, renderer);
            }

            @Override
            protected void destroyItem(Item item) {
                IRenderService renderer = (IRenderService) item.get("item");
                ListViewService.this.onRemoveRenderService(item, renderer);
                renderer.unbind();
            }
        };
        add(view);

        if (properties.get("item") != null) {
            tracker = new ServiceTracker<IRenderService>(IRenderService.class) {
                private static final long serialVersionUID = 1L;

                public void onServiceAdded(IRenderService service, String name) {
                    services.add(service);
                    redraw();
                }

                public void onServiceChanged(IRenderService service, String name) {
                }

                public void onRemoveService(IRenderService service, String name) {
                    services.remove(service);
                    redraw();
                }
            };
            context.registerTracker(tracker, properties.getString("item"));
        } else {
            log.warn("No item id configured");
        }
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        for (IRenderService child : services) {
            child.render(target);
        }
    }

    protected void onAddRenderService(Item item, IRenderService renderer) {
    }

    protected void onRemoveRenderService(Item item, IRenderService renderer) {
    }
}
