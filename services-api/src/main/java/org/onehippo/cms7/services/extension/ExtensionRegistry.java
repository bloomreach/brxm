/*
 *  Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.services.extension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

public class ExtensionRegistry {

    public enum ExtensionType {
        HST, OTHER
    }

    private static Map<String, ExtensionEvent> registry = new HashMap<>();

    /**
     * Register a site's classloader
     * @throws IllegalStateException if the ServletContext already has been registered by its contextPath
     * @param ctx the servletContext to register
     * @param type the <code>WebAppType</code> to which the <code>ctx</code> belongs. Not allowed to be <code>null</code>
     */
    public synchronized static void register(final ServletContext ctx, final ExtensionEvent extensionEvent, final ExtensionType type) {
        if (registry.containsKey(ctx.getContextPath())) {
            throw new IllegalStateException("ServletContext "+ctx.getContextPath()+" already registered");
        }
        if (type == null) {
            throw new IllegalArgumentException("ExtensionType argument is not allowed to be null.");
        }
        Map<String, ExtensionEvent> newMap = new HashMap<>(registry);
        newMap.put(ctx.getContextPath(), extensionEvent);
        registry = Collections.unmodifiableMap(newMap);
    }

    /**
     * Unregister site
     * @throws IllegalStateException if the ServletContext has not been registered by its contextPath
     * @param ctx the servletContext to unregister
     */
    public synchronized static void unregister(final ServletContext ctx) {
        if (!registry.containsKey(ctx.getContextPath())) {
            throw new IllegalStateException("ServletContext "+ctx.getContextPath()+" not registered");
        }
        Map<String, ExtensionEvent> newMap = new HashMap<>(registry);
        newMap.remove(ctx.getContextPath());
        registry = Collections.unmodifiableMap(newMap);
    }

    /**
     * @param contextPath The contextPath for which to lookup the site
     * @return the site's Classloader registered under the parameter contextPath
     */
    public static ExtensionEvent getContext(final String contextPath) {
        return registry.get(contextPath);
    }

    /**
     * @return unmodifiable map of all currently registered site's classloaders mapped by their contextPath
     */
    public static Map<String, ExtensionEvent> getContexts() {
        return registry;
    }
}