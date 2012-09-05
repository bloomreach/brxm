Ext.ns('Hippo.Reports');

Hippo.Reports.BrokenLinksListPanel = Ext.extend(Hippo.Reports.Portlet, {

    constructor: function(config) {
        var self = this;

        this.store = config.store;
        this.pageSize = config.pageSize;
        this.paging = config.paging;
        this.pagingText = config.resources['documents-paging'];
        this.noDataText = config.noDataText;
        this.updateText = config.updateText;
        this.autoExpandColumn = config.autoExpandColumn;

        this.columns = config.columns;

        var columnCount = this.columns.length;


        var grid = new Ext.grid.GridPanel({
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
                text: self.updateText
            }],
            listeners: {
                cellclick: function(grid, rowIndex, columnIndex, event) {
                    var record = grid.getStore().getAt(rowIndex);
                    var columnId = grid.getColumnModel().getColumnId(columnIndex);

                    if (columnId != 'brokenlinksLinks') {
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
        if (this.store.getTotalCount() == 0) {
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
