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
package org.hippoecm.frontend.plugins.admin.logout;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.UserSession;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.PluginDescriptor;

public class LogoutPlugin extends Plugin {
    private static final long serialVersionUID = 1L;
    
    public LogoutPlugin(PluginDescriptor pluginDescriptor, JcrNodeModel model, Plugin parentPlugin) {
        super(pluginDescriptor, model, parentPlugin);

        UserSession session = (UserSession) getSession();
        ValueMap credentials = session.getCredentials();
        String username = credentials.getString("username");
        
        add(new DialogLink("logout-dialog", "Logout", LogoutDialog.class, model,
        		pluginDescriptor.getIncoming(), getPluginManager().getChannelFactory()));        
        add(new Label("username", username));
    }

}
