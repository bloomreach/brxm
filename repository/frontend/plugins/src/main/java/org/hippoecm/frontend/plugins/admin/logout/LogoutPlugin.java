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
package org.hippoecm.frontend.plugins.admin.logout;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.util.value.ValueMap;
import org.hippoecm.frontend.legacy.model.IPluginModel;
import org.hippoecm.frontend.legacy.plugin.Plugin;
import org.hippoecm.frontend.legacy.plugin.PluginDescriptor;
import org.hippoecm.frontend.legacy.plugin.channel.Notification;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.session.UserSession;

/**
 * @deprecated use org.hippoecm.frontend.plugins.logout.* instead
 */
@Deprecated
public class LogoutPlugin extends Plugin {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;

    public LogoutPlugin(PluginDescriptor pluginDescriptor, IPluginModel model, Plugin parentPlugin) {
        super(pluginDescriptor, new JcrNodeModel(model), parentPlugin);

        UserSession session = (UserSession) getSession();
        ValueMap credentials = session.getCredentials();
        String username = credentials.getString("username");

        add(new LogoutLink("logout-link", "Logout", LogoutDialog.class, (JcrNodeModel) getModel(), getTopChannel(),
                getPluginManager().getChannelFactory()));
        add(new Label("username", username));
    }

    @Override
    public void receive(Notification notification) {
        if ("logout".equals(notification.getOperation())) {
            UserSession userSession = (UserSession) getSession();
            userSession.logout();
        }
        super.receive(notification);
    }
}
