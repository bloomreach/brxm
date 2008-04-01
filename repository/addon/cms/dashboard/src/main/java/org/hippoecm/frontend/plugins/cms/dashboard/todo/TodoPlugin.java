/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugins.cms.dashboard.todo;

import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TodoPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(TodoPlugin.class);

    public TodoPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        if (!(model instanceof IDataProvider)) {
            throw new IllegalArgumentException("TodoPlugin needs an IDataProvider as Plugin model.");
        }

        add(new Label("header", "Todo"));
        add(new TodoView("view", model));
    }

    private class TodoView extends RefreshingView {
        private static final long serialVersionUID = 1L;

        public TodoView(String id, IPluginModel model) {
            super(id, model);
        }

        @Override
        protected Iterator getItemModels() {
            IDataProvider dataProvider = (IDataProvider) getPluginModel();
            return dataProvider.iterator(0, 0);
        }

        @Override
        protected void populateItem(final Item item) {
            Node requestNode = (Node) item.getModelObject();
            String path;
            try {
                path = requestNode.getPath();
                item.add(new BrowseLink("request", path, getTopChannel()));
            } catch (RepositoryException e) {
                item.add(new Label("request", e.getMessage()));
            }           
            item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                public Object getObject() {
                    return (item.getIndex() % 2 == 1) ? "even" : "odd";
                }
            }));
        }
    }

}
