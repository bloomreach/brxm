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

package org.hippoecm.hst.plugins.frontend.editor.dialogs;

import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;

public abstract class HstPickerConfig extends JavaPluginConfig {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    public HstPickerConfig(IPluginConfig config) {
        super(config);

        if (getPluginConfig("cluster.options") == null) {
            put("cluster.options", new JavaPluginConfig("cluster.options"));
        }

        IPluginConfig clusterOpts = getPluginConfig("cluster.options");
        if (!clusterOpts.containsKey("content.path")) {
            IPluginConfig cc = new JavaPluginConfig(clusterOpts);
            cc.put("content.path", getPath());
            put("cluster.options", cc);
        }
    }

    protected abstract String getPath();

}
