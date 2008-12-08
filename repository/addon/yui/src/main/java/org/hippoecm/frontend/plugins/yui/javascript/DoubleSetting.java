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
package org.hippoecm.frontend.plugins.yui.javascript;

import org.hippoecm.frontend.plugin.config.IPluginConfig;


public class DoubleSetting extends Setting<Double> {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    
    public DoubleSetting(String javascriptKey) {
        this(javascriptKey, null);
    }
    
    public DoubleSetting(String javascriptKey, Double defaultValue) {
        super(javascriptKey, defaultValue);
    }

    @Override
    protected Double getValueFromConfig(IPluginConfig config, Settings settings) {
        return config.getDouble(configKey);
    }

    public  Value<Double> newValue() {
        return new DoubleValue(defaultValue);
    }

    public void setFromString(String value, Settings settings) {
        set(Double.valueOf(value), settings);
    }

}
