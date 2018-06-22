/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.channelmanager.channels;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.ChannelManagerHeaderItem;
import org.onehippo.cms7.channelmanager.ExtStoreFuture;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.ExtProperty;
import org.wicketstuff.js.ext.util.JSONIdentifier;

@ExtClass("Hippo.ChannelManager.ChannelIconPanel")
public class ChannelIconPanel extends ExtPanel {

    public static final String CHANNEL_ICON_PANEL_JS = "ChannelIconPanel.js";

    private ChannelStore store;

    @ExtProperty
    private String userId;

    public ChannelIconPanel(IPluginConfig channelListConfig, ExtStoreFuture storeFuture) {
        this.store = (ChannelStore) storeFuture.getStore();
        this.userId = UserSession.get().getJcrSession().getUserID();
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);

        response.render(ChannelManagerHeaderItem.get());
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("store", new JSONIdentifier(this.store.getJsObjectId()));
    }
    
}
