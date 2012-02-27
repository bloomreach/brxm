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

Hippo.ChannelManager.ChannelIconPanel = Ext.extend(Ext.Panel, {

    constructor: function(config) {
        this.resources = config.resources;
        this.store = config.store;

        this.currentComponents = [];
        this.types = [];
        this.regions = [];

        this.store.on('load', function() {
            this.store.each(function(record) {
                var type = record.get('type');
                if (type) {
                    if (this.resources[type]) {
                        this.types[this.resources[type]] = type;
                    } else {
                        this.types[type] = type;
                    }
                }
                var region = record.get('region');
                if (region) {
                    if (this.resources[region]) {
                        this.regions[this.resources[region]] = region;
                    } else {
                        this.regions[region] = region;
                    }
                }
            }, this);

            // the associative arrays are storing 'label' => 'key'
            this.regions = this.sortObject(this.regions);
            this.types = this.sortObject(this.types);

            var typeOverviewPanel = Ext.getCmp('typeOverviewPanel');
            typeOverviewPanel.add(this.createDataViews('type', this.types));
            typeOverviewPanel.doLayout();

            var regionOverviewPanel = Ext.getCmp('regionOverviewPanel');
            regionOverviewPanel.add(this.createDataViews('region', this.regions));
            regionOverviewPanel.doLayout();
        }, this, { single: true } );

        var self = this;
        var toolbar = new Ext.Toolbar({
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
                autoScroll: true
            }, {
                id: 'regionOverviewPanel',
                border: false,
                xtype: 'panel',
                autoScroll: true
            }]
        });

        Hippo.ChannelManager.ChannelIconPanel.superclass.constructor.call(this, config);
    },

    sortObject : function (object){
        var sortedKeys = [];
        var sortedObj = {};

        for (var i in object) {
            sortedKeys.push(i);
        }
        sortedKeys.sort();

        for (var i=0; i<sortedKeys.length; i++) {
            sortedObj[sortedKeys[i]] = object[sortedKeys[i]];
        }
        return sortedObj;
    },


    filterDataByType : function(property, value) {
        return function(records, startIndex) {
            var r = [], i = 0, len = records.length;
            for(; i < len; i++){
                if (records[i].data[property] != value) {
                    continue;
                }
                r[r.length] = this.prepareData(records[i].json, startIndex + i, records[i]);
            }
            return r;
        };
    },

    createDataView : function(property, value) {
        var self = this;
        var dataView = new Ext.DataView({
            store: this.store,
            tpl : new Ext.XTemplate(
                    '<ul class="channel-group">',
                    '<tpl for=".">',
                    '<li class="channel" channelId="{id}">',
                    '<img width="64" height="64" src="{type_img}" />',
                    '<br /><img src="{region_img}" class="regionIcon" /><span class="channel-name">{name}</span>',
                    '</li>',
                    '</tpl>',
                    '</ul>'
            ),
            cls : 'channel-data-view',
            collectData : this.filterDataByType(property, value),
            itemSelector: 'li.channel',
            overClass   : 'channel-hover',
            autoScroll  : true
        });
        dataView.on('click', function(dataView, index, element, eventObject) {
            this.selectedChannelId = element.getAttribute('channelId');
            var record = this.store.getById(this.selectedChannelId);
            this.fireEvent('channel-selected', this.selectedChannelId, record);
        }, this);
        return dataView;
    },

    createDataViews : function(property, values) {
        var views = [];
        var self = this;
        for (var value in values) {
            if (typeof values[value] == 'function') {
                continue;
            }
            var dataView = this.createDataView(property, values[value]);
            (function(views, dataView) {
                var panel = new Ext.Panel({
                    html: '<span class="collapse-group expanded">'+value+'</span>',
                    listeners: {
                        afterrender : function(panel) {
                            var spanElement = Ext.get(Ext.select('.collapse-group', true, panel.el.dom));
                            spanElement.on('click', function() {
                                if (!dataView.hidden) {
                                    dataView.hide();
                                    spanElement.replaceClass('expanded', 'collapsed');
                                    self.doLayout();
                                } else {
                                    dataView.show();
                                    spanElement.replaceClass('collapsed', 'expanded');
                                    self.doLayout();
                                }
                            });
                        }
                    },
                    border: false,
                    height: 18
                });
                views[views.length] = panel;
            })(views, dataView);
            views[views.length] = dataView;
        }
        return views;
    }

});

Ext.reg('Hippo.ChannelManager.ChannelIconPanel', Hippo.ChannelManager.ChannelIconPanel);
