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
package org.hippoecm.frontend;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of Wicket's {@link AjaxRequestTarget} that filters the list of {@link Component}s that
 * have been added.
 */
public class PluginRequestTarget extends AjaxRequestTarget implements AjaxRequestTarget.IListener {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger log = LoggerFactory.getLogger(PluginRequestTarget.class);

    private Set<Component> updates;
    private List<IListener> listeners;

    public PluginRequestTarget(Page page) {
        super(page);

        this.updates = new HashSet<Component>();

        super.addListener(this);
    }

    @Override
    public void addComponent(Component component) {
        this.updates.add(component);
    }

    @Override
    public void addListener(IListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Argument `listener` cannot be null");
        }

        if (listeners == null) {
            listeners = new LinkedList();
        }

        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    // implement AjaxRequestTarget.IListener

    public void onBeforeRespond(Map existing, AjaxRequestTarget target) {
        if (existing.size() > 0) {
            log.warn("Some components have already been added to the target.");
        }

        if (listeners != null) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                ((IListener) it.next()).onBeforeRespond(existing, this);
            }
        }

        Iterator<Component> components = updates.iterator();
        while (components.hasNext()) {
            Component component = components.next();
            if (!component.isVisibleInHierarchy()) {
                continue;
            }

            MarkupContainer parent = component.getParent();
            while (parent != null) {
                if (parent instanceof Page) {
                    super.addComponent(component);
                    break;
                } else if (updates.contains(parent)) {
                    break;
                }
                parent = parent.getParent();
            }
        }
    }

    public void onAfterRespond(Map map, IJavascriptResponse response) {
        if (listeners != null) {
            Iterator it = listeners.iterator();
            while (it.hasNext()) {
                ((IListener) it.next()).onAfterRespond(map, response);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PluginRequestTarget) {
            return super.equals(obj);
        }
        return false;
    }

}
