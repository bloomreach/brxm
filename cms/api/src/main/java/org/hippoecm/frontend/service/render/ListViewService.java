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
package org.hippoecm.frontend.service.render;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IRenderService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.frontend.widgets.AbstractView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility base class for list render services.  A {@link ServiceTracker} tracks
 * the {@link IRenderService}s that register at the service name {@linkplain #getItemId()}.
 * By default, the name that is available under key "item" is used.
 * <p>
 * The service redraws completely when a child service (dis)appears.
 */
public class ListViewService<T> extends RenderService<T> {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ListViewService.class);

    public static final String ITEM = "item";

    private static class RenderServiceModel extends Model<IRenderService> {
        private static final long serialVersionUID = 1L;

        RenderServiceModel(IRenderService service) {
            super(service);
        }

        @Override
        public int hashCode() {
            return getObject().hashCode() * 19;
        }

        @Override
        public boolean equals(Object that) {
            if (that != null && that instanceof RenderServiceModel) {
                return ((RenderServiceModel) that).getObject() == getObject();
            }
            return false;
        }
    }

    private static class EvenOddModel extends Model<String> {
        private static final long serialVersionUID = 1L;

        public EvenOddModel(int index) {
            if (index % 2 == 0) {
                setObject("even");
            } else {
                setObject("odd");
            }
        }

    }

    private List<IRenderService> services;
    private ServiceTracker<IRenderService> tracker;
    private AbstractView<IRenderService> view;

    public ListViewService(IPluginContext context, IPluginConfig properties) {
        super(context, properties);
        services = new LinkedList<IRenderService>();

        final IDataProvider<IRenderService> provider = new ListDataProvider<IRenderService>(services) {
            private static final long serialVersionUID = 1L;

            @Override
            public IModel<IRenderService> model(IRenderService object) {
                return new RenderServiceModel(object);
            }
        };

        view = new AbstractView<IRenderService>("view", provider) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(Item<IRenderService> item) {
                IRenderService renderer = item.getModelObject();
                renderer.bind(ListViewService.this, "item");
                item.add(renderer.getComponent());
                ListViewService.this.onAddRenderService(item, renderer);
                item.add(new AttributeAppender("class", new EvenOddModel(item.getIndex()), " "));
            }

            @Override
            protected void destroyItem(Item<IRenderService> item) {
                IRenderService renderer = item.getModelObject();
                item.remove(renderer.getComponent());
                ListViewService.this.onRemoveRenderService(item, renderer);
                renderer.unbind();
            }
        };
        add(view);

        String itemId = getItemId();
        if (itemId != null) {
            tracker = new ServiceTracker<IRenderService>(IRenderService.class) {
                private static final long serialVersionUID = 1L;

                @Override
                public void onServiceAdded(IRenderService service, String name) {
                    log.debug("adding " + service + " to ListViewService at " + name);
                    services.add(service);
                    redraw();
                }

                @Override
                public void onServiceChanged(IRenderService service, String name) {
                }

                @Override
                public void onRemoveService(IRenderService service, String name) {
                    log.debug("removing " + service + " from ListViewService at " + name);
                    services.remove(service);
                    redraw();
                }
            };
            context.registerTracker(tracker, itemId);
        } else {
            log.warn("No item id configured");
        }
    }

    protected String getItemId() {
        return getPluginConfig().getString(ITEM);
    }

    @Override
    public void render(PluginRequestTarget target) {
        view.populate();
        super.render(target);
        for (IRenderService child : services) {
            child.render(target);
        }
    }

    protected void onAddRenderService(Item<IRenderService> item, IRenderService renderer) {
    }

    protected void onRemoveRenderService(Item<IRenderService> item, IRenderService renderer) {
    }
}
