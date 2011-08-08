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

        this.channelId = null;
        this.resources = config.resources;

        Ext.apply(config, {
            animCollapse: true,
            collapsed: true,
            collapsible: true,
            collapsibleSplitTip: config.resources['split.tip'],
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

        config.items.push({
            xtype: 'button',
            text: 'Open Channel',
            listeners: {
                click : {
                    fn: function() {
                        Hippo.App.Main.initComposer(this.subMountPath, this.hostname);
                        Ext.getCmp('rootPanel').layout.setActiveItem(1);
                        document.getElementById('Hippo.App.Main').className = 'x-panel';
                    },
                    scope : this
                }
            }
        });

        Hippo.ChannelManager.ChannelPropertiesPanel.superclass.constructor.call(this, config);
    },

    initComponent: function() {
        Hippo.ChannelManager.ChannelPropertiesPanel.superclass.initComponent.apply(this, arguments);

        this.on('beforeexpand', function(self, animate) {
            return this.channelId != null;
        }, this);
    },

    showPanel: function(channelId, channelName, record) {
        this.channelId = channelId;

        if (channelName) {
            this.setTitle(channelName);
        }
        this.hostname = record.get('hostname');
        this.subMountPath = record.get('subMountPath');
        this.expand();
        this.fireEvent('selectchannel', channelId);
    },

    hidePanel: function() {
        this.collapse();
    },

    closePanel: function() {
        this.channelId = null;
        this.hidePanel();
    },

    isShown: function() {
        return !this.collapsed;
    }

});

Ext.reg('Hippo.ChannelManager.ChannelPropertiesPanel', Hippo.ChannelManager.ChannelPropertiesPanel);
