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

public class ExtensionRegistry {

    public enum ExtensionType {
        HST, OTHER
    }

    private static Map<String, ExtensionEvent> registry = new HashMap<>();

    /**
     * Register a extension event
     * @throws IllegalStateException if the extension already has been registered by its hst root
     * @param type the <code>WebAppType</code> to which the <code>ctx</code> belongs. Not allowed to be <code>null</code>
     */
    public synchronized static void register(final ExtensionEvent extensionEvent, final ExtensionType type) {
        if (registry.containsKey(extensionEvent.getHstRoot())) {
            throw new IllegalStateException("Hst root " + extensionEvent.getHstRoot() + " is already registered");
        }
        if (type == null) {
            throw new IllegalArgumentException("ExtensionType argument is not allowed to be null.");
        }
        Map<String, ExtensionEvent> newMap = new HashMap<>(registry);
        newMap.put(extensionEvent.getHstRoot(), extensionEvent);
        registry = Collections.unmodifiableMap(newMap);
    }

    /**
     * Unregister extension
     * @throws IllegalStateException if the extension has not been registered by its hst root
     * @param hstRoot the hst root to unregister
     */
    public synchronized static void unregister(final String hstRoot) {
        if (!registry.containsKey(hstRoot)) {
            throw new IllegalStateException("Hst root " + hstRoot + " is not registered");
        }
        Map<String, ExtensionEvent> newMap = new HashMap<>(registry);
        newMap.remove(hstRoot);
        registry = Collections.unmodifiableMap(newMap);
    }

    /**
     * @param hstRoot The hst root for which of the extension
     * @return the extension registered under the extension's hst root
     */
    public static ExtensionEvent getExtension(final String hstRoot) {
        return registry.get(hstRoot);
    }

    /**
     * @return unmodifiable map of all currently registered extensions mapped by their hst root
     */
    public static Map<String, ExtensionEvent> getHstRoots() {
        return registry;
    }
}