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
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

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
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.dashboard.BrowseLink;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentActivityPlugin extends RenderPlugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(CurrentActivityPlugin.class);
    static DateFormat df;

    public CurrentActivityPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        if (!(getModel() instanceof IDataProvider)) {
            throw new IllegalArgumentException("CurrentActivityPlugin needs a model that is an IDataProvider.");
        }

        //FIXME: detect client timezone (use StyleDateConverter?)
        //TimeZone tz = TimeZone.getTimeZone("Europe/Amsterdam");
        df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        //df.setTimeZone(tz);

        add(new CurrentActivityView("view", getModel()));
    }

    @Override
    protected void onModelChanged() {
        super.onModelChanged();
        redraw();
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

    private class CurrentActivityView extends RefreshingView {
        private static final long serialVersionUID = 1L;

        public CurrentActivityView(String id, IModel model) {
            super(id, model);
        }

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
            Node node = (Node) item.getModelObject();
            Session session = ((UserSession) getSession()).getJcrSession();
            try {
                if (!node.isNodeType("hippolog:item")) {
                    throw new IllegalArgumentException(
                            "CurrentActivityPlugin can only process Nodes of type hippolog:item.");
                }

                // Add even/odd row css styling
                item.add(new AttributeModifier("class", true, new AbstractReadOnlyModel() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getObject() {
                        return (item.getIndex() % 2 == 1) ? "even" : "odd";
                    }
                }));

                Calendar nodeCal = Calendar.getInstance();
                nodeCal.setTime(new Date(Long.parseLong(node.getName())));
                String timestamp = "";
                try {
                    timestamp = relativeTime(nodeCal);
                } catch (IllegalArgumentException ex) {
                }

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
                        try {
                            targetVariantExists = session.itemExists(targetVariant);
                        } catch (RepositoryException e) {
                            targetVariantExists = false;
                        }
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
                    // We have a path to a document variant, so we can link to it!
                    String label = new StringResourceModel(timestamp, this, null, "").getString()
                            + new StringResourceModel(node.getProperty("hippo:eventMethod").getString(), this, null,
                                    new Object[] { node.getProperty("hippo:eventUser").getString(),
                                            new NodeTranslator(new JcrNodeModel(path)).getNodeName().getObject() })
                                    .getString();
                    BrowseLink link = new BrowseLink(getPluginContext(), getPluginConfig(), "entry", path, label);
                    item.add(link);
                    return;
                } else {
                    //Maybe both variants have been deleted, try to create a link to the handle
                    if (sourceVariant != null) {
                        String handle = StringUtils.substringBeforeLast(sourceVariant, "/");
                        if (session.itemExists(handle)) {

                            String label = new StringResourceModel(timestamp, this, null, "").getString()
                                    + new StringResourceModel(node.getProperty("hippo:eventMethod").getString(), this,
                                            null, new Object[] {
                                                    node.getProperty("hippo:eventUser").getString(),
                                                    new NodeTranslator(new JcrNodeModel(handle)).getNodeName()
                                                            .getObject() }).getString();
                            BrowseLink link = new BrowseLink(getPluginContext(), getPluginConfig(), "entry", handle,
                                    label);
                            item.add(link);
                            return;
                        } else {
                            String name = StringUtils.substringAfterLast(sourceVariant, "/");
                            // No path, so we're just rendering a label without a link
                            String label = new StringResourceModel(timestamp, this, null, "").getString()
                                    + new StringResourceModel(node.getProperty("hippo:eventMethod").getString(), this,
                                            null, new Object[] { node.getProperty("hippo:eventUser").getString(),
                                            (name == null ? "" : name) }).getString();
                            Label entryLabel = new Label("entry", label);
                            entryLabel.setEscapeModelStrings(false);
                            item.add(entryLabel);
                            return;
                        }
                    }
                }

                //Apparently the log item wasn't created by a Workflow step
                //on a document.
                String label = new StringResourceModel(timestamp, this, null, "").getString()
                        + new StringResourceModel(node.getProperty("hippo:eventMethod").getString(), this, null,
                                new Object[] { node.getProperty("hippo:eventUser").getString() }).getString();
                Label entryLabel = new Label("entry", label);
                entryLabel.setEscapeModelStrings(false);
                item.add(entryLabel);

            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
                if (item.get("timestamp") == null) {
                    item.add(new Label("timestamp", ""));
                }
                //                if (item.get("user") == null) {
                //                    item.add(new Label("user", ""));
                //                }
                if (item.get("method") == null) {
                    item.add(new Label("method", ""));
                }
            }
        }

        private String relativeTime(Calendar nodeCal) {

            Calendar currentCal = Calendar.getInstance();

            Calendar yesterdayCal = Calendar.getInstance();
            yesterdayCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal
                    .get(Calendar.DAY_OF_MONTH), 0, 0, 0);
            yesterdayCal.add(Calendar.DAY_OF_MONTH, -1);

            Calendar todayCal = Calendar.getInstance();
            todayCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal
                    .get(Calendar.DAY_OF_MONTH), 0, 0, 0);

            Calendar thisEveningCal = Calendar.getInstance();
            thisEveningCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal
                    .get(Calendar.DAY_OF_MONTH), 23, 59, 59);

            Calendar thisAfternoonCal = Calendar.getInstance();
            thisAfternoonCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal
                    .get(Calendar.DAY_OF_MONTH), 18, 0, 0);

            Calendar thisMorningCal = Calendar.getInstance();
            thisMorningCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH), currentCal
                    .get(Calendar.DAY_OF_MONTH), 12, 0, 0);

            Calendar hourAgoCal = Calendar.getInstance();
            hourAgoCal.add(Calendar.HOUR, -1);

            Calendar halfHourAgoCal = Calendar.getInstance();
            halfHourAgoCal.add(Calendar.MINUTE, -30);

            Calendar tenMinutesAgoCal = Calendar.getInstance();
            tenMinutesAgoCal.add(Calendar.MINUTE, -10);

            Calendar fiveMinutesAgoCal = Calendar.getInstance();
            fiveMinutesAgoCal.add(Calendar.MINUTE, -5);

            Calendar oneMinuteAgoCal = Calendar.getInstance();
            oneMinuteAgoCal.add(Calendar.MINUTE, -1);

            if (nodeCal.after(oneMinuteAgoCal)) {
                return new String("one-minute");
            }
            if (nodeCal.after(fiveMinutesAgoCal)) {
                return new String("five-minutes");
            }
            if (nodeCal.after(tenMinutesAgoCal)) {
                return new String("ten-minutes");
            }
            if (nodeCal.after(halfHourAgoCal)) {
                return new String("half-hour");
            }
            if (nodeCal.after(hourAgoCal)) {
                return new String("hour");
            }
            if (nodeCal.before(thisMorningCal) && nodeCal.after(todayCal)) {
                return new String("morning");
            }
            if (nodeCal.before(thisAfternoonCal) && nodeCal.after(todayCal)) {
                return new String("afternoon");
            }
            if (nodeCal.before(thisEveningCal) && nodeCal.after(todayCal)) {
                return new String("evening");
            }
            if (nodeCal.after(yesterdayCal)) {
                return new String("yesterday");
            }
            return df.format(nodeCal);
        }
    }

}
