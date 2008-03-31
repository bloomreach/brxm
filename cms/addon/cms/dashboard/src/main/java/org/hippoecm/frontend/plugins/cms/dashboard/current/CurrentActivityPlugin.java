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
import org.hippoecm.frontend.model.JcrNodeModel;
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
            try {
                if (node.isNodeType(HippoNodeType.NT_LOGITEM)) {
                    String timestamp = df.format(new Date(Long.parseLong(node.getName())));
                    item.add(new Label("timestamp", timestamp));
                    item.add(new Label("user", node.getProperty("hippo:eventUser").getString()));
                    item.add(new Label("method", node.getProperty("hippo:eventMethod").getString()));

                    String retval = "";
                    if (node.hasProperty("hippo:eventReturnValue")) {
                        retval = node.getProperty("hippo:eventReturnValue").getValue().getString();
                        String value = StringUtils.substringBetween(retval, "[uuid=", "]");
                        if (value != null && !value.equals("")) {
                            Session session = ((UserSession) getSession()).getJcrSession();
                            retval = session.getNodeByUUID(value).getPath();
                            retval = StringUtils.substringBeforeLast(retval, "[");
                            retval = StringUtils.substringBeforeLast(retval, "/");
                        }
                    }
                    item.add(new BrowseLink("retval", new JcrNodeModel(retval), getTopChannel()));

                } else {
                    item.add(new Label("timestamp", ""));
                    item.add(new Label("user", ""));
                    item.add(new Label("method", ""));
                    item.add(new Label("retval", ""));
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
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

//                    String arguments = "";
//                    if (node.hasProperty("hippo:eventArguments")) {
//                        Value[] values = node.getProperty("hippo:eventArguments").getValues();
//                        for (int i = 0; i < values.length; i++) {
//                            arguments += values[i].getString() + ", ";
//                        }
//                    }
//                    if (arguments.endsWith(", ")) {
//                        arguments = arguments.substring(0, arguments.length()-2);
//                    }
//                    item.add(new Label("arguments", arguments));