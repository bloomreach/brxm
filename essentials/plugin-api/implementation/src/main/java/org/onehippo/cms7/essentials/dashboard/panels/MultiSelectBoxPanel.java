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

package org.onehippo.cms7.essentials.dashboard.panels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.ListMultipleChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.PropertyModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Select box wrapper. Items is added to the form, so there is no need to call {@code form.add(box)}
 * @version "$Id$"
 */
public class MultiSelectBoxPanel extends Panel {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(MultiSelectBoxPanel.class);
    private final Collection<EventListener<String>> listeners = new CopyOnWriteArrayList<>();
    @SuppressWarnings("UnusedDeclaration")
    private List<String> selectedItems;
    private List<String> items;
    private final ListMultipleChoice<String> selectBox;

    public MultiSelectBoxPanel(final String id, final String title, final Form<?> form, final Collection<String> model, final EventListener<String> listener) {
        this(id, title, form, model);
        addListener(listener);
    }

    public MultiSelectBoxPanel(final String id, final String title, final Form<?> form, final Collection<String> model, final Collection<EventListener<String>> listeners) {
        this(id, title, form, model);
        listeners.addAll(listeners);
    }

    public MultiSelectBoxPanel(final String id, final String title, final Form<?> form, final Collection<String> model) {
        super(id);
        items = new ArrayList<>();
        items.addAll(model);
        final PropertyModel<List<String>> listModel = new PropertyModel<>(this, "selectedItems");

        selectBox = new ListMultipleChoice<>("selectBox", listModel, items);

        selectBox.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                log.debug("@selected items {}", selectedItems);
                // notify listeners for changes
                for (EventListener<String> listener : listeners) {
                    listener.onSelected(target, items);
                }
            }
        });
        //############################################
        // ADD COMPONENTS
        //############################################

        selectBox.setOutputMarkupId(true);
        add(selectBox);
        add(new Label("title", title));
        form.add(this);
    }

    public void changeModel(final AjaxRequestTarget target, final Collection<String> newModel) {
        items.clear();
        if (newModel != null) {
            items.addAll(newModel);
        }
        target.add(selectBox);
    }

    public void removeListener(final EventListener<String> listener) {
        log.debug("@removing event listener {}", listener);
        listeners.remove(listener);
    }

    public void addListener(final EventListener<String> listener) {
        log.debug("@adding listener {}", listener);
        listeners.add(listener);
    }

    public List<String> getSelectedItems() {
        return selectedItems;
    }

    public ListMultipleChoice<String> getSelectBox() {
        return selectBox;
    }
}
