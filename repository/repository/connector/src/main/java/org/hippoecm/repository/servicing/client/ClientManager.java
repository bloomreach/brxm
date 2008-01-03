/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.servicing.client;

import org.apache.jackrabbit.rmi.client.ClientObject;

/* FIXME: [BvH] the decorating layer it probably not the right point for
 * most of this functionality
 */

public abstract class ClientManager extends ClientObject {

    protected ClientManager(LocalServicingAdapterFactory factory) {
        super(factory);
    }

    /**
     * Utility routine to set the thread context to the repository class loader.
     * Call it when making an RMI call and an object could be returned whose
     * class resides in the repository.
     */
    protected ClassLoader setContextClassLoader() {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        LocalServicingAdapterFactory factory = (LocalServicingAdapterFactory) getFactory();
        Thread.currentThread().setContextClassLoader(factory.getClassLoader());
        return current;
    }

}
