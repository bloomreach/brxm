/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.service.preferences;

import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class PreferencesStorePlugin extends Plugin implements IPreferencesStore {
    private static final long serialVersionUID = 1L;
    
    
    private PreferencesStore store;
    
    public PreferencesStorePlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        store = new PreferencesStore();
        
        context.registerService(this, IPreferencesStore.SERVICE_ID);
    }

    public boolean getBoolean(String context, String name) {
        return store.getBoolean(context, name);
    }

    public int getInt(String context, String name) {
        return store.getInt(context, name);
    }

    public String getString(String context, String name) {
        return store.getString(context, name);
    }

    public void set(String context, String name, String value) {
        store.set(context, name, value);
    }

    public void set(String context, String name, int value) {
        store.set(context, name, value);
    }

    public void set(String context, String name, boolean value) {
        store.set(context, name, value);        
    }
    
}
