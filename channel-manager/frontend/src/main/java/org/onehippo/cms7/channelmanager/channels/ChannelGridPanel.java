/**
 * Copyright 2011 Hippo
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

package org.onehippo.cms7.channelmanager.channels;

import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Ext Grid Panel for Channels Listing.
 */
@ExtClass("Hippo.ChannelManager.ChannelGridPanel")
public class ChannelGridPanel extends ExtPanel {

    private static final Logger log = LoggerFactory.getLogger(ChannelGridPanel.class);
    private ChannelStore store;

    public ChannelGridPanel() {
        super();
        add(JavascriptPackageResource.getHeaderContribution(ChannelGridPanel.class,
                "Hippo.ChannelManager.ChannelGridPanel.js"));
        List<ExtField> fieldList = new ArrayList<ExtField>();
        fieldList.add(new ExtField("title"));
        this.store = new ChannelStore(fieldList);
        add(this.store);
    }

    @Override
    protected void preRenderExtHead(StringBuilder js) {
        store.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }


    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("store", new JSONIdentifier(this.store.getJsObjectId()));
    }

    public ChannelStore getStore() {
        return this.store;
    }

}
