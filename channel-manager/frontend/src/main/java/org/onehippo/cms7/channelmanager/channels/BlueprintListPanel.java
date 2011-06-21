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
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import java.util.ArrayList;
import java.util.List;

@ExtClass("Hippo.ChannelManager.BlueprintListPanel")
public class BlueprintListPanel extends ExtPanel {

    private BlueprintStore store;

    public BlueprintListPanel(String id) {
        super(id);
        add(JavascriptPackageResource.getHeaderContribution(BlueprintListPanel.class,
                "Hippo.ChannelManager.BlueprintListPanel.js"));
        List<ExtField> fieldList = new ArrayList<ExtField>();
        fieldList.add(new ExtField("name"));
        this.store = new BlueprintStore(fieldList);
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

}
