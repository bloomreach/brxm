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
 * @class Hippo.ChannelManager.ChannelGridPanel
 * @extends Ext.grid.GridPanel
 */
Hippo.ChannelManager.ChannelGridPanel = Ext.extend(Ext.grid.GridPanel, {
    constructor: function(config) {
        var self = this;

        this.store = config.store;
        this.columns = config.columns;
        this.selectedChannelId = null;

        Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

        Ext.apply(config, {
            id: 'channel-grid-panel',
            store: self.store,
            stripeRows: true,
            autoHeight: true,
            viewConfig: {
                forceFit: true
            },
            stateful: true,
            stateEvents: ['columnmove', 'columnresize', 'sortchange', 'groupchange', 'selectionchange' ],
            title: 'Channel Manager',

            colModel: new Ext.grid.ColumnModel({
                columns: [
                    {
                        header: 'Channel Name',
                        width: 20,
                        dataIndex: 'name',
                        sortable: true,
                        scope: self
                    }
                ]
            }),

            tbar: new Ext.Toolbar({
                layout: 'hbox',
                layoutConfig: {
                    pack: 'center'
                },
                items: [
                    {
                        text: "Add Channel",
                        handler: function() {
                            this.fireEvent('add-channel');
                        },
                        scope: this
                    }
                ]
            }),

            sm: new Ext.grid.RowSelectionModel({
                singleSelect: true
            })
        });

        Hippo.ChannelManager.ChannelGridPanel.superclass.constructor.call(this, config);
    },

    initComponent: function() {
        Hippo.ChannelManager.ChannelGridPanel.superclass.initComponent.apply(this, arguments);
        this.store.load({
            callback: function() {
                this.selectChannel(this.selectedChannelId);
                this.getView().focusEl.focus();
            },
            scope: this
        });
        this.addEvents('add-channel');
    },

    // Selects the row of the channel with this channel id. If no such channel exists, the selection will be cleared.
    selectChannel: function(channelId) {
        console.log("SELECT CHANNEL ID " + channelId);
        var index = this.store.find('id', channelId)
        this.selectRow(index);
    },

    // Selects the row with this index (0-based). A negative index clears the selection.
    selectRow: function(index) {
        if (index >= 0) {
            this.getSelectionModel().selectRow(index);
            this.getView().focusRow(index);
            return;
        } else {
            // clear existing selection and fire the selectionchange event to force a state update
            this.getSelectionModel().clearSelections();
            this.fireEvent('selectionchange', this);
        }
    },

    // Returns the title of the currently selected channel
    getState: function() {
        var state = Hippo.ChannelManager.ChannelGridPanel.superclass.getState.call(this);
        var selectedRecord = this.getSelectionModel().getSelected();
        if (selectedRecord) {
            state.selected = selectedRecord.get('id');
        }
        return state;
    },

    // Restores the title of the currently selected channel. The corresponding row is selected after the store has
    // been loaded.
    applyState: function(state) {
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
