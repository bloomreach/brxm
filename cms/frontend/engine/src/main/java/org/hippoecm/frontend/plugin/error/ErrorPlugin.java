/*
 * Copyright 2007 Hippo
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
package org.hippoecm.frontend.plugin.error;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.hippoecm.frontend.plugin.JcrEvent;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class ErrorPlugin extends Plugin {
    private static final long serialVersionUID = 1L;

    public ErrorPlugin(PluginDescriptor pluginDescriptor, Exception exception, String message) {
        super(pluginDescriptor, null, null);
        String errorMessage = "";
        if (exception != null) {
            errorMessage = exception.getClass().getName() + ": " + exception.getMessage();
        }
        if (exception != null &&  message != null) {
            errorMessage += "\n";
        }
        if (message != null) {
            errorMessage += message;
        }
        add(new MultiLineLabel("message", errorMessage));
    }

    public void update(AjaxRequestTarget target, JcrEvent jcrEvent) {
        // nothing much to do here
    }

}
