/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager');

    /**
     * @class Hippo.ChannelManager.ChannelOverview
     * @extends Ext.Panel
     */
    Hippo.ChannelManager.ChannelOverview = Ext.extend(Ext.Panel, {

        constructor: function(config) {
            this.resources = config.resources;

            this.canModifyChannels = config.canModifyChannels;
            this.blueprintsAvailable = config.blueprintsAvailable;

            var toolbar = new Ext.Toolbar({
                height: 28,
                cls: 'channel-manager-toolbar',
                items: []
            });
            if (this.canModifyChannels && this.blueprintsAvailable) {
                toolbar.add({
                    text: config.resources['action.add.channel'],
                    handler: function() {
                        this.fireEvent('add-channel');
                    },
                    allowDepress: false,
                    scope: this,
                    iconCls: 'add-channel'
                });
            }

            toolbar.add('->');
            toolbar.add({
                handler: function() {
                    this.selectCard(0);
                },
                toggleGroup: 'channelViewGrouping',
                enableToggle: true,
                pressed: true,
                scope: this,
                iconCls: 'icon-view'
            });
            toolbar.add({
                handler: function() {
                    this.selectCard(1);
                },
                toggleGroup: 'channelViewGrouping',
                enableToggle: true,
                scope: this,
                iconCls: 'list-view'
            });

            Ext.apply(config, {
                id: 'channelOverview',
                layout: 'card',
                activeItem: 0,
                layoutOnCardChange: true,
                deferredRender: true,
                viewConfig: {
                    forceFit: true
                },
                border: false,
                tbar: toolbar
            });

            Hippo.ChannelManager.ChannelOverview.superclass.constructor.call(this, config);
        },

        selectCard: function(itemId) {
            if (itemId) {
                this.layout.setActiveItem(itemId);
            } else {
                this.layout.setActiveItem(0);
            }
        },

        update: function(config) {
            this.selectCard(config.activeItem);
        }

    });

    Ext.reg('Hippo.ChannelManager.ChannelOverview', Hippo.ChannelManager.ChannelOverview);

}());