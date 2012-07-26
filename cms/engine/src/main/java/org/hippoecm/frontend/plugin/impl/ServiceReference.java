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
package org.hippoecm.frontend.plugin.impl;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.plugin.IServiceReference;

public class ServiceReference<T extends IClusterable> implements IServiceReference<T> {

    private static final long serialVersionUID = 1L;

    private PluginPage page;
    private String serviceId;

    ServiceReference(PluginPage page, String serviceId) {
        this.page = page;
        this.serviceId = serviceId;
    }

    public T getService() {
        PluginManager mgr = page.getPluginManager();
        return mgr.getService(this);
    }

    public String getServiceId() {
        return serviceId;
    }

    public void detach() {
    }

}
