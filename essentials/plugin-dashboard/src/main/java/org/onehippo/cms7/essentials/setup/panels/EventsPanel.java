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

package org.onehippo.cms7.essentials.setup.panels;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.onehippo.cms7.essentials.dashboard.event.DisplayEvent;
import org.onehippo.cms7.essentials.dashboard.event.PluginEvent;
import org.onehippo.cms7.essentials.dashboard.event.listeners.MemoryPluginEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

/**
 * @version "$Id$"
 */
public class EventsPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(EventsPanel.class);
    @Inject
    private MemoryPluginEventListener listener;
    private final ListView<DisplayEvent> repeater;

    public EventsPanel(final String id) {
        super(id);
        //############################################
        // REPEATER 
        //############################################
        final Form<?> form = new Form("form");
        final List<DisplayEvent> eventsModelList = new LinkedList<>(listener.consumeEvents());

        repeater = new ListView<DisplayEvent>("repeater", eventsModelList) {
            private static final long serialVersionUID = 1L;

            protected void populateItem(final ListItem<DisplayEvent> item) {
                final PluginEvent pluginEvent = item.getModelObject();
                final Label eventMessage = new Label("eventMessage", new Model<>(pluginEvent.getMessage()));
                item.add(eventMessage);
                // TODO add model..
                final CheckBox undoCheckbox = new CheckBox("undoCheckbox", new PropertyModel<Boolean>(pluginEvent, "selected"));
                undoCheckbox.setVisible(false);
                item.add(undoCheckbox);
            }
        };

        repeater.setReuseItems(true);
        form.add(repeater);
        add(form);
        setOutputMarkupId(true);

    }


    @Override
    protected void onModelChanged() {
        final Queue<DisplayEvent> events = listener.consumeEvents();
        final List<DisplayEvent> modelObject = repeater.getModel().getObject();
        modelObject.clear();
        modelObject.addAll(events);
    }



}
