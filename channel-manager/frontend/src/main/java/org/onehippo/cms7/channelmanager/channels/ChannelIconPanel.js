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
"use strict";

Ext.namespace('Hippo.ChannelManager');

Hippo.ChannelManager.ChannelIconDataView = Ext.extend(Ext.DataView, {

    tpl : new Ext.XTemplate(
        '<tpl for=".">',
            '<span class="channel-group-handle expanded {[xindex % 2 === 0 ? "even" : "odd"]}">{name}</span>',
            '<ul class="channel-group {[xindex % 2 === 0 ? "even" : "odd"]}">',
                '<tpl for="channels">',
                    '<li class="channel" channelId="{id}">',
                        '<img src="{channelTypeImg}" />',
                        '<br /><img src="{channelRegionImg}" class="regionIcon" /><span class="channel-name">{name}</span>',
                        '<tpl if="lockedBy.length &gt; 0"><br /><span class="lockedBy">{lockedLabel}</span></tpl>',
                    '</li>',
                '</tpl>',
            '</ul>',
        '</tpl>'
    ),

    constructor : function(config) {
        this.resources = config.resources;
        this.groupByProperty = config.groupByProperty;
        Hippo.ChannelManager.ChannelIconDataView.superclass.constructor.apply(this, arguments);
    },

    initComponent : function() {

        this.on('refreshDataView', function() {
            var channelGroupHandles, channelGroups, i, len, channelGroupHandle, channelGroup;

            channelGroupHandles = Ext.query('.channel-group-handle');
            channelGroups = Ext.query('.channel-group');

                for (i=0, len=channelGroupHandles.length; i < len; i++) {
                    channelGroupHandle = Ext.get(channelGroupHandles[i]);
                    channelGroupHandle.removeAllListeners();

                    channelGroup = Ext.get(channelGroups[i]);
                    channelGroup.setVisibilityMode(Ext.Element.DISPLAY);

                    (function(channelGroupHandle, channelGroup) {
                        channelGroupHandle.on('click', function() {
                            if (channelGroup.isVisible()) {
                                channelGroup.hide();
                                channelGroupHandle.replaceClass('expanded', 'collapsed');
                            } else {
                                channelGroup.show();
                                channelGroupHandle.replaceClass('collapsed', 'expanded');
                            }
                        });
                    })(channelGroupHandle, channelGroup);
                }
            }, this);

        Hippo.ChannelManager.ChannelIconDataView.superclass.initComponent.apply(this, arguments);
    },

    collectData : function(records, startIndex) {
        var groups = {}, i, len, data, lockedDate, groupId, dataObject;

        for (i= 0, len=records.length; i < len; i++) {
            data = this.prepareData(records[i].json, startIndex + i, records[i]);

            if (data.lockedBy.length > 0) {
                lockedDate = new Date(parseInt(data.lockedOn)).format(this.resources['locked-date-format']);
                data.lockedLabel = this.resources['locked'].format(data.lockedBy, lockedDate);
            }

            groupId = records[i].json[this.groupByProperty];
            if (!groupId) {
                groupId = 'Unknown';
            }

            if (!groups[groupId]) {
                groups[groupId] = {
                    id: groupId,
                    name: (this.resources[groupId]) ? this.resources[groupId] : groupId,
                    channels : []
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

    refresh : function() {
        Hippo.ChannelManager.ChannelIconDataView.superclass.refresh.apply(this, arguments);
        this.fireEvent('refreshDataView');
    }

});


Hippo.ChannelManager.ChannelIconPanel = Ext.extend(Ext.Panel, {

    constructor: function(config) {
        var self, toolbar, channelTypeDataView, channelRegionDataView;
        this.resources = config.resources;
        this.store = config.store;

        self = this;
        toolbar = new Ext.Toolbar({
            items : [
                {
                    text: config.resources['type'],
                    enableToggle: true,
                    pressed: true,
                    toggleGroup: 'channelPropertyGrouping',
                    handler: function() {
                        self.layout.setActiveItem(0);
                    }
                },
                {
                    text: config.resources['region'],
                    enableToggle: true,
                    toggleGroup: 'channelPropertyGrouping',
                    handler: function() {
                        self.layout.setActiveItem(1);
                    }
                }
            ]
        });
        channelTypeDataView = this.createDataView('channelType');
        channelRegionDataView = this.createDataView('channelRegion');

        Ext.apply(config, {
            id: 'channelIconPanel',
            border: false,
            tbar: toolbar,
            layout: 'card',
            activeItem: 0,
            layoutOnCardChange: true,
            items : [{
                id: 'typeOverviewPanel',
                border: false,
                xtype: 'panel',
                autoScroll: true,
                items : [
                    channelTypeDataView
                ]
            }, {
                id: 'regionOverviewPanel',
                border: false,
                xtype: 'panel',
                autoScroll: true,
                items: [
                    channelRegionDataView
                ]
            }]
        });

        Hippo.ChannelManager.ChannelIconPanel.superclass.constructor.call(this, config);
    },

    createDataView : function(groupByProperty) {
        var dataView = new Hippo.ChannelManager.ChannelIconDataView({
            groupByProperty : groupByProperty,
            store: this.store,
            itemSelector: 'li.channel',
            overClass   : 'channel-hover',
            autoScroll  : true,
            resources : this.resources
        });
        dataView.on('click', function(dataView, index, element, eventObject) {
            this.selectedChannelId = element.getAttribute('channelId');
            var record = this.store.getById(this.selectedChannelId);
            this.fireEvent('channel-selected', this.selectedChannelId, record);
        }, this);
        return dataView;
    }

});

Ext.reg('Hippo.ChannelManager.ChannelIconPanel', Hippo.ChannelManager.ChannelIconPanel);
