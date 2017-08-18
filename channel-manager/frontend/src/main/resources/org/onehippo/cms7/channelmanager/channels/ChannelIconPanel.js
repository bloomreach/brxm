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
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager');

    Hippo.ChannelManager.ChannelIconDataView = Ext.extend(Ext.DataView, {

        tpl: new Ext.XTemplate(
            '<tpl for=".">',
            '  <div class="channel-group-handle expanded {[xindex % 2 === 0 ? "even" : "odd"]}">{name}</div>',
            '  <ul class="channel-group {[xindex % 2 === 0 ? "even" : "odd"]}">',
            '    <tpl for="channels">',
            '      <li class="channel" channelId="{id}">',
            '        <div class="channel-icon-stack">',
            '          <img class="channel-type-icon" src="{channelTypeImg}" />',
            '          <tpl if="changedBySet.length &gt; 0">',
            '            <div class="channel-modified-icon"></div>',
            '          </tpl>',
            '        </div>',
            '        <br>',
            '        <img class="channel-region-icon" src="{channelRegionImg}" class="regionIcon" />',
            '        <span class="channel-name">{name}</span>',
            '        <tpl if="changedBySet.length &gt; 0">',
            '          <div class="lockedBy" title="{lockedDetail}">{lockedLabel}</div>',
            '        </tpl>',
            '      </li>',
            '    </tpl>',
            '  </ul>',
            '</tpl>'
        ),

        constructor: function(config) {
            this.resources = config.resources;
            this.groupByProperty = config.groupByProperty;
            Hippo.ChannelManager.ChannelIconDataView.superclass.constructor.apply(this, arguments);
        },

        initComponent: function() {

            this.on('refreshDataView', function() {
                var channelGroupHandles, channelGroups, i, len, channelGroupHandle, channelGroup;

                channelGroupHandles = Ext.query('.channel-group-handle');
                channelGroups = Ext.query('.channel-group');

                for (i = 0, len = channelGroupHandles.length; i < len; i++) {
                    channelGroupHandle = Ext.get(channelGroupHandles[i]);
                    channelGroupHandle.removeAllListeners();

                    channelGroup = Ext.get(channelGroups[i]);
                    channelGroup.setVisibilityMode(Ext.Element.DISPLAY);

                    this.registerOnClick(channelGroupHandle, channelGroup);
                }
            }, this);

            Hippo.ChannelManager.ChannelIconDataView.superclass.initComponent.apply(this, arguments);
        },

        registerOnClick: function(channelGroupHandle, channelGroup) {
            channelGroupHandle.on('click', function() {
                if (channelGroup.isVisible()) {
                    channelGroup.hide();
                    channelGroupHandle.replaceClass('expanded', 'collapsed');
                } else {
                    channelGroup.show();
                    channelGroupHandle.replaceClass('collapsed', 'expanded');
                }
            });
        },

        collectData: function(records, startIndex) {
            var groups = {}, i, len, data, groupId, dataObject, changedByCurrentUser, k, klen;

            for (i = 0, len = records.length; i < len; i++) {
                data = this.prepareData(records[i].json, startIndex + i, records[i]);
                if (data.changedBySet.length > 0) {
                    data.lockedDetail = data.changedBySet.join();
                    changedByCurrentUser = false;
                    for (k = 0, klen = data.changedBySet.length; k < klen; k++ ) {
                        if (data.changedBySet[k] === this.userId) {
                            changedByCurrentUser = true;
                        }
                    }
                    if (changedByCurrentUser && data.changedBySet.length === 1) {
                        data.lockedLabel = this.resources['you-have-unpublished-changes'];
                    } else if (changedByCurrentUser && data.changedBySet.length === 2){
                        data.lockedLabel = this.resources['you-and-one-user-have-unpublished-changes'];
                    } else if (changedByCurrentUser && data.changedBySet.length > 2){
                        data.lockedLabel = this.resources['you-and-x-users-have-unpublished-changes'].format(data.changedBySet.length -1);
                    } else if (data.changedBySet.length === 1) {
                        data.lockedLabel = this.resources['one-user-has-unpublished-changes'];
                    } else {
                        data.lockedLabel = this.resources['x-users-have-unpublished-changes'].format(data.changedBySet.length);
                    }

                }
                groupId = records[i].json[this.groupByProperty];
                if (!groupId) {
                    groupId = 'Unknown';
                }

                if (!groups[groupId]) {
                    groups[groupId] = {
                        id: groupId,
                        name: (this.resources[groupId]) ? this.resources[groupId] : groupId,
                        channels: []
                    };
                }
                groups[groupId].channels.push(data);
            }

            // create non associative array
            dataObject = [];

            Ext.iterate(groups, function(groupId, group) {
                group.channels.sort(function(channel1, channel2) {
                    return channel1.name > channel2.name;
                });
                dataObject.push(group);
            }, this);

            dataObject.sort(function(group1, group2) {
                return group1.name > group2.name;
            });

            return dataObject;
        },

        refresh: function() {
            Hippo.ChannelManager.ChannelIconDataView.superclass.refresh.apply(this, arguments);
            this.fireEvent('refreshDataView');
        },

    });


    Hippo.ChannelManager.ChannelIconPanel = Ext.extend(Ext.Panel, {

        constructor: function(config) {
            var channelTypeDataView,
                channelRegionDataView;

            this.resources = config.resources;
            this.store = config.store;
            this.userId = config.userId;
            this.initUserPreferences();
            channelTypeDataView = this.createDataView('channelType');
            channelRegionDataView = this.createDataView('channelRegion');

            Ext.apply(config, {
                id: 'channelIconPanel',
                border: false,
                layout: 'card',
                activeItem: this.userPreferences.sort,
                layoutOnCardChange: true,
                items: [
                    {
                        id: 'typeOverviewPanel',
                        border: false,
                        xtype: 'panel',
                        autoScroll: true,
                        items: [
                            channelTypeDataView
                        ]
                    },
                    {
                        id: 'regionOverviewPanel',
                        border: false,
                        xtype: 'panel',
                        autoScroll: true,
                        items: [
                            channelRegionDataView
                        ]
                    }
                ]
            });

            Hippo.ChannelManager.ChannelIconPanel.superclass.constructor.call(this, config);
        },

        createDataView: function(groupByProperty) {
            var dataView = new Hippo.ChannelManager.ChannelIconDataView({
                groupByProperty: groupByProperty,
                store: this.store,
                userId: this.userId,
                itemSelector: 'li.channel',
                overClass: 'channel-hover',
                autoScroll: true,
                resources: this.resources
            });
          dataView.on('click', function(dataView, index, element, eventObject) {
                this.selectedChannelId = element.getAttribute('channelId');
                var record = this.store.getById(this.selectedChannelId);
                this.fireEvent('channel-selected', this.selectedChannelId, record);
            }, this);
            return dataView;
        },

        initUserPreferences() {
            this.userPreferences = JSON.parse(localStorage.getItem('channelMgrConf'));
            if(!this.userPreferences) {
                this.userPreferences = {'sort': 0, 'display': 0, 'channelRegion_even': 1, 'channelRegion_odd': 1, 'channelType_even': 1, 'channelType_odd': 1};
                localStorage.setItem('channelMgrConf', JSON.stringify(this.userPreferences));
            }
        },

        setUserPreferences: function (type, id) {
            this.userPreferences[type] = id;
            localStorage.setItem('channelMgrConf', JSON.stringify(this.userPreferences));
        },

    });

    Ext.reg('Hippo.ChannelManager.ChannelIconPanel', Hippo.ChannelManager.ChannelIconPanel);

}());
