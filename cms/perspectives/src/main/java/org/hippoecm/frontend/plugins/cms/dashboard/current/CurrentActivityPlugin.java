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

import java.text.DateFormat;
import java.util.Iterator;

import javax.jcr.Node;

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

public class CurrentActivityPlugin extends RenderPlugin<Node> {

    private static final long serialVersionUID = 1L;

    public CurrentActivityPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (!(getDefaultModel() instanceof IDataProvider)) {
            throw new IllegalArgumentException("CurrentActivityPlugin needs a model that is an IDataProvider.");
        }

        add(new CurrentActivityView("view", getDefaultModel()));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
    }

    private class CurrentActivityView extends RefreshingView<Node> {
        private static final long serialVersionUID = 1L;

        private final DateFormat dateFormat;

        public CurrentActivityView(String id, IModel model) {
            super(id, model);
            dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, getSession().getLocale());
        }

        @Override
        protected Iterator<IModel<Node>> getItemModels() {
            final IDataProvider<Node> dataProvider = (IDataProvider<Node>) getDefaultModel();
            final Iterator<? extends Node> iter = dataProvider.iterator(0, 0);
            return new Iterator<IModel<Node>>() {

                public boolean hasNext() {
                    return iter.hasNext();
                }

                public IModel<Node> next() {
                    return dataProvider.model(iter.next());
                }

                public void remove() {
                    iter.remove();
                }

            };
        }

        @Override
        protected void populateItem(final Item<Node> item) {
            // Add even/odd row css styling
            item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public Object getObject() {
                    return (item.getIndex() % 2 == 1) ? "even" : "odd";
                }
            }));

            final DocumentEvent documentEvent = new DocumentEvent(item.getModel());
            final IModel<String> nameModel = documentEvent.getName();
            String path = documentEvent.getDocumentPath();
            if (path != null) {
                path = fixPathForRequests(path);
                EventModel label = new EventModel((JcrNodeModel) item.getModel(), nameModel, dateFormat);
                BrowseLinkTarget target = new BrowseLinkTarget(path);
                BrowseLink link = new BrowseLink(getPluginContext(), getPluginConfig(), "entry", new Model<BrowseLinkTarget>(target), label);
                item.add(link);
            } else {
                EventModel label = new EventModel((JcrNodeModel) item.getModel(), nameModel, dateFormat);
                Label entryLabel = new Label("entry", label);
                entryLabel.setEscapeModelStrings(false);
                item.add(entryLabel);
            }
        }

        /*
         * FIXME: This is a temporary(?) best effort fix for not showing "hippo:request" in 
         * the activity view.
         */
        private String fixPathForRequests(String path) {
            if (!path.endsWith("hippo:request")) {
                return path;
            }

            String[] pathElts = path.split("/");
            StringBuilder newPath = new StringBuilder();
            // build new path, strip last element, eg "hippo:request"
            for (int i = 0; i < pathElts.length - 1; i++) {
                if (pathElts[i].length() > 0) {
                    newPath.append("/").append(pathElts[i]);
                }
            }
            // add last part again to point to document
            newPath.append("/").append(pathElts[pathElts.length - 2]);
            return newPath.toString();
        }
    }

}
