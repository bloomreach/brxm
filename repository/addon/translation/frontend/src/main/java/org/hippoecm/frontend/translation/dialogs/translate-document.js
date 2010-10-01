HippoTranslator = Ext.extend(Ext.FormPanel, {

    labelWidth: 75, // label settings here cascade unless overridden
    frame:false,
    bodyStyle:'padding:5px 5px 0',
    width: 630,
    defaultType: 'textfield',

    constructor: function(config) {
        var self = this;

        this.store = config.store;
        this.codecUrl = config.codecUrl;

        this.imgLeft = config.imgLeft;
        this.imgRight = config.imgRight;
        this.folderName = config.folderName;
        this.urlName = config.urlName;
        this.addTranslation = config.addTranslation;

        this.emptyImg = config.emptyImg;
        this.folderImg = config.folderImg;
        this.documentImg = config.documentImg;

        this.record = null;
        this.task = null;

        this.updateUrl = function(record, value) {
            Ext.Ajax.request({
                url: self.codecUrl,
                params: { name: value },
                success: function(response, opts) {
                    var obj = Ext.decode(response.responseText);
                    record.data['urlfr'] = obj.data;
                    record.commit();
                }
            });
        },

        this.onKeyUp = function (field, event) {
            var rec = self.record;
            self.task.delay(500, function() {
            	self.updateUrl(rec, field.getRawValue());
            });
        };

        // the column model has information about grid columns
        // dataIndex maps the column to the specific data field in
        // the data store (created below)
        this.cm = new Ext.grid.ColumnModel({
            // specify any defaults for each column
            defaults: {
                sortable: false, // columns are not sortable by default
                orderable: false,
                menuDisabled: true,
            },
            columns: [{
                id: 'name',
                header: "<img src='" + self.imgLeft + "' style='vertical-align: top;' /> " + self.folderName,
                dataIndex: 'name',
                width: 300,
                renderer: self.renderFolder.createDelegate(self, [], 3),
                // use shorthand alias defined above
            }, {
                id: 'namefr',
                header: "<img src='" + self.imgRight + "' style='vertical-align: top;' /> " + self.folderName,
                dataIndex: 'namefr',
                width: 310,
                renderer: self.renderFolder.createDelegate(self, [], 3),
                editor: new Ext.form.TextField({
                    allowBlank: false,
                    enableKeyEvents: true,
                    listeners: {
                        keyup: function(field, event) {
                            self.onKeyUp(field, event);
                        }
                    }
                })
            }],
            isCellEditable: function(col, row) {
                var record = self.store.getAt(row);
                return record.get('editable');
            }
        });

        HippoTranslator.superclass.constructor.call(this, {

            renderTo: config.applyTo,
            
            items: [{
                xtype: 'editorgrid',
                name: 'grid',
                store: config.store,
                width: 630,
                height: 150,
                frame: false,
                clicksToEdit: 1,
                enableColumnMove: false,
                cm: self.cm,
                sm: new Ext.grid.RowSelectionModel({
                    singleSelect: true,
                    listeners: {
                        rowselect: function(sm, row, rec) {
                            if (rec != self.record) {
                                self.task = new Ext.util.DelayedTask(null, self);
                                self.record = rec;
                                self.form.loadRecord(rec);
                            }
                        },
                    },
                }),
                listeners: {
            		afteredit: function(e) {
            			if (e.field == "namefr") {
            				self.updateUrl(e.record, e.value);
            			}
            		}
            	}
            }, {
                xtype: 'compositefield',
                width: 630,
                hideLabel: true,
                frame: true,
                items: [{
                    xtype: 'fieldset',
                    title: this.urlName,
                    height: 60,
                    items: [{
                        xtype: 'compositefield',
                        width: 282,
                        hideLabel: true,
                        frame: true,
                        items: [{
                            xtype: 'displayfield',
                            value: '<img src="' + this.imgLeft + '" style="vertical-align: top;" />',
                            width: 25,
                        }, {
                            xtype: 'displayfield',
                            name: 'url',
                            height: 60,
                            width: 225,
                        }]
                    }]
                }, {
                    xtype: 'fieldset',
                    title: this.urlName,
                    height: 60,
                    items: [{
                        xtype: 'compositefield',
                        width: 282,
                        hideLabel: true,
                        frame: true,
                        items: [{
                            xtype: 'displayfield',
                            value: '<img src="' + this.imgRight + '" style="vertical-align: top;" />',
                            width: 25,
                        }, {
                            xtype: 'textfield',
                            disabled: true,
                            name: 'urlfr',
                            id: 'urlfr',
                            width: 210,
                        }, {
                            xtype: 'checkbox',
                            name: 'edit',
                            value: false,
                            hideLabel: false,
                            boxLabel: 'edit',
                            fieldLabel: 'edit',
                            listeners: {
                                check: function(chkbox, checked) {
                                    Ext.getCmp('urlfr').setDisabled(!checked);
                                }
                            }
                        }]
                    }]
                }]
            }]
        });

        Ext.apply(this, config);
    },

    initComponent : function() {
        HippoTranslator.superclass.initComponent.call(this);

        this.bodyCfg = {
    		tag: "div",
    		cls: "x-panel-body"
    	};

        this.store.on('update', function(store, record, operation) {
            if (record == this.record) {
                Ext.getCmp('urlfr').setRawValue(record.get('urlfr'));
            }
        }.createDelegate(this));
        
        this.store.load();
    },

    renderFolder: function(value, p, record){
        var txt = "<img src='" + this.emptyImg + "' width='" + 15 * record.data.id + "' height='1'/><img src='";
        if (record.data.type == "folder") {
            txt += this.folderImg;
        } else {
            txt += this.documentImg;
        }
        txt += "' heigth='10'/> ";

        if (value == "") {
            txt += "<font color='#ff0000'><i>" + this.addTranslation + "</i></font>";
        } else {
            txt += String.format("{0}", value);
        }
        return txt;
    },

});
