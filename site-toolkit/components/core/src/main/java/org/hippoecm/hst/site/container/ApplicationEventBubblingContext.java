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
package org.hippoecm.hst.site.container;

import org.apache.commons.lang.mutable.MutableInt;

/**
 * ApplicationEventBubblingContext
 * <P>
 * This class allows to control ApplicationEvent bubbling count of Spring Framework's ApplicationContext.
 * The default ApplicationContext implementations (AbstractApplicationContext) bubble up the ApplicationEvent 
 * after handling the ApplicationEvent in the direction of child-to-parent, but ApplicationContexts of ModuleInstance 
 * must not bubble up the ApplicationEvents because ApplicationEvents must be propagated by the ComponentManager of 
 * the HST-2 Container in the direction of parent-to-child. e.g., HttpSession event publishing from the container
 * to component manager and to each add-on module instance.
 * </P>
 * <P>
 * By the way, AbstractApplicationContext of Spring Framework doesn't provide a way not to publish event to the parent 
 * application context. See AbstractApplicationContext#publishEvent(ApplicationEvent) for detail.
 * </P>
 * <P>
 * Therefore, by controlling the event bubbling count in thread-local when publishing events, we can overcome the limitation,
 * without allowing the application context of add-on modules to publish the events again to the parent application context.
 * </P>
 */
public class ApplicationEventBubblingContext {

    private static ThreadLocal<MutableInt> tlLimit = new ThreadLocal<MutableInt>();
    private static ThreadLocal<MutableInt> tlCount = new ThreadLocal<MutableInt>();

    private ApplicationEventBubblingContext() {

    }

    public static void reset(int limit) {
        setLimit(limit);

        MutableInt count = tlCount.get();

        if (count != null) {
            count.setValue(0);
        }
    }

    public static void clear() {
        tlLimit.remove();
        tlCount.remove();
    }

    public static void setLimit(int value) {
        MutableInt limit = tlLimit.get();

        if (limit != null) {
            limit.setValue(value);
        } else {
            tlLimit.set(new MutableInt(value));
        }
    }

    public static void bubble() {
        MutableInt count = tlCount.get();

        if (count != null) {
            count.increment();
        } else {
            tlCount.set(new MutableInt(1));
        }
    }

    public static boolean canBubble() {
        MutableInt limit = tlLimit.get();

        if (limit == null) {
            return true;
        }

        int limitValue = limit.intValue();

        MutableInt count = tlCount.get();

        int countValue = 0;

        if (count != null) {
            countValue = count.intValue();
        }

        return (countValue < limitValue);
    }
}
