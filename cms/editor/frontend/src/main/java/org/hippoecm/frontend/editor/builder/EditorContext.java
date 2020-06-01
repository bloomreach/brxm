/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.editor.builder;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;

public class EditorContext implements IClusterable {

    private static final long serialVersionUID = 1L;

    public static final String MODE = "mode";
    
    private IPluginConfig config;

    public enum Mode {
        VIEW, EDIT
    }

    public EditorContext(IPluginContext context, IPluginConfig config) {
        this.config = config;
    }

    public Mode getMode() {
        if ("view".equals(config.getString(MODE, "view"))) {
            return Mode.VIEW;
        }
        return Mode.EDIT;
    }
    
}
