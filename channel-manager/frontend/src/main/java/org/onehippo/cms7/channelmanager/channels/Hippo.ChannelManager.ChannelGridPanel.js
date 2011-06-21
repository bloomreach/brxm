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
                this.store = config.store;
                this.columns = config.columns;
                Hippo.ChannelManager.ChannelGridPanel.superclass.constructor.call(this, config);
            },

            initComponent: function() {
                var me = this;
                var config = {
                    store: me.store,
                    stripeRows: true,
                    autoHeight: true,
                    viewConfig: {
                        forceFit: true
                    },

                    colModel: new Ext.grid.ColumnModel({
                                columns: [
                                    {
                                        header: 'Channel Name',
                                        width: 20,
                                        dataIndex: 'title',
                                        scope: me
                                    }
                                ]
                            })
                };

                Ext.apply(this, Ext.apply(this.initialConfig, config));

                Hippo.ChannelManager.ChannelGridPanel.superclass.initComponent.apply(this, arguments);
                this.on('reloadGrid', function() {
                    this.store.reload();
                }, this);
                this.store.load();

            },

            showChannelWindow: function() {
                console.log("TODO: Show channel window with blueprints ");

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
