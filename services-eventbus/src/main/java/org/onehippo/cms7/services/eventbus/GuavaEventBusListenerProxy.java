/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.services.eventbus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Base class for dynamically generated proxy classes which are used as wrapper for Guava EventBus registration through
 * the {@link GuavaHippoEventBus}.
 * <p>
 *   The creation and caching of both generated proxy classes and instances is managed through the
 *   {@link org.onehippo.cms7.services.eventbus.GuavaEventBusListenerProxyFactory}.
 * </p>
 * @see GuavaHippoEventBus
 * @see GuavaEventBusListenerProxyFactory
 */
public abstract class GuavaEventBusListenerProxy implements Cloneable {

    private volatile Object listener;
    private ClassLoader cl;
    private Method[] methods;

    protected GuavaEventBusListenerProxy(Object listener, ClassLoader cl, Method[] methods) {
        this.listener = listener;
        this.cl = cl;
        this.methods = methods;
    }

    protected void handleEvent(int methodIndex, Object event) throws InvocationTargetException {
        ClassLoader lcl = cl;
        Method m = methods[methodIndex];
        Object ll = listener;
        // protect against concurrent called destroy
        if (listener != null) {
            ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(lcl);
                m.invoke(ll, event);
            } catch (IllegalArgumentException e) {
                throw new Error("Method rejected target/argument: " + event, e);
            } catch (IllegalAccessException e) {
                throw new Error("Method became inaccessible: " + event, e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof Error) {
                    throw (Error) e.getCause();
                }
                throw e;
            }
            finally {
                Thread.currentThread().setContextClassLoader(ccl);
            }
        }
    }

    GuavaEventBusListenerProxy clone(Object listener) {
        try {
            GuavaEventBusListenerProxy clone = (GuavaEventBusListenerProxy)super.clone();
            clone.listener = listener;
            return clone;
        } catch (CloneNotSupportedException e) {
            // will never happen
        }
        return null;
    }

    /**
     * Clears the proxy wrapped listener, its classloader and its wrapped methods.
     * The proxy can no longer be used after this, other than to unregister from the Guava EventBus!
     */
    void destroy() {
        listener = null;
        cl = null;
        for (int i = 0; i < methods.length; i++) {
            methods[i] = null;
        }
    }
}
