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

/**
 * @class Hippo.ChannelManager.BlueprintListPanel
 * @extends Ext.grid.GridPanel
 */
Hippo.ChannelManager.BlueprintListPanel = Ext.extend(Ext.grid.GridPanel, {
    constructor: function(config) {
        this.store = config.store;
        this.columns = config.columns;
        Hippo.ChannelManager.BlueprintListPanel.superclass.constructor.call(this, config);
    },

    initComponent: function() {
        var me, config;
        me = this;
        config = {
            id: me.id,
            store: me.store,
            loadMask: true,
            stripeRows: true,
            height: 400,
            viewConfig: {
                forceFit: true
            },

            colModel: new Ext.grid.ColumnModel({
                columns: [
                    {
                        header: me.resources['new-channel-blueprint-name'],
                        align: 'left'
                    },
                    {
                        header: me.resources['new-channel-blueprint-description'],
                        align: 'left'
                    }
                ]
            }),
            sm: new Ext.grid.RowSelectionModel({
                singleSelect: true
            })
        };

        Ext.apply(this, Ext.apply(this.initialConfig, config));

        Hippo.ChannelManager.BlueprintListPanel.superclass.initComponent.apply(this, arguments);

        this.on('render', function() {
            this.store.load();
        }, this);

        this.store.on('load', function () {
            //TODO: select the first row only when there are rows.
            this.getSelectionModel().selectFirstRow();
        }, this)

    }
});

Ext.reg('Hippo.ChannelManager.BlueprintListPanel', Hippo.ChannelManager.BlueprintListPanel);
