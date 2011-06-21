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
import org.onehippo.cms7.channelmanager.channels.ChannelGridPanel;
import org.onehippo.cms7.channelmanager.channels.PropertiesPanel;
import org.wicketstuff.js.ext.ExtPanel;
import org.wicketstuff.js.ext.layout.BorderLayout;
import org.wicketstuff.js.ext.util.ExtClass;

@ExtClass("Hippo.ChannelManager.RootPanel")
public class RootPanel extends ExtPanel {

    public RootPanel(String id) {
        super(id);
        add(JavascriptPackageResource.getHeaderContribution(RootPanel.class, "Hippo.ChannelManager.RootPanel.js"));
        ChannelGridPanel gridPanel = new ChannelGridPanel();
        gridPanel.setRegion(BorderLayout.Region.CENTER);
        gridPanel.setSplit(true);
        add(gridPanel);

        PropertiesPanel propertiesPanel = new PropertiesPanel();
        propertiesPanel.setRegion(BorderLayout.Region.EAST);
        propertiesPanel.setCollapsed(true);
        propertiesPanel.setCollapsible(true);
        propertiesPanel.setSplit(true);
        add(propertiesPanel);

    }

}
