/*
 *  Copyright 2019 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You
 *  may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hippoecm.hst.configuration.GenericMountWrapper;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;

import static java.util.Collections.unmodifiableList;

public class RuntimeMount extends GenericMountWrapper {

    private final VirtualHost virtualHost;
    private final Mount parent;
    private Map<String, Mount> children = new HashMap<>();
    private List<Mount> childrenList;

    public RuntimeMount(final Mount delegatee, final VirtualHost virtualHost) {
        this(delegatee, virtualHost, null);
    }

    public RuntimeMount(final Mount delegatee, final VirtualHost virtualHost, final Mount parent) {
        super(delegatee);
        this.virtualHost = virtualHost;
        this.parent = parent;
        delegatee.getChildMounts().forEach(child ->
                children.put(child.getName(), new RuntimeMount(child, virtualHost, RuntimeMount.this))
        );
        childrenList = unmodifiableList(new ArrayList<>(children.values()));
    }


    @Override
    public Mount getParent() {
        return parent;
    }

    @Override
    public List<Mount> getChildMounts() {
        return childrenList;
    }

    @Override
    public Mount getChildMount(final String name) {
        return children.get(name);
    }

    @Override
    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

}
