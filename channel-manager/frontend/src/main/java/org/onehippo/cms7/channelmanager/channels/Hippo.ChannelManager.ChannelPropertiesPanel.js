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
        var self = this;

        this.resources = config.resources;

        Ext.apply(config, {
            animCollapse: true,
            collapsed: true,
            collapsible: true,
            collapsibleSplitTip: self.resources['split.tip'],
            cmargins: '0',
            enabled: false,
            floatable: false,
            id: 'channel-properties-panel',
            title: ' ',
            split: true,
            stateful: true,
            useSplitTips: true,
            width: 600
        });

        Hippo.ChannelManager.ChannelPropertiesPanel.superclass.constructor.call(this, config);
    },

    showPanel: function(channelId, channelName) {
        this.expand();

        if (channelName) {
            this.setTitle(channelName);
        }

        this.fireEvent('selectchannel', channelId);
    },

    hidePanel: function() {
        this.collapse();
    },

    isShown: function() {
        return !this.collapsed;
    }

});

Ext.reg('Hippo.ChannelManager.ChannelPropertiesPanel', Hippo.ChannelManager.ChannelPropertiesPanel);
