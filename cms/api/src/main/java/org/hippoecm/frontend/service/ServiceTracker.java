/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.service;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.IServiceTracker;

/**
 * Service tracker implementation that is aware of it's class.
 *
 * @param <S> the service interface class
 */
public class ServiceTracker<S extends IClusterable> implements IServiceTracker<S> {

    private static final long serialVersionUID = 1L;

    private Class<S> clazz;

    public ServiceTracker(Class<S> clazz) {
        this.clazz = clazz;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void addService(S service, String name) {
        if (clazz.isInstance(service)) {
            onServiceAdded(service, name);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void removeService(S service, String name) {
        if (clazz.isInstance(service)) {
            onRemoveService(service, name);
        }
    }

    protected void onServiceAdded(S service, String name) {
    }

    protected void onServiceChanged(S service, String name) {
    }

    protected void onRemoveService(S service, String name) {
    }

}
