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
package org.hippoecm.frontend.plugins.cms.dashboard.current;

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.IPluginModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLink;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentActivityPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CurrentActivityPlugin.class);
    protected DateFormat df;

    public CurrentActivityPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        if (!(model instanceof IDataProvider)) {
            throw new IllegalArgumentException("CurrentActivityPlugin needs an IDataProvider as Plugin model.");
        }

        //FIXME: detect client timezone
        TimeZone tz = TimeZone.getTimeZone("Europe/Amsterdam");
        df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        df.setTimeZone(tz);

        add(new Label("header", "What's going on"));
        add(new CurrentActivityView("view", model));
    }

    private class CurrentActivityView extends RefreshingView {
        private static final long serialVersionUID = 1L;

        public CurrentActivityView(String id, IModel model) {
            super(id, model);
        }

        @Override
        protected Iterator getItemModels() {
            IDataProvider dataProvider = (IDataProvider) getPluginModel();
            return dataProvider.iterator(0, 0);
        }

        @Override
        protected void populateItem(final Item item) {
            Node node = (Node) item.getModelObject();
            Session session = ((UserSession) getSession()).getJcrSession();
            try {
                if (!node.isNodeType(HippoNodeType.NT_LOGITEM)) {
                    throw new IllegalArgumentException("CurrentActivityPlugin can only process Nodes of type "
                            + HippoNodeType.NT_LOGITEM + ".");
                }

                // Add even/odd row css styling
                item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                    private static final long serialVersionUID = 1L;

                    public Object getObject() {
                        return (item.getIndex() % 2 == 1) ? "even" : "odd";
                    }
                }));

                String timestamp = df.format(new Date(Long.parseLong(node.getName())));
                item.add(new Label("timestamp", timestamp));
                item.add(new Label("user", node.getProperty("hippo:eventUser").getString()));
                item.add(new Label("method", node.getProperty("hippo:eventMethod").getString()));

                // Best effort algoritm to create a 'browse' link to a document.

                // The path to the document variant that was used as input for a Workflow step.    
                String sourceVariant = null;
                boolean sourceVariantExists = false;
                if (node.hasProperty("hippo:eventDocument")) {
                    sourceVariant = node.getProperty("hippo:eventDocument").getValue().getString();
                    sourceVariantExists = ((UserSession) getSession()).getJcrSession().itemExists(sourceVariant);
                }

                //The path to the document variant that was returned by a Workflow step.
                //Workflow steps can return a Document instance who's toString()
                //value is stored as 'Document[uuid=...]'
                String targetVariant = null;
                boolean targetVariantExists = false;
                if (node.hasProperty("hippo:eventReturnValue")) {
                    targetVariant = node.getProperty("hippo:eventReturnValue").getValue().getString();
                    String uuid = StringUtils.substringBetween(targetVariant, "[uuid=", "]");
                    if (uuid != null && !uuid.equals("")) {
                        //The Workflow step has returned a Document instance, look up the 
                        //document it refers to.
                        String path = uuid2Path(uuid);
                        if (path != null && !path.equals("")) {
                            targetVariantExists = session.itemExists(path);
                            if (targetVariantExists) {
                                targetVariant = path;
                            }
                        }
                    } else {
                        //Workflow steps can also return a path String 
                        targetVariantExists = session.itemExists(targetVariant);
                    }
                }

                //Try to create a link to the document variant
                String path = null;
                if (targetVariantExists) {
                    path = targetVariant;
                } else if (sourceVariantExists) {
                    path = sourceVariant;
                }
                if (path != null) {
                    item.add(new BrowseLink("docpath", path, getTopChannel()));
                    return;
                }

                //Maybe both variants have been deleted, try to create a link to the handle
                if (sourceVariant != null) {
                    String handle = StringUtils.substringBeforeLast(sourceVariant, "/");
                    if (session.itemExists(handle)) {
                        item.add(new BrowseLink("docpath", handle, getTopChannel()));
                        return;
                    }
                }

                //Apparently the log item wasn't created by a Workflow step
                //on a document.
                item.add(new Label("docpath", ""));
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
                if (item.get("timestamp") == null) {
                    item.add(new Label("timestamp", ""));
                }
                if (item.get("user") == null) {
                    item.add(new Label("user", ""));
                }
                if (item.get("method") == null) {
                    item.add(new Label("method", ""));
                }
                if (item.get("docpath") == null) {
                    item.add(new Label("docpath", e.getClass().getSimpleName() + ": " + e.getMessage()));
                }
            }
        }
    }

    String uuid2Path(String uuid) {
        if (uuid == null || uuid.equals("")) {
            return null;
        }
        try {
            Session session = ((UserSession) getSession()).getJcrSession();
            Node node = session.getNodeByUUID(uuid);
            return node.getPath();
        } catch (ItemNotFoundException e) {
            return null;
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
