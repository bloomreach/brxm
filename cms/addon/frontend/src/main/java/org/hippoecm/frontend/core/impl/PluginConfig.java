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
package org.hippoecm.frontend.core.impl;

import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.core.IPluginConfig;

public class PluginConfig extends ValueMap implements IPluginConfig {
    private static final long serialVersionUID = 1L;

    public PluginConfig() {
    }

    public IPluginConfig getConfig(String key) {
        return (IPluginConfig) get(key);
    }

    public IPluginConfig[] getConfigArray(String key) {
        return (IPluginConfig[]) get(key);
    }
}
