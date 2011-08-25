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

Ext.ToolTip.prototype.onTargetOver =
        Ext.ToolTip.prototype.onTargetOver.createInterceptor(function(e) {
            this.baseTarget = e.getTarget();
        });
Ext.ToolTip.prototype.onMouseMove =
        Ext.ToolTip.prototype.onMouseMove.createInterceptor(function(e) {
            if (!e.within(this.baseTarget)) {
                this.onTargetOver(e);
                return false;
            }
        });

/**
 * @class Hippo.ChannelManager.ChannelGridPanel
 * @extends Ext.grid.GridPanel
 */
Hippo.ChannelManager.ChannelGridPanel = Ext.extend(Ext.grid.GridPanel, {
    constructor: function(config) {
        this.store = config.store;
        this.columns = config.columns;
        this.resources = config.resources;
        this.selectedChannelId = null;

        Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

        Ext.apply(config, {
            id: 'channel-grid-panel',
            stripeRows: true,
            autoHeight: true,
            viewConfig: {
                forceFit: true
            },
            stateful: true,
            stateEvents: ['columnmove', 'columnresize', 'sortchange', 'groupchange', 'savestate' ],
            title: config.resources['title'],

            colModel: new Ext.grid.ColumnModel({
                columns: config.columns,
                defaults: {
                    sortable: true
                }
            }),

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

            tbar: new Ext.Toolbar({
                layout: 'hbox',
                layoutConfig: {
                    pack: 'center'
                },
                items: [
                    {
                        text: config.resources['action.add.channel'],
                        handler: function() {
                            this.fireEvent('add-channel');
                        },
                        scope: this
                    }
                ]
            }),

            sm: new Ext.grid.RowSelectionModel({
                singleSelect: true
            }),

            // enable per-cell tooltips
            onRender: function() {
                Ext.grid.GridPanel.prototype.onRender.apply(this, arguments);
                this.addEvents("beforetooltipshow");
                this.tooltip = new Ext.ToolTip({
                    renderTo: Ext.getBody(),
                    target: this.view.mainBody,
                    listeners: {
                        beforeshow: function(tooltip) {
                            var v = this.getView();
                            var row = v.findRowIndex(tooltip.baseTarget);
                            if (row) {
                                var cell = v.findCellIndex(tooltip.baseTarget);
                                if (cell) {
                                    this.fireEvent("beforetooltipshow", this, row, cell);
                                }
                            }
                        },
                        scope: this
                    }
                });
            },
            listeners: {
                render: function(g) {
                    g.on("beforetooltipshow", function(grid, row, col) {
                        var record = grid.getStore().getAt(row);
                        var colName = grid.getColumnModel().getDataIndex(col);
                        var value = record.get(colName);
                        grid.tooltip.body.update(value);
                    });
                }
            }

        });

        Hippo.ChannelManager.ChannelGridPanel.superclass.constructor.call(this, config);
    },

    initComponent: function() {
        Hippo.ChannelManager.ChannelGridPanel.superclass.initComponent.apply(this, arguments);
        this.store.on('load', function() {
            if (this.selectChannel(this.selectedChannelId)) {
                this.getView().focusEl.focus();
            } else {
                this.selectedChannelId = null;
            }
        }, this);
        this.on('afterrender', function() {
            this.store.load();
        }, this);

        this.addEvents('add-channel', 'channel-selected', 'channel-deselected', 'savestate');

        var sm = this.getSelectionModel();
        sm.on('rowselect', function() {
            this.fireEvent('savestate');
        }, this);
        sm.on('rowdeselect', function() {
            this.fireEvent('savestate');
        }, this);

        // ensure that we do not create a channel-deselected event quickly followed by a channel-selected event when
        // up or down is used to navigate to the next row, since this may ruin animations of components that listen
        // to these events
        this.isSelectingRow = true;
        sm.on('beforerowselect', function() {
            this.isSelectingRow = true;
            return true;
        }, this);
        sm.on('rowdeselect', function(sm) {
            if (!this.isSelectingRow) {
                this.selectedChannelId = null;
                this.fireEvent('channel-deselected');
            }
        }, this);
        sm.on('rowselect', function(sm, rowIndex, record) {
            this.isSelectingRow = false;
            this.selectedChannelId = record.get('id');
            this.fireEvent('channel-selected', this.selectedChannelId, record.get('name'), record);
        }, this);

        // register keyboard navigation
        this.on('keydown', function(event) {
            switch (event.keyCode) {
                case 13: // ENTER
                    var selectedRecord = this.getSelectionModel().getSelected();
                    if (selectedRecord) {
                        this.selectChannel(selectedRecord.get('id'));
                    }
                    break;
                case 27: // ESC
                    this.fireEvent('channel-escaped');
                    break;
            }
        }, this);
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
        var state = Hippo.ChannelManager.ChannelGridPanel.superclass.getState.call(this);
        var selectedRecord = this.getSelectionModel().getSelected();
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
