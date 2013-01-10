/*
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
Ext.ns('Hippo.Reports');

Hippo.Reports.BrokenLinksListPanel = Ext.extend(Hippo.Reports.Portlet, {

    constructor: function(config) {
        var self, grid;

        self = this;

        this.store = config.store;
        this.pageSize = config.pageSize;
        this.paging = config.paging;
        this.pagingText = config.resources['documents-paging'];
        this.noDataText = config.noDataText;
        this.updateText = config.updateText;
        this.autoExpandColumn = config.autoExpandColumn;

        this.columns = config.columns;

        grid = new Ext.grid.GridPanel({
            store: self.store,
            colModel: new Ext.grid.ColumnModel({
                defaults: {
                    menuDisabled: true,
                    sortable: false
                },
                columns: self.columns
            }),
            loadMask: false,
            autoExpandColumn: self.autoExpandColumn,
            border: false,
            disableSelection: true,
            viewConfig: {
                scrollOffset: 2
            },
            bbar: self.paging ? new Ext.PagingToolbar({
                pageSize: self.pageSize,
                store: self.store,
                displayInfo: true,
                displayMsg: self.pagingText,
                emptyMsg: '',
                afterPageText: '',
                listeners: {
                    afterrender: function(bbar) {
                        bbar.last.hideParent = true;
                        bbar.last.hide();
                        bbar.refresh.hideParent = true;
                        bbar.refresh.hide();
                    }
                }
            }) : null,
            tbar:[{
                xtype: 'tbtext',
                text: self.updateText
            }],
            listeners: {
                cellclick: function(grid, rowIndex, columnIndex, event) {
                    var record = grid.getStore().getAt(rowIndex),
                        columnId = grid.getColumnModel().getColumnId(columnIndex);

                    if (columnId !== 'brokenlinksLinks') {
                        self.fireEvent('documentSelected', {path: record.data.path});
                    }
                }
            }
        });

        config = Ext.apply(config, {
            bodyCssClass: 'hippo-reports-document-list',
            items:[ grid ]
        });

        Hippo.Reports.BrokenLinksListPanel.superclass.constructor.call(this, config);

    },

    loadStore: function() {
        this.store.load({
            params: {
                start: 0,
                limit: this.pageSize
            }
        });
    },

    checkNoData: function(component) {
        if (this.store.getTotalCount() === 0) {
            this.showMessage(this.noDataText+"\n"+this.updateText);
        }
    },

    initComponent: function() {
        Hippo.Reports.BrokenLinksListPanel.superclass.initComponent.call(this);
        this.store.on('load', this.checkNoData, this);
        Hippo.Reports.RefreshObservableInstance.addListener("refresh", this.loadStore, this);
        this.loadStore();
    },

    destroy: function() {
        Hippo.Reports.BrokenLinksListPanel.superclass.destroy.call(this);
        Hippo.Reports.RefreshObservableInstance.removeListener("refresh", this.loadStore, this);
    }

});

Ext.reg('Hippo.Reports.BrokenLinksListPanel', Hippo.Reports.BrokenLinksListPanel);
