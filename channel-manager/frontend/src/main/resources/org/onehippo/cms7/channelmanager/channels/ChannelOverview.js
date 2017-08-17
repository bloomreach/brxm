/**
 * Copyright 2011-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {
    "use strict";

    Ext.namespace('Hippo.ChannelManager');

    /**
     * @class Hippo.ChannelManager.ChannelOverview
     * @extends Ext.Panel
     */
    Hippo.ChannelManager.ChannelOverview = Ext.extend(Ext.Panel, {

        constructor: function (config) {
            var iconPanel, toolbar;

            iconPanel = Ext.getCmp('channelIconPanel');
            this.resources = config.resources;
            this.canModifyChannels = config.canModifyChannels;
            this.blueprintsAvailable = config.blueprintsAvailable;
            this.userPreferences = JSON.parse(localStorage.getItem('channelMgrConf'));

            toolbar = new Ext.Toolbar({
                height: 40,
                cls: 'channel-manager-toolbar',
                items: [
                    '->',
                    {
                        text: config.resources.type,
                        id: 'type-button',
                        enableToggle: true,
                        pressed: this.userPreferences.sort === 0,
                        hidden: this.userPreferences.display === 1,
                        toggleGroup: 'channelPropertyGrouping',
                        scope: this,
                        handler: function () {
                            iconPanel.layout.setActiveItem(0);
                            this.setUserPreferences('sort', 0);
                        }
                    },
                    {
                        text: config.resources.region,
                        id: 'region-button',
                        enableToggle: true,
                        pressed: this.userPreferences.sort === 1,
                        hidden: this.userPreferences.display === 1,
                        toggleGroup: 'channelPropertyGrouping',
                        scope: this,
                        handler: function () {
                            iconPanel.layout.setActiveItem(1);
                            this.setUserPreferences('sort', 1);
                        }
                    },
                    ' ',
                    {
                        handler: function () {
                            var typeButton = Ext.getCmp('type-button'),
                                regionButton = Ext.getCmp('region-button');

                            typeButton.show();
                            regionButton.show();
                            this.selectCard(0);
                        },
                        toggleGroup: 'channelViewGrouping',
                        enableToggle: true,
                        pressed: this.userPreferences.display === 0,
                        scope: this,
                        iconCls: 'icon-view'
                    },
                    {
                        handler: function () {
                            var typeButton = Ext.getCmp('type-button'),
                                regionButton = Ext.getCmp('region-button');

                            typeButton.hide();
                            regionButton.hide();
                            this.selectCard(1);
                        },
                        toggleGroup: 'channelViewGrouping',
                        pressed: this.userPreferences.display === 1,
                        enableToggle: true,
                        scope: this,
                        iconCls: 'list-view'
                    }
                ],
            });

            if (this.canModifyChannels && this.blueprintsAvailable) {
                toolbar.insert(0, {
                    text: config.resources['action.add.channel'],
                    handler: function () {
                        this.fireEvent('add-channel');
                    },
                    allowDepress: false,
                    scope: this,
                    iconCls: 'add-channel'
                });
            }

            Ext.apply(config, {
                id: 'channelOverview',
                layout: 'card',
                activeItem: this.userPreferences.display,
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

        selectCard: function (itemId) {
            if (itemId) {
                this.layout.setActiveItem(itemId);
                this.setUserPreferences('display', itemId);
            } else {
                this.layout.setActiveItem(0);
                this.setUserPreferences('display', 0);
            }
        },

        update: function (config) {
            this.selectCard(config.activeItem);
        },

        setUserPreferences: function (type, id) {
          this.userPreferences = JSON.parse(localStorage.getItem('channelMgrConf'));
          this.userPreferences[type] = id;
          localStorage.setItem('channelMgrConf', JSON.stringify(this.userPreferences));
        },

    });

    Ext.reg('Hippo.ChannelManager.ChannelOverview', Hippo.ChannelManager.ChannelOverview);
}());
