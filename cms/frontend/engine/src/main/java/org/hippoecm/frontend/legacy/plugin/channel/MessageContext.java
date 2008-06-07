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
package org.hippoecm.frontend.legacy.plugin.channel;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated use org.hippoecm.frontend.sa.* instead
 */
@Deprecated
public class MessageContext implements IClusterable {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(MessageContext.class);

    private List<Component> updates;
    private List<Entry> methods;

    private static class Entry implements IClusterable {
        private static final long serialVersionUID = 1L;

        Component component;
        String methodName;

        Entry(Component component, String method) {
            this.component = component;
            this.methodName = method;
        }

        Method getMethod() {
            try {
                return component.getClass().getMethod(methodName, AjaxRequestTarget.class);
            } catch (NoSuchMethodException ex) {
                return null;
            }
        }
    }

    public MessageContext() {
        this.updates = new LinkedList<Component>();
        this.methods = new LinkedList<Entry>();
    }

    public void apply(AjaxRequestTarget target) {
        if (target == null) {
            return;
        }

        Iterator<Component> components = updates.iterator();
        while (components.hasNext()) {
            Component component = components.next();
            if (component.findParent(Page.class) != null) {
                target.addComponent(component);
            }
        }

        try {
            Iterator<Entry> delegates = methods.iterator();
            while (delegates.hasNext()) {
                Entry entry = delegates.next();
                if (entry.component.findParent(Page.class) != null) {
                    Method method = entry.getMethod();
                    if (method != null) {
                        method.invoke(entry.component, target);
                    }
                }
            }
        } catch (InvocationTargetException ex) {
            // should not happen, as we've taken the method from the object
        } catch (IllegalArgumentException ex) {
            // should not happen, as we've taken the method from the object
        } catch (IllegalAccessException ex) {
            // should not happen, as we've taken the method from the object
        }
    }

    public void addRefresh(Component component) {
        this.updates.add(component);
    }

    public void addRefresh(Component component, String methodName) {
        this.methods.add(new Entry(component, methodName));
    }

}
