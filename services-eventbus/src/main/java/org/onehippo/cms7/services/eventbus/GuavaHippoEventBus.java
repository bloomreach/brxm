/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.eventbus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.HippoAnnotationHandlerFinder;

import org.onehippo.cms7.event.HippoEvent;
import org.onehippo.cms7.services.HippoServiceException;
import org.onehippo.cms7.services.HippoServiceRegistration;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuavaHippoEventBus implements HippoEventBus {

    static final Logger log = LoggerFactory.getLogger(GuavaHippoEventBus.class);

    final ExecutorService executor = Executors.newSingleThreadExecutor();
    final AsyncEventBus eventBus = new AsyncEventBus(executor);

    final List<HippoServiceRegistration> listeners = Collections.synchronizedList(new ArrayList<HippoServiceRegistration>());

    private volatile int version = -1;
    
    public GuavaHippoEventBus() {
        try {
            Field finderField = EventBus.class.getDeclaredField("finder");
            finderField.setAccessible(true);
            finderField.set(eventBus, new HippoAnnotationHandlerFinder() {
                @Override
                protected boolean acceptMethod(final Object listener, final Annotation[] annotations, final Class<?> parameterType) {
                    return GuavaHippoEventBus.this.acceptMethod(listener, annotations, parameterType);
                }
            });
        } catch (NoSuchFieldException e) {
            throw new HippoServiceException("Unable to initialize event bus", e);
        } catch (IllegalAccessException e) {
            throw new HippoServiceException("Unable to initialize event bus", e);
        }
    }

    protected boolean acceptMethod(final Object listener, final Annotation[] annotations, final Class<?> parameterType) {
        return true;
    }

    public void destroy() {
        executor.shutdown();
    }

    @Override
    public void register(final Object listener) {
        eventBus.register(listener);
        log.warn("HippoEventBus method #register is deprecated, use whiteboard pattern instead");
    }

    @Override
    public void unregister(final Object listener) {
        eventBus.unregister(listener);
    }

    public void post(final Object event) {
        if (updateListenersNeeded()) {
            updateListeners();
        }
        if (event instanceof HippoEvent) {
            ((HippoEvent) event).sealEvent();
        }
        eventBus.post(event);
    }

    private boolean updateListenersNeeded() {
        if (version == HippoServiceRegistry.getVersion()) {
            return false;
        }
        return true;
    }

    private void updateListeners() {
        List<HippoServiceRegistration> registered = getServiceRegistrations();
        for (HippoServiceRegistration registration : registered) {
            if (!listeners.contains(registration)) {
                listeners.add(registration);
                eventBus.register(registration);
            }
        }
        Iterator<HippoServiceRegistration> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            HippoServiceRegistration registration = iterator.next();
            if (!registered.contains(registration)) {
                iterator.remove();
                eventBus.unregister(registration);
            }
        }
        version = HippoServiceRegistry.getVersion();
    }

    protected List<HippoServiceRegistration> getServiceRegistrations() {
        return HippoServiceRegistry.getRegistrations(HippoEventBus.class);
    }
}
