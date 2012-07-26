/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.editor.template;

import java.util.Iterator;
import java.util.Map;

import org.hippoecm.frontend.model.ocm.IStore;
import org.hippoecm.frontend.model.ocm.StoreException;
import org.hippoecm.frontend.plugin.config.IClusterConfig;

public class TemplateLocator implements ITemplateLocator {

    private static final long serialVersionUID = 1L;

    private IStore[] stores;

    public TemplateLocator(IStore[] stores) {
        this.stores = stores;
    }

    public IClusterConfig getTemplate(Map<String, Object> criteria) throws StoreException {
        for (int i = 0; i < stores.length; i++) {
            Iterator<IClusterConfig> iter = stores[i].find(criteria);
            if (iter.hasNext()) {
                return iter.next();
            }
        }
        throw new StoreException("Could not find template");
    }

}
