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

public class RuntimeVirtualHost extends GenericVirtualHostWrapper {

    private final String hostName;
    private final String name;
    private final String hostGroupName;
    private final VirtualHost child;

    public RuntimeVirtualHost(final VirtualHost delegatee, final String serverName, final String hostGroupName) {
        super(delegatee);
        this.hostGroupName = hostGroupName;

        hostName = StringUtils.substringBefore(serverName, ":");
        final String[] hostNameSegments = hostName.split("\\.");

        final int position = hostNameSegments.length - 1;
        // add hosts in reverse order, thus last segment first

        name = hostNameSegments[position];
        if (position > 0) {
            child = new RuntimeVirtualHost(delegatee, hostName, hostNameSegments, position -1, hostGroupName);
        } else {
            child = null;
        }
    }

    public RuntimeVirtualHost(final VirtualHost delegatee, final String hostName, final String[] hostNameSegments, final int position, final String hostGroupName) {
        super(delegatee);
        this.hostName = hostName;
        this.hostGroupName = hostGroupName;
        name = hostNameSegments[position];

        if (position > 0) {
            child = new RuntimeVirtualHost(delegatee, hostName, hostNameSegments, position - 1, hostGroupName);
        } else {
            child = null;
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
        final PortMount portMount = super.getPortMount(portNumber);

        if (portMount == null) {
            return null;
        }
        return new PortMount() {
            @Override
            public int getPortNumber() {
                return portMount.getPortNumber();
            }

            @Override
            public Mount getRootMount() {
                if (portMount.getRootMount() == null) {
                    return null;
                }
                return new RuntimeMount(portMount.getRootMount(), RuntimeVirtualHost.this);
            }
        };

    }

    @Override
    public boolean isPortInUrl() {
        // TODO validate, is this optional? Opposed to typically localhost, we do not want the port number in the URL
        // TODO for auto created hosts, but we do inherit the show contextpath from the delegatee
        return false;
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
}
