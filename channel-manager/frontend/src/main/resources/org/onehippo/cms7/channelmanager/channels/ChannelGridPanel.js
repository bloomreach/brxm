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
     * @class Hippo.ChannelManager.ChannelGridPanel
     * @extends Ext.grid.GridPanel
     */
    Hippo.ChannelManager.ChannelGridPanel = Ext.extend(Ext.grid.GridPanel, {
        constructor: function(config) {
            var columns = [], i, column, columnModel;
            this.store = config.store;
            this.columns = config.columns;
            this.resources = config.resources;
            this.selectedChannelId = null;

            Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

            columns = [];
            for (i = 0; i < config.columns.length; i++) {
                column = config.columns[i];
                if (!column.internal) {
                    columns.push(column);
                }
            }
            columnModel = new Ext.grid.ColumnModel({
                columns: columns,
                defaults: {
                    sortable: true
                }
            });

            Ext.apply(config, {
                id: 'channel-grid-panel',
                stripeRows: true,
                autoHeight: true,
                viewConfig: {
                    forceFit: true
                },
                stateful: true,
                stateEvents: ['columnmove', 'columnresize', 'sortchange', 'groupchange'],
                colModel: columnModel,

                view: new Ext.grid.GroupingView({
                    forceFit: true,
                    columnsText: config.resources['menu.columns'],
                    emptyText: config.resources['zero.channels'],
                    groupByText: config.resources['menu.group.by'],
                    groupTextTpl: String.format('{text} ({[values.rs.length]} {[values.rs.length != 1 ? "{0}" : "{1}"]})',
                            config.resources['group.channels'], config.resources['group.channel']),
                    showGroupsText: config.resources['menu.show.groups'],
                    sortAscText: config.resources['menu.sort.ascending'],
                    sortDescText: config.resources['menu.sort.descending']
                }),

                disableSelection: true,
                trackMouseOver: false,

                listeners: {
                    cellclick: function(grid, rowIndex, columnIndex, e) {
                        var record = this.getStore().getAt(rowIndex);
                        switch (e.getTarget().name) {
                            case 'show-channel':
                                this.selectedChannelId = record.get('id');
                                e.stopEvent();
                                this.fireEvent('channel-selected', this.selectedChannelId, record);
                                break;
                            case 'show-preview':
                                this.synchronousAjaxRequest(
                                        record.get('contextPath') + this.composerRestMountPath + '/cafebabe-cafe-babe-cafe-babecafebabe./previewmode/' + record.get('hostname') + '?FORCE_CLIENT_HOST=true',
                                        {
                                            'CMS-User': this.cmsUser,
                                            'FORCE_CLIENT_HOST': 'true'
                                        }
                                );
                                break;
                            case 'show-live':
                                break;
                        }
                    },
                    scope: this
                }

            });

            Hippo.ChannelManager.ChannelGridPanel.superclass.constructor.call(this, config);
        },

        synchronousAjaxRequest: function(url, headers) {
            var AJAX, i;
            if (window.XMLHttpRequest) {
                AJAX = new XMLHttpRequest();
            } else {
                AJAX = new ActiveXObject("Microsoft.XMLHTTP");
            }
            if (AJAX) {
                AJAX.open('GET', url, false);
                for (i in headers) {
                    AJAX.setRequestHeader(i, headers[i]);
                }
                AJAX.send(null);
                return AJAX.responseText;
            } else {
                return false;
            }
        },

        initComponent: function() {
            Hippo.ChannelManager.ChannelGridPanel.superclass.initComponent.apply(this, arguments);
            this.addEvents('add-channel', 'channel-selected');
            var self = this;
            this.on('afterlayout', function() {
                var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
                YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function() {
                    self.el.dom.style.height = (arguments[0].body.h - 55) + 'px';
                }, true);
            }, this, {single: true});
        },

        // Selects the row of the channel with this channel id. If no such channel exists, the selection will be cleared.
        // This method call selectRow(index);
        // Returns true if the channel was selected, false if not (e.g. because the channel does not exist)
        selectChannel: function(channelId) {
            var index = this.store.findExact('id', channelId);
            this.selectRow(index);
            return index >= 0;
        },

        getChannelByMountId: function(mountId) {
            var collection = this.store.query('mountId', mountId);
            return collection.first();
        },

        // Selects the row with this index (0-based). A negative index clears the selection.
        // Fires a channel-selected event when a channel is selected, otherwise fires a channel-deselected event.
        selectRow: function(index) {
            var model = this.getSelectionModel();
            if (index >= 0) {
                model.selectRow(index);
                this.getView().focusRow(index);
            } else {
                model.clearSelections();
            }
        },

        isChannelSelected: function() {
            return this.getSelectionModel().hasSelection();
        },

        // Returns the title of the currently selected channel
        getState: function() {
            var state, selectedRecord;
            state = Hippo.ChannelManager.ChannelGridPanel.superclass.getState.call(this);
            selectedRecord = this.getSelectionModel().getSelected();
            if (selectedRecord) {
                state.selected = selectedRecord.get('id');
            } else {
                // remove any old value
                delete state.selected;
            }
            return state;
        },

        // Restores the title of the currently selected channel. The corresponding row is selected after the store has
        // been loaded.
        applyState: function(state) {
            Hippo.ChannelManager.ChannelGridPanel.superclass.applyState.call(this, state);
            if (state.selected) {
                this.selectedChannelId = state.selected;
            }
        }

    });

    Ext.reg('Hippo.ChannelManager.ChannelGridPanel', Hippo.ChannelManager.ChannelGridPanel);

    /**
     * @class Hippo.ChannelManager.ChannelStore
     * @extends Ext.data.GroupingStore
     *
     * A simple extension of the grouping store that is configured with a reader automatically, similar to the
     * Ext.data.JsonStore
     *
     */
    Hippo.ChannelManager.ChannelStore = Ext.extend(Ext.data.GroupingStore, {
        constructor: function(config) {
            Hippo.ChannelManager.ChannelStore.superclass.constructor.call(this, Ext.apply(config, {
                reader: new Ext.data.JsonReader(config)
            }));
        }
    });

    Ext.reg('Hippo.ChannelManager.ChannelStore', Hippo.ChannelManager.ChannelStore);

}());