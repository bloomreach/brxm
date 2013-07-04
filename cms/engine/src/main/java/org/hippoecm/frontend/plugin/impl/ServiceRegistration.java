/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.wicket.util.io.IClusterable;
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
        Map.Entry<Integer, RefCount> entry = mgr.internalGetReference(service);
        for (String name : names) {
            if (name != null) {
                entry.getValue().addRef();
                mgr.internalRegisterService(service, name);
            }
        }
    }

    void cleanup() {
        mgr.cleanup(service);
    }
}
