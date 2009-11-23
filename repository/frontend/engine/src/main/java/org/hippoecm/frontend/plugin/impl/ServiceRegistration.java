package org.hippoecm.frontend.plugin.impl;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.plugin.impl.PluginManager.RefCount;

class ServiceRegistration implements IClusterable {
    private static final long serialVersionUID = 1L;

    PluginManager mgr;
    IClusterable service;
    String id;
    List<String> names;

    ServiceRegistration(PluginManager mgr, IClusterable service, String id, String name) {
        this.mgr = mgr;
        this.service = service;
        this.id = id;
        this.names = new LinkedList<String>();
        names.add(name);
    }

    void addName(String name) {
        this.names.add(name);
    }

    void removeName(String name) {
        this.names.remove(name);
    }

    void notifyTrackers() {
        mgr.internalRegisterService(service, id);
        for (String name : names) {
            Map.Entry<Integer, RefCount> entry = mgr.internalGetReference(service);
            entry.getValue().addRef();
            mgr.internalRegisterService(service, name);
        }
    }

    void cleanup() {
        mgr.cleanup(service);
    }
}
