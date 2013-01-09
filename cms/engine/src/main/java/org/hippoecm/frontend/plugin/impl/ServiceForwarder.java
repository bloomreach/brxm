/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.service.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to forward services from one name to another one.
 */
public final class ServiceForwarder extends ServiceTracker<IClusterable> {

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
            return seThat.name.equals(this.name) && (seThat.service == this.service);
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

    private List<IClusterable> pending;
    private boolean started;

    ServiceForwarder(PluginManager mgr, Class<IClusterable> clazz, String source, String target) {
        super(clazz);

        this.pluginMgr = mgr;
        this.source = source;
        this.target = target;
        this.forwarded = new LinkedList<IClusterable>();
    }

    public void start() {
        pending = new LinkedList<IClusterable>();
        pluginMgr.registerTracker(this, source);
        started = true;
    }

    public void connect() {
        for (IClusterable service : pending) {
            forwardService(service, source);
        }
        pending = null;
    }

    public void disconnect() {
        pending = new ArrayList<IClusterable>(forwarded);
        for (IClusterable service : pending) {
            unforwardService(service, source);
        }
    }

    public void stop() {
        pluginMgr.unregisterTracker(this, source);
        pending = null;
    }

    @Override
    protected void onServiceAdded(IClusterable service, String name) {
        if (pending != null) {
            if (!started) {
                pending.add(service);
            }
            return;
        }
        forwardService(service, name);
    }
    
    protected void forwardService(IClusterable service, String name) {
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
        if (pending != null) {
            pending.remove(service);
            return;
        }
        unforwardService(service, name);
    }

    protected void unforwardService(IClusterable service, String name) {
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
