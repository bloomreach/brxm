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
package org.hippoecm.frontend.plugin.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to forward services from one name to another one.
 */
public final class ServiceForwarder extends ServiceTracker<IClusterable> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(ServiceForwarder.class.getName());

    private static final ThreadLocal<Set<StackEntry>> threadLocal = new ThreadLocal<Set<StackEntry>>();

    private static class StackEntry {
        String name;
        IClusterable service;

        StackEntry(String name, IClusterable service) {
            this.name = name;
            this.service = service;
        }

        @Override
        public boolean equals(Object that) {
            if (!(that instanceof StackEntry)) {
                return false;
            }
            StackEntry seThat = (StackEntry) that;
            return new EqualsBuilder().append(seThat.name, this.name).append(seThat.service, this.service).isEquals();
        }

        @Override
        public int hashCode() {
            return (service.hashCode() << 4) + name.hashCode();
        }
    }

    private PluginManager pluginMgr;
    private String source;
    private String target;
    private List<IClusterable> forwarded;

    ServiceForwarder(PluginManager mgr, Class<IClusterable> clazz, String source, String target) {
        super(clazz);

        this.pluginMgr = mgr;
        this.source = source;
        this.target = target;
        this.forwarded = new LinkedList<IClusterable>();
    }

    public void start() {
        pluginMgr.registerTracker(this, source);
    }

    public void stop() {
        pluginMgr.unregisterTracker(this, source);
    }

    @Override
    protected void onServiceAdded(IClusterable service, String name) {
        Set<StackEntry> stack = threadLocal.get();
        if (stack == null) {
            threadLocal.set(stack = new HashSet<StackEntry>());
        }
        try {
            // detect recursion; forwarded services may be forwarded yet again,
            // but shouldn't be registered twice under the same name
            StackEntry targetEntry = new StackEntry(target, service);
            if (!stack.contains(targetEntry)) {
                StackEntry sourceEntry = new StackEntry(name, service);
                stack.add(sourceEntry);
                if (log.isDebugEnabled()) {
                    log.debug("Forwarding " + service + " from " + source + " to " + target);
                }
                pluginMgr.registerService(service, target);
                forwarded.add(service);
                stack.remove(sourceEntry);
            }
        } finally {
            if (stack.size() == 0) {
                threadLocal.remove();
            }
        }
    }

    @Override
    protected void onRemoveService(IClusterable service, String name) {
        if (forwarded.contains(service)) {
            forwarded.remove(service);
            Set<StackEntry> stack = threadLocal.get();
            if (stack == null) {
                threadLocal.set(stack = new HashSet<StackEntry>());
            }
            try {
                StackEntry targetEntry = new StackEntry(target, service);
                if (!stack.contains(targetEntry)) {
                    StackEntry sourceEntry = new StackEntry(name, service);
                    stack.add(sourceEntry);
                    if (log.isDebugEnabled()) {
                        log.debug("Removing " + service + " from " + target + " (source: " + source + ")");
                    }
                    pluginMgr.unregisterService(service, target);
                    stack.remove(sourceEntry);
                }
            } finally {
                if (stack.size() == 0) {
                    threadLocal.remove();
                }
            }
        }
    }

}
