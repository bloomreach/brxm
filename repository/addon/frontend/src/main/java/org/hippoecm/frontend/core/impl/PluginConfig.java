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

import java.util.Hashtable;

import org.hippoecm.frontend.core.IPluginConfig;
import org.hippoecm.frontend.plugin.parameters.ParameterValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginConfig extends Hashtable<String, ParameterValue> implements IPluginConfig {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(PluginConfig.class);

    private Hashtable<ParameterValue, String> reverse;

    public PluginConfig() {
        reverse = new Hashtable<ParameterValue, String>();
    }

    public ParameterValue get(String key) {
        return super.get(key);
    }

    @Override
    public ParameterValue put(String key, ParameterValue value) {
        ParameterValue old = super.put(key, value);
        if (old != null) {
            reverse.put(old, null);
        }
        if (value != null) {
            reverse.put(value, key);
        }
        return old;
    }

    public String resolve(String name) {
        if (name != null) {
            if (reverse.containsKey(name)) {
                return reverse.get(name);
            }
        } else {
            log.error("Cannot resolve null name");
        }
        return null;
    }
}
