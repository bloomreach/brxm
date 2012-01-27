/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.frontend.plugins.cms.dashboard;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.MissingResourceException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.model.JcrItemModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventModel implements IComponentAssignedModel<String> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EventModel.class);

    private DateFormat df;
    private String time;
    private String method;
    private String user;
    private IModel<String> nameModel;

    public EventModel(JcrNodeModel eventNode) {
        this(eventNode, null);
    }

    public EventModel(JcrNodeModel eventNode, IModel<String> nameModel) {
        Node node = eventNode.getNode();
        try {
            if (node == null || !node.isNodeType("hippolog:item")) {
                throw new IllegalArgumentException(
                        "CurrentActivityPlugin can only process Nodes of type hippolog:item.");
            }

            //FIXME: detect client timezone (use StyleDateConverter?)
            //TimeZone tz = TimeZone.getTimeZone("Europe/Amsterdam");
            df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            //df.setTimeZone(tz);

            Calendar nodeCal = Calendar.getInstance();
            nodeCal.setTime(new Date(Long.parseLong(node.getName())));
            String timestamp = "";
            try {
                timestamp = relativeTime(nodeCal);
            } catch (IllegalArgumentException ex) {
            }

            this.time = timestamp;
            // add eventClass to resolve workflow resource bundle
            this.method = node.getProperty("hippolog:eventMethod").getString() + ",class="
                    + node.getProperty("hippolog:eventClass").getString();
            this.user = node.getProperty("hippolog:eventUser").getString();
            this.nameModel = nameModel;
        } catch (RepositoryException ex) {
            JcrItemModel itemModel = eventNode.getItemModel();
            if (itemModel.exists()) {
                log.error("Could not parse event node " + itemModel.getPath());
            } else {
                log.warn("Event node retrieved that no longer exists");
            }
        }
    }

    public IWrapModel wrapOnAssignment(Component component) {
        return new AssignmentWrapper(component);
    }

    public String getObject() {
        throw new UnsupportedOperationException("Model " + getClass() + " does not support getObject(Object)");
    }

    public void setObject(String object) {
        throw new UnsupportedOperationException("Model " + getClass() + " does not support setObject(Object)");
    }

    public void detach() {
        if (nameModel != null) {
            nameModel.detach();
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
            return "one-minute";
        }
        if (nodeCal.after(fiveMinutesAgoCal)) {
            return "five-minutes";
        }
        if (nodeCal.after(tenMinutesAgoCal)) {
            return "ten-minutes";
        }
        if (nodeCal.after(halfHourAgoCal)) {
            return "half-hour";
        }
        if (nodeCal.after(hourAgoCal)) {
            return "hour";
        }
        if (nodeCal.before(thisMorningCal) && nodeCal.after(todayCal)) {
            return "morning";
        }
        if (nodeCal.before(thisAfternoonCal) && nodeCal.after(todayCal)) {
            return "afternoon";
        }
        if (nodeCal.before(thisEveningCal) && nodeCal.after(todayCal)) {
            return "evening";
        }
        if (nodeCal.after(yesterdayCal)) {
            return "yesterday";
        }
        return df.format(nodeCal);
    }

    private class AssignmentWrapper implements IWrapModel<String> {
        private static final long serialVersionUID = 1L;

        private final Component component;

        public AssignmentWrapper(Component component) {
            this.component = component;
        }

        /**
         * @see org.apache.wicket.model.IWrapModel#getWrappedModel()
         */
        public IModel<String> getWrappedModel() {
            return EventModel.this;
        }

        public String getObject() {
            try {
                if (nameModel != null) {
                    String name = nameModel.getObject();
                    name = StringEscapeUtils.escapeHtml(name);
                    StringResourceModel operationModel = new StringResourceModel(method, component, null,
                                                                                 new Object[]{user, name});
                    return new StringResourceModel(time, component, null, "").getString() + operationModel.getString();
                } else {
                    StringResourceModel operationModel = new StringResourceModel(method, component, null,
                                                                                 new Object[]{user});
                    return new StringResourceModel(time, component, null, "").getString() + operationModel.getString();
                }
            } catch (MissingResourceException mre) {
                return "Warning: could not translate Workflow operation " + method;
            }
        }

        public void detach() {
            EventModel.this.detach();
        }

        /**
         * @see org.apache.wicket.model.AbstractReadOnlyModel#setObject
         */
        public void setObject(String object) {
            throw new UnsupportedOperationException("Model " + getClass() + " does not support setObject(Object)");
        }

    }

}
