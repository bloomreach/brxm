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
package org.hippoecm.frontend.legacy.plugin.error;

import java.util.Map;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;

/**
 * @deprecated use org.hippoecm.frontend.sa.plugin.error.* instead
 */
@Deprecated
public class ErrorPlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public ErrorPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, null);
        Map<String, Object> map = model.getMapRepresentation();
        String message = (String) map.get("error");
        if(message == null) {
            message = "An error occurred.  No further details available";
        }
        add(new MultiLineLabel("message", message));
    }

    @Override
    public void addChildren() {

    }

}
