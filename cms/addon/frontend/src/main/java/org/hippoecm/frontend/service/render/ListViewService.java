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
package org.hippoecm.frontend.service.render;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.hippoecm.frontend.application.PluginRequestTarget;
import org.hippoecm.frontend.core.PluginContext;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.util.ServiceTracker;
import org.hippoecm.frontend.widgets.AbstractView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListViewService extends RenderService {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ListViewService.class);

    private List<IRenderService> services;
    private ServiceTracker<IRenderService> tracker;

    public ListViewService() {
        services = new LinkedList<IRenderService>();

        tracker = new ServiceTracker<IRenderService>(IRenderService.class);
        tracker.addListener(new ServiceTracker.IListener<IRenderService>() {
            private static final long serialVersionUID = 1L;

            public void onServiceAdded(String name, IRenderService service) {
                services.add(service);
                redraw();
            }

            public void onServiceChanged(String name, IRenderService service) {
            }

            public void onRemoveService(String name, IRenderService service) {
                services.remove(service);
                redraw();
            }
        });

        AbstractView view = new AbstractView("view", new ListDataProvider(services)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item item) {
                IRenderService renderer = (IRenderService) item.getModelObject();
                renderer.bind(ListViewService.this, "item");
                item.add((Component) renderer);
            }

            @Override
            protected void destroyItem(Item item) {
                IRenderService renderer = (IRenderService) item.get("item");
                renderer.unbind();
            }
        };
        add(view);
    }

    @Override
    public void init(PluginContext context, String serviceId, Map<String, ParameterValue> properties) {
        super.init(context, serviceId, properties);
        if (properties.get("item") != null) {
            tracker.open(context, properties.get("item").getStrings().get(0));
        } else {
            log.warn("No item service id configured for {}", serviceId);
        }
    }

    @Override
    public void destroy() {
        tracker.close();
    }

    @Override
    public void render(PluginRequestTarget target) {
        super.render(target);
        for (IRenderService child : services) {
            child.render(target);
        }
    }
}
