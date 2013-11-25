/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.configuration.hosting;

import java.util.Map;

public interface MutableVirtualHosts extends VirtualHosts {

    /**
     * Add this mount for lookup through {@link #getMountByGroupAliasAndType(String, String, String)}
     * @param mount
     */
    void addMount(Mount mount);
    
    /**
     * Adds the <code>virtualHost</code> as child to this {@link MutableVirtualHosts}
     * @param virtualHost the {@link MutableVirtualHost} to add
     * @throws IllegalArgumentException if the <code>virtualHost</code> could not be added
     */
    void addVirtualHost(MutableVirtualHost virtualHost) throws IllegalArgumentException;
 
    /**
     * The root virtualhosts are the first segment of a host. For example just 'com', or just 'org'. In case
     * of an IP adres, for for example the hostName 127.0.0.1, the root virtualhost will be '1' 
     * @return all the root virtualhosts by hostgroup
     */
    Map<String, Map<String, MutableVirtualHost>> getRootVirtualHostsByGroup();
}
