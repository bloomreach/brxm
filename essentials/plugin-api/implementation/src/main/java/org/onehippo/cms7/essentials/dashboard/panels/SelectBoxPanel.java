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
public class SelectBoxPanel<T> extends Panel {

    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(SelectBoxPanel.class);
    private final Collection<EventListener<T>> listeners = new CopyOnWriteArrayList<>();
    @SuppressWarnings("UnusedDeclaration")
    private List<T> selectedItems;
    private List<T> items;

    public SelectBoxPanel(final String id, final String title, final Form<?> form, final Collection<T> model, final EventListener<T> listener) {
        this(id, title, form, model);
        addListener(listener);
    }

    public SelectBoxPanel(final String id, final String title, final Form<?> form, final Collection<T> model, final Collection<EventListener<T>> listeners) {
        this(id, title, form, model);
        listeners.addAll(listeners);
    }

    public SelectBoxPanel(final String id, final String title,  final Form<?> form, final Collection<T> model) {

        super(id);

        items = new ArrayList<>();
        items.addAll(model);
        final PropertyModel<List<T>> listModel = new PropertyModel<>(this, "selectedItems");
        final ListMultipleChoice<T> selectBox = new ListMultipleChoice<>("selectBox", listModel, items);

        selectBox.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(final AjaxRequestTarget target) {
                log.debug("@selected items {}", selectedItems);
                // notify listeners for changes
                for (EventListener<T> listener : listeners) {
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

    public void changeModel(final AjaxRequestTarget target, final Collection<T> newModel) {
        items.clear();
        if (newModel != null) {
            items.addAll(newModel);
        }
        target.add(this);
    }

    public void removeListener(final EventListener<T> listener) {
        log.debug("@removing event listener {}", listener);
        listeners.remove(listener);
    }

    public void addListener(final EventListener<T> listener) {
        log.debug("@adding listener {}", listener);
        listeners.add(listener);
    }

    public List<T> getSelectedItems() {
        return selectedItems;
    }
}
