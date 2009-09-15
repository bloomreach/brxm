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
package org.hippoecm.frontend.plugins.cms.dashboard.current;

import java.util.Iterator;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLink;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLinkTarget;
import org.hippoecm.frontend.plugins.cms.dashboard.DocumentEvent;
import org.hippoecm.frontend.plugins.cms.dashboard.EventModel;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentActivityPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CurrentActivityPlugin.class);

    public CurrentActivityPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (!(getModel() instanceof IDataProvider)) {
            throw new IllegalArgumentException("CurrentActivityPlugin needs a model that is an IDataProvider.");
        }

        add(new CurrentActivityView("view", getModel()));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }

    private class CurrentActivityView extends RefreshingView {
        private static final long serialVersionUID = 1L;

        public CurrentActivityView(String id, IModel model) {
            super(id, model);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Iterator getItemModels() {
            final IDataProvider dataProvider = (IDataProvider) getModel();
            final Iterator iter = dataProvider.iterator(0, 0);
            return new Iterator() {

                public boolean hasNext() {
                    return iter.hasNext();
                }

                public Object next() {
                    return dataProvider.model(iter.next());
                }

                public void remove() {
                    iter.remove();
                }

            };
        }

        @Override
        protected void populateItem(final Item item) {
            // Add even/odd row css styling
            item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return (item.getIndex() % 2 == 1) ? "even" : "odd";
                }
            }));

            DocumentEvent documentEvent = new DocumentEvent((JcrNodeModel) item.getModel());
            String path = documentEvent.getDocumentPath();
            if (path != null) {
                EventModel label = new EventModel((JcrNodeModel) item.getModel(), new JcrNodeModel(path));
                BrowseLinkTarget target = new BrowseLinkTarget(path);
                BrowseLink link = new BrowseLink(getPluginContext(), getPluginConfig(), "entry", new Model(target),
                        label);
                item.add(link);
            } else {
                EventModel label = new EventModel((JcrNodeModel) item.getModel());
                Label entryLabel = new Label("entry", label);
                entryLabel.setEscapeModelStrings(false);
                item.add(entryLabel);
            }
        }
    }

}
