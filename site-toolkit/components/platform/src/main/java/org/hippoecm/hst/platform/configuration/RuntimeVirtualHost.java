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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.GenericVirtualHostWrapper;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;

public class RuntimeVirtualHost extends GenericVirtualHostWrapper {

    private final VirtualHost delegatee;
    private final String hostName;
    private final String name;
    private final String hostGroupName;
    private final VirtualHost child;
    private final PortMount portMount;

    public RuntimeVirtualHost(final VirtualHost delegatee, final String serverName, final String hostGroupName) {
        this(delegatee, StringUtils.substringBefore(serverName, ":").split("\\."), hostGroupName);
    }

    private RuntimeVirtualHost(final VirtualHost delegatee, final String[] hostNameSegments, final String hostGroupName) {
        this(delegatee, "", hostNameSegments, hostNameSegments.length - 1, hostGroupName);
    }

    public RuntimeVirtualHost(final VirtualHost delegatee, final String hostNamePrefix, final String[] hostNameSegments, final int position, final String hostGroupName) {
        super(delegatee);
        this.delegatee = delegatee;
        if (hostNamePrefix.length() == 0) {
            hostName = hostNameSegments[position] + hostNamePrefix;
        } else {
            hostName = hostNameSegments[position] + "." + hostNamePrefix;
        }
        this.hostGroupName = hostGroupName;
        name = hostNameSegments[position];

        if (position > 0) {
            child = new RuntimeVirtualHost(delegatee, hostName, hostNameSegments, position - 1, hostGroupName);
            portMount = null;
        } else {
            child = null;
            // we can use '0' since we never really have port mounts, and '0' is the default catch all port
            final PortMount delegateePortMount = delegatee.getPortMount(0);

            final Mount rootMount = delegateePortMount.getRootMount();

            final RuntimeMount runtimeMount;
            if (rootMount instanceof ContextualizableMount) {
                runtimeMount = new RuntimeContextualizableMount((ContextualizableMount)rootMount, RuntimeVirtualHost.this);
            } else {
                runtimeMount = new RuntimeMount(rootMount, RuntimeVirtualHost.this);
            }

            this.portMount = new PortMount() {
                @Override
                public int getPortNumber() {
                    return delegateePortMount.getPortNumber();
                }

                @Override
                public Mount getRootMount() {
                    return runtimeMount;
                }
            };
        }
    }


    @Override
    public String getHostName() {
        return hostName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    @Override
    public PortMount getPortMount(final int portNumber) {
       return portMount;
    }

    @Override
    public boolean isPortInUrl() {
        // TODO validate, is this optional? Opposed to typically localhost, we do not want the port number in the URL
        // TODO for auto created hosts, but we do inherit the show contextpath from the delegatee
        return false;
    }

    @Override
    public String getScheme() {
        // TODO make this configurable? Most likely most be https
        return super.getScheme();
    }

    @Override
    public VirtualHost getChildHost(final String name) {
        if (child != null && child.getName().equals(name)) {
            return child;
        }
        return null;
    }

    @Override
    public List<VirtualHost> getChildHosts() {
        return child == null ? Collections.emptyList() : Collections.singletonList(child);
    }

    @Override
    public String toString() {
        return "RuntimeVirtualHost{" +
                "delegatee=" + delegatee +
                '}';
    }
}
