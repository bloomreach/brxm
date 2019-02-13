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

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.configuration.site.HstSite;
import org.onehippo.cms7.services.hst.Channel;

import static java.util.Collections.unmodifiableList;

public class RuntimeMount extends GenericMountWrapper {

    private final Mount delegatee;
    private final VirtualHost virtualHost;
    private final Mount parent;
    private final Map<String, Mount> children = new HashMap<>();
    private final List<Mount> childrenList;
    private final HstSite hstSite;

    public RuntimeMount(final Mount delegatee, final VirtualHost virtualHost) {
        this(delegatee, virtualHost, null);
    }

    public RuntimeMount(final Mount delegatee, final VirtualHost virtualHost, final Mount parent) {
        super(delegatee);
        this.delegatee = delegatee;
        this.virtualHost = virtualHost;
        this.parent = parent;

        final HstSite delegateeSite = delegatee.getHstSite();
        if (delegateeSite != null) {
            hstSite = new RuntimeHstSite(delegateeSite, this);
        } else {
            hstSite = null;
        }

        delegatee.getChildMounts().forEach(child ->
                {
                    if (child instanceof ContextualizableMount) {
                        children.put(child.getName(), new RuntimeContextualizableMount((ContextualizableMount)child, virtualHost, RuntimeMount.this) {
                        });
                    } else {
                        children.put(child.getName(), new RuntimeMount(child, virtualHost, RuntimeMount.this));
                    }
                }

        );
        childrenList = unmodifiableList(new ArrayList<>(children.values()));
    }

    public HstSite getHstSite() {
        return hstSite;
    }

    @Override
    public Channel getChannel() {
        if (hstSite == null) {
            return null;
        }
        return hstSite.getChannel();
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
    public boolean isPortInUrl() {
        return virtualHost.isPortInUrl();
    }

    @Override
    public String getScheme() {
        return virtualHost.getScheme();
    }

    @Override
    public boolean isExplicit() {
        // although you might argue that a runtime created mount is an implicit mount, we need to know in the
        // channel mngr when we are dealing with an explicit configured Mount or not, hence we request the delegatee
        // I know we don't need to override #isExplicit here but only do this for the sake of the comment above
        return super.isExplicit();
    }

    @Override
    public VirtualHost getVirtualHost() {
        return virtualHost;
    }

    public Mount getDelegatee() {
        return delegatee;
    }


    @Override
    public String toString() {
        return "RuntimeMount{" +
                "delegatee=" + delegatee +
                '}';
    }

}
