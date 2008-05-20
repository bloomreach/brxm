/*
 * Copyright 2008 Hippo
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
package org.hippoecm.frontend.sa.core;

import java.io.Serializable;

import org.hippoecm.frontend.sa.PluginPage;
import org.hippoecm.frontend.sa.core.impl.PluginManager;

public class ServiceReference<T extends Serializable> implements Serializable {
    private static final long serialVersionUID = 1L;

    private PluginPage page;
    private int id;

    public ServiceReference(PluginPage page, int id) {
        this.page = page;
        this.id = id;
    }

    public T getService() {
        PluginManager mgr = page.getPluginManager();
        return mgr.getService(this);
    }

    public PluginPage getPage() {
        return page;
    }

    public int getId() {
        return id;
    }
}
