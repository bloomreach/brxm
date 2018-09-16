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
package org.onehippo.cms7.services.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.onehippo.cms7.services.HippoServiceException;
import org.onehippo.cms7.services.WhiteboardServiceRegistry;

public class HippoWebappContextRegistry extends WhiteboardServiceRegistry<HippoWebappContext> {

    private static final HippoWebappContextRegistry INSTANCE = new HippoWebappContextRegistry();

    private static Map<String, HippoWebappContext> contextMap = new ConcurrentHashMap<>();

    private HippoWebappContextRegistry() {
    }

    public static HippoWebappContextRegistry get() {
        return INSTANCE;
    }

    @Override
    public synchronized void register(final HippoWebappContext serviceObject) throws HippoServiceException {
        String contextPath = serviceObject.getServletContext().getContextPath();
        if (contextMap.containsKey(contextPath)) {
            throw new HippoServiceException(
                    String.format("HippoWebappContext with context path %s is already registered", contextPath));
        }
        super.register(serviceObject);
        contextMap.put(contextPath, serviceObject);
    }

    @Override
    public synchronized boolean unregister(final HippoWebappContext serviceObject) {
        final boolean removed = super.unregister(serviceObject);
        contextMap.remove(serviceObject.getServletContext().getContextPath());
        return removed;
    }

    public HippoWebappContext getContext(final String contextPath) {
        return contextMap.get(contextPath);
    }
}
