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
package org.hippoecm.frontend.plugin;

import org.apache.wicket.IClusterable;
import org.apache.wicket.Page;
import org.apache.wicket.model.IDetachable;

/**
 * A reference to a service.  This identifies a service by the service id assigned to it by the plugin manager.
 * <p>
 * The service reference is cross-page safe.  I.e. if a service lives on a different {@link Page} B, then this
 * reference can still be serialized on {@link Page} A.
 * <p>
 * This interface is not intended to be implemented by clients, the plugin framework will make instances available.
 *
 * @param <T> the service interface of the referenced service
 */
public interface IServiceReference<T extends IClusterable> extends IDetachable {
    final static String SVN_ID = "$Id$";

    /**
     * The referenced service.
     *
     * @return the service
     */
    T getService();

    /**
     * The id for the service.
     *
     * @return the id of the service
     */
    String getServiceId();

}
