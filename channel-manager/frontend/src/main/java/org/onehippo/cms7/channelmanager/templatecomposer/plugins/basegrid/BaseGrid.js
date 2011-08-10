/*
 *  Copyright 2010 Hippo.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

Ext.namespace('Hippo.ChannelManager.TemplateComposer');

Hippo.ChannelManager.TemplateComposer.BaseGridPanel = Ext.extend(Ext.grid.GridPanel, {
    frame: false,
    loadMask: true,
    viewConfig: {
        forceFit: true,
        onLoad: Ext.emptyFn,
        listeners: {
            beforerefresh: function(v) {
                v.scrollTop = v.scroller.dom.scrollTop;
                v.scrollHeight = v.scroller.dom.scrollHeight;
            },
            refresh: function(v) {
                //v.scroller.dom.scrollTop = v.scrollTop;
                v.scroller.dom.scrollTop = v.scrollTop +
                        (v.scrollTop == 0 ? 0 : v.scroller.dom.scrollHeight - v.scrollHeight);
            }
        }
    },

    initComponent: function() {

        var store = this.initialConfig.store || new Ext.data.ArrayStore();
        var columnModel = this.initialConfig.colModel || new Ext.grid.ColumnModel({
            defaults: {
                sortable: false,
                menuDisabled: true,
                width: 100
            }
        });

        var listeners = {
            contextmenu:{
                scope:this,
                fn:Ext.emptyFn,
                stopEvent:true
            },

            rowcontextmenu: {
                scope:      this,
                stopEvent:  true,
                fn:         function(grid, rowIndex, e) {
                    var record = grid.getStore().getAt(rowIndex);
                    var selected = grid.getSelectionModel().hasSelection() &&
                            grid.getSelectionModel().getSelected().id == record.id;

                    var menu = new Ext.menu.Menu({
                        items: this.menuProvider ? this.menuProvider.getMenuActions(record, selected) : this.newEmptyContextMenu()
                    });
                    menu.showAt(e.getXY());
                    e.preventDefault();
                }
            }
        };
        Ext.apply(this.initialConfig.listeners, listeners);
        var config = {
            store: store,
            colModel :columnModel,
            listeners: listeners,

            stripeRows: true
        };

        Ext.apply(this, Ext.apply(this.initialConfig, config));

        Hippo.ChannelManager.TemplateComposer.BaseGridPanel.superclass.initComponent.apply(this, arguments);
    },

    newEmptyContextMenu: function() {
        return  [
            new Ext.Action({
                text: '-- Empty --',
                handler: function() {
                }
            })
        ];
    }
});
Ext.reg('h_base_grid', Hippo.ChannelManager.TemplateComposer.BaseGridPanel);