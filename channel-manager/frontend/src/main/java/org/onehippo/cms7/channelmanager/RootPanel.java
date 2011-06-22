/*
 *  Copyright 2011 Hippo.
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

package org.onehippo.cms7.channelmanager;

import org.apache.wicket.markup.html.JavascriptPackageResource;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.channelmanager.channels.BlueprintStore;
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.ChannelStore;
import org.onehippo.cms7.channelmanager.channels.PropertiesPanel;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.data.ExtField;
import org.wicketstuff.js.ext.layout.BorderLayout;
import org.wicketstuff.js.ext.util.ExtClass;
import org.wicketstuff.js.ext.util.JSONIdentifier;

import java.util.ArrayList;
import java.util.List;

@ExtClass("Hippo.ChannelManager.RootPanel")
public class RootPanel extends ExtPanel {

    private BlueprintStore blueprintStore;
    private ChannelStore channelStore;


    public RootPanel(String id) {
        super(id);
        add(JavascriptPackageResource.getHeaderContribution(RootPanel.class, "Hippo.ChannelManager.RootPanel.js"));
        add(JavascriptPackageResource.getHeaderContribution(RootPanel.class, "Hippo.ChannelManager.BlueprintListPanel.js"));
        add(JavascriptPackageResource.getHeaderContribution(RootPanel.class, "Hippo.ChannelManager.ChannelFormPanel.js"));

        ChannelGridPanel gridPanel = new ChannelGridPanel();
        gridPanel.setRegion(BorderLayout.Region.CENTER);
        gridPanel.setSplit(true);
        add(gridPanel);
        //Use the same store variable as the grid panel, no need to create another store.
        this.channelStore = gridPanel.getStore();


        PropertiesPanel propertiesPanel = new PropertiesPanel();
        propertiesPanel.setRegion(BorderLayout.Region.EAST);
        propertiesPanel.setCollapsed(true);
        propertiesPanel.setCollapsible(true);
        propertiesPanel.setSplit(true);
        add(propertiesPanel);

        List<ExtField> blueprintFieldList = new ArrayList<ExtField>();
        blueprintFieldList.add(new ExtField("name"));
        this.blueprintStore = new BlueprintStore(blueprintFieldList);
        add(this.blueprintStore);

    }


    @Override
    protected void preRenderExtHead(StringBuilder js) {
        blueprintStore.onRenderExtHead(js);
        super.preRenderExtHead(js);
    }

    @Override
    protected void onRenderProperties(JSONObject properties) throws JSONException {
        super.onRenderProperties(properties);
        properties.put("blueprintStore", new JSONIdentifier(this.blueprintStore.getJsObjectId()));
        properties.put("channelStore", new JSONIdentifier(this.channelStore.getJsObjectId()));
    }

}
