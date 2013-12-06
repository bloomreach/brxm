/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.beanwriter.gui;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.model.BeanWriterLogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id$"
 */
public class BeanWriterActionPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(BeanWriterActionPanel.class);
    private ListView<BeanWriterLogEntry> repeater;
    private List<BeanWriterLogEntry> displayItems;

    public BeanWriterActionPanel(final String id, final  List<BeanWriterLogEntry> displayItems) {
        super(id);

        this.displayItems = displayItems;
        setDefaultModel(new PropertyModel<>(this, "displayItems"));
        repeater = new ListView<BeanWriterLogEntry>("repeater", new PropertyModel<List<BeanWriterLogEntry>>(this, "displayItems")) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(final ListItem<BeanWriterLogEntry> item) {
                final BeanWriterLogEntry pluginEvent = item.getModelObject();
                final Label eventMessage = new Label("message", new Model<>(pluginEvent.getMessage()));
                item.add(eventMessage);
                eventMessage.setEscapeModelStrings(false);
            }
        };
        repeater.setReuseItems(true);
        repeater.setOutputMarkupId(true);
        setOutputMarkupId(true);
        add(repeater);

    }


    public void replaceModel(final List<BeanWriterLogEntry> displayItems) {

        displayItems.clear();
        displayItems.addAll(new LinkedList<>(displayItems));
        repeater.modelChanged();
        modelChanged();
    }


}
