/*
 *  Copyright 2012 Hippo.
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
package org.onehippo.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Generic Hippo event bus.  Specific implementations can dispatch events based on metadata of the listener and event
 * objects.  (see e.g. the guava implementation)
 */
public abstract class HippoEventBus {

    private static final String HIPPO_EVENT_BUS_BINDER_CLASS = "org.onehippo.event.impl.HippoEventBusBinder";
    private static final String HIPPO_EVENT_BUS_BINDER_METHOD = "getInstance";

    private static final Map<ClassLoader, HippoEventBus> EVENT_BUS_MAP = new WeakHashMap<ClassLoader, HippoEventBus>();

    private static HippoEventBus getContextEventBus(ClassLoader classLoader) {
        try {
            Class<?> eventbusbinder = classLoader.loadClass(HIPPO_EVENT_BUS_BINDER_CLASS);
            Method method = eventbusbinder.getMethod(HIPPO_EVENT_BUS_BINDER_METHOD);
            return (HippoEventBus) method.invoke(null);
        } catch (ClassNotFoundException e) {
            throw new HippoEventException("Unable to find event bus to register " + HIPPO_EVENT_BUS_BINDER_CLASS, e);
        } catch (NoSuchMethodException e) {
            throw new HippoEventException("Unable to find event bus binder method " + HIPPO_EVENT_BUS_BINDER_METHOD, e);
        } catch (InvocationTargetException e) {
            throw new HippoEventException("Event bus binding failed.", e);
        } catch (IllegalAccessException e) {
            throw new HippoEventException("Not allowed to bind event bus.", e);
        }
    }

    protected HippoEventBus() {
    }

    protected void doRegister(Object listener) {
    }

    protected void doUnregister(Object listener) {
    }

    protected void doPost(Object event) {
    }

    public static void register(Object listener) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        HippoEventBus heb = getContextEventBus(cl);
        synchronized (HippoEventBus.class) {
            EVENT_BUS_MAP.put(cl, heb);
        }
        heb.doRegister(listener);
    }

    public static void unregister(Object listener) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        HippoEventBus heb = getContextEventBus(cl);
        heb.doUnregister(listener);
    }

    public static void post(Object event) {
        List<HippoEventBus> busses;
        synchronized (HippoEventBus.class) {
            busses = new ArrayList<HippoEventBus>(EVENT_BUS_MAP.values());
        }
        for (HippoEventBus bus : busses) {
            bus.doPost(event);
        }
    }
}
