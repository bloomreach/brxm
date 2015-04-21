/*
 * Copyright 2015 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

/**
 * The ServletContextRegistry provides a central registration of web applications their {@link ServletContext} using
 * their contextPath as key.
 * <p>
 * ServletContexts can/should be registered/unregistered via init/destroy methods of a {@link javax.servlet.Servlet}
 * or {@link javax.servlet.Filter}, or a {@link javax.servlet.ServletContextListener} its
 * contextInitialized/contextDestroyed event methods.
 * </p>
 * <p>
 * Registered ServletContexts can be looked up by their {@link #getContext(String) contextPath} or as an
 * unmodifiable {@link #getContexts() map} by their context paths.
 * </p>
 */
public final class ServletContextRegistry {

    private static Map<String, ServletContext> registry = new HashMap<>();

    /**
     * Register a {@link ServletContext}
     * @throws IllegalStateException if the ServletContext already has been registered by its contextPath
     * @param ctx the servletContext to register
     */
    public synchronized static void register(ServletContext ctx) {
        if (registry.containsKey(ctx.getContextPath())) {
            throw new IllegalStateException("ServletContext "+ctx.getContextPath()+" already registered");
        }
        Map<String, ServletContext> newMap = new HashMap<>(registry);
        newMap.put(ctx.getContextPath(), ctx);
        registry = Collections.unmodifiableMap(newMap);
    }

    /**
     * Unregister a {@link ServletContext}
     * @throws IllegalStateException if the ServletContext has not been registered by its contextPath
     * @param ctx the servletContext to unregister
     */
    public synchronized static void unregister(ServletContext ctx) {
        if (!registry.containsKey(ctx.getContextPath())) {
            throw new IllegalStateException("ServletContext "+ctx.getContextPath()+" not registered");
        }
        Map<String, ServletContext> newMap = new HashMap<>(registry);
        newMap.remove(ctx.getContextPath());
        registry = Collections.unmodifiableMap(newMap);
    }

    /**
     * @param contextPath The contextPath for which to lookup the ServletContext
     * @return the ServletContext registered under the parameter contextPath
     */
    public static ServletContext getContext(String contextPath) {
        return registry.get(contextPath);
    }

    /**
     * @return all currently registered ServletContexts mapped by their contextPath
     */
    public static Map<String, ServletContext> getContexts() {
        return registry;
    }
}
