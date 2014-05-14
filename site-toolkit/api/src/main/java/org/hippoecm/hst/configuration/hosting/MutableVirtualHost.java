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

import java.util.List;

public interface MutableVirtualHost extends VirtualHost {

    /**
     * Adds the <code>virtualHost</code> as child to this {@link MutableVirtualHost}
     * @param virtualHost the {@link MutableVirtualHost} to add
     * @throws IllegalArgumentException if the <code>virtualHost</code> could not be added
     */
    void addVirtualHost(MutableVirtualHost virtualHost) throws IllegalArgumentException;
    
    /**
     * Add the <code>portMount</code> to this {@link MutableVirtualHost}
     * @param portMount the {@link MutablePortMount} to add
     * @throws IllegalArgumentException if the <code>portMount</code> could not be added
     */
    void addPortMount(MutablePortMount portMount) throws IllegalArgumentException;

    /**
     * @return the cms location (fully qualified URL) or <code>null</code> if not configured
     * @deprecated since 7.9.1 : Use {@link #getCmsLocations()} instead
     */
    @Deprecated
    String getCmsLocation();

    /**
     * @return the cms location(s) (fully qualified URL) or empty list if not configured
     */
    List<String> getCmsLocations();

}
