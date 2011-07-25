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

Ext.namespace('Hippo.ChannelManager');

/**
 * @class Hippo.ChannelManager.PropertiesPanel
 * @extends Ext.Panel
 */
Hippo.ChannelManager.ChannelPropertiesPanel = Ext.extend(Ext.Panel, {
    constructor: function(config) {
        var me = this;

        Ext.apply(config, {
            id: 'channel-properties-panel',
            width: 600,
            height: 400,
            collapsed: true,
            animCollapse: true,
            collapseMode: 'mini',
            collapsible: false,
            hideCollapseTool: true,
            disabled: true,
            title: ' ',
            split: true,
            // do not save the 'collapsed' state of this panel; that is managed by the Root panel depending on whether
            // a channel is selected or not
            stateful: false
        });

        Hippo.ChannelManager.ChannelPropertiesPanel.superclass.constructor.call(this, config);
    },

    showPanel: function(channelId, channelName) {
        this.expand();
        this.enable();
        if (channelName) {
            this.setTitle(channelName);
        }
        this.fireEvent('selectchannel', channelId);
    },

    hidePanel: function() {
        this.disable();
        this.collapse();
    },

    isShown: function() {
        return !this.collapsed;
    }

});

Ext.reg('Hippo.ChannelManager.ChannelPropertiesPanel', Hippo.ChannelManager.ChannelPropertiesPanel);
