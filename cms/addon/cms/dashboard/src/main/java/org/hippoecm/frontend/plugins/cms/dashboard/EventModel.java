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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.model.IComponentAssignedModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.frontend.i18n.model.NodeTranslator;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventModel implements IComponentAssignedModel {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(EventModel.class);

    private DateFormat df;
    private String time;
    private String method;
    private String user;
    private JcrNodeModel nodeModel;

    public EventModel(JcrNodeModel eventNode) {
        this(eventNode, null);
    }

    public EventModel(JcrNodeModel eventNode, JcrNodeModel targetNode) {
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
            this.method = node.getProperty("hippolog:eventMethod").getString();
            this.user = node.getProperty("hippolog:eventUser").getString();
            this.nodeModel = targetNode;
        } catch (RepositoryException ex) {
            log.error("Could not parse event node " + eventNode.getItemModel().getPath());
        }
    }

    public IWrapModel wrapOnAssignment(Component component) {
        return new AssignmentWrapper(component);
    }

    public Object getObject() {
        throw new UnsupportedOperationException("Model " + getClass() + " does not support getObject(Object)");
    }

    public void setObject(Object object) {
        throw new UnsupportedOperationException("Model " + getClass() + " does not support setObject(Object)");
    }

    public void detach() {
        if (nodeModel != null) {
            nodeModel.detach();
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

    private class AssignmentWrapper implements IWrapModel {
        private static final long serialVersionUID = 1L;

        private final Component component;

        public AssignmentWrapper(Component component) {
            this.component = component;
        }

        /**
         * @see org.apache.wicket.model.IWrapModel#getWrappedModel()
         */
        public IModel getWrappedModel() {
            return EventModel.this;
        }

        public Object getObject() {
            if (nodeModel != null) {
                String name = (String) new NodeTranslator(nodeModel).getNodeName().getObject();
                return new StringResourceModel(time, component, null, "").getString()
                        + new StringResourceModel(method, component, null, new Object[] { user, name }).getString();
            } else {
                return new StringResourceModel(time, component, null, "").getString()
                        + new StringResourceModel(method, component, null, new Object[] { user }).getString();
            }
        }

        public void detach() {
            EventModel.this.detach();
        }

        /**
         * @see org.apache.wicket.model.AbstractReadOnlyModel#setObject()
         */
        public void setObject(Object object) {
            throw new UnsupportedOperationException("Model " + getClass() + " does not support setObject(Object)");
        }

    }

}
