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
package org.hippoecm.frontend.plugin.error;

import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

public class ErrorPlugin extends RenderPlugin {

    private static final long serialVersionUID = 1L;

    public static final String ERROR_MESSAGE = "error.message";

    public ErrorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        String message = config.getString(ERROR_MESSAGE);
        if(message == null) {
            message = "An error occurred.  No further details available";
        }
        add(new MultiLineLabel("message", message));
    }

}
