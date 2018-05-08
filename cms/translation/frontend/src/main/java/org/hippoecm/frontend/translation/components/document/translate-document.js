/*
 * Copyright 2010-2018 Hippo B.V. (http://www.onehippo.com)
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


if (Hippo.Translation.WicketHook === undefined) {
    (function() {

        Hippo.Translation.WicketHook = {
            listeners: [],

            cleanup: function() {
                var i, listener, listenerId;
                i = 0;
                while (i < this.listeners.length) {
                    listener = this.listeners[i];
                    listenerId = Ext.get(listener.id);
                    if (listenerId === null || listenerId === undefined) {
                        this.listeners.splice(i, 1);
                    } else {
                        i++;
                    }
                }
            },

            preAjaxCall: function() {
                var i, len, listener, el;
                for (i = 0, len = this.listeners.length; i < len; i++) {
                    listener = this.listeners[i];
                    el = Ext.get(listener.id);
                    if (el !== null && el !== undefined) {
                        listener.callback.call(window);
                    }
                }
            },

            addListener: function(id, callback) {
                this.cleanup();
                this.listeners.push({ id: id, callback: callback });
            }

        };
    }());

    Wicket.Event.subscribe('/ajax/call/before', function() {
        Hippo.Translation.WicketHook.preAjaxCall();
    });
}

Hippo.Translation.Document = Ext.extend(Ext.FormPanel, {

    labelWidth: 75, // label settings here cascade unless overridden
    frame: false,
    width: 661,
    defaultType: 'textfield',

    constructor: function(config) {
        var self, imageService;

        self = this;

        this.dirty = [];
        this.store = config.store;
        this.codecUrl = config.codecUrl;

        this.resources = config.resources;

        this.emptyImg = config.emptyImg;
        this.folderImg = config.folderImg;
        this.documentImg = config.documentImg;

        imageService = new Hippo.Translation.ImageService(config.imageService);
        this.imgLeft = imageService.getImage(config.sourceLanguage);
        this.imgRight = imageService.getImage(config.targetLanguage);

        this.record = null;
        this.updateUrlTask = null;

        this.updateUrl = function(record, value, async) {
            var xhr, handleResponse;
            if (window.XMLHttpRequest) {    // Mozilla/Safari
                xhr = new XMLHttpRequest();
            } else if (window.ActiveXObject) {     // IE
                xhr = new window.ActiveXObject("Microsoft.XMLHTTP");
            }
            handleResponse = function() {
                var obj = Ext.decode(xhr.responseText);
                if (!record.checked) {
                    if (async) {
                        Ext.getCmp('urlfr').setRawValue(obj.data);
                    } else {
                        record.set('urlfr', obj.data); // triggers store update event
                    }
                }
            };
            xhr.open('POST', self.codecUrl + "&" + Ext.urlEncode({name: value}), async);
            xhr.setRequestHeader('Content-Type', 'application/json');
            xhr.setRequestHeader('Wicket-Ajax', "true");
            xhr.setRequestHeader('Wicket-Ajax-BaseURL', Wicket.Ajax.baseUrl);
            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    handleResponse();
                }
            };
            xhr.send(null);
            if (!async) {
                if (xhr.status === 200) {
                    handleResponse();
                    self.store.save();
                }
            }
        };

        Hippo.Translation.WicketHook.addListener(config.id, function() {
            var record = self.record;
            if (record !== null && self.dirty.indexOf(record) !== -1 && !record.checked) {
                self.updateUrl(record, record.get('namefr'), false);
            }
        });

        this.onKeyUp = function(field, event) {
            var rec = self.record;
            if (self.dirty.indexOf(rec) === -1) {
                self.dirty.push(rec);
            }
            if (self.updateUrlTask) {
                self.updateUrlTask.cancel();
            }
            self.updateUrlTask = new Ext.util.DelayedTask(function() {
                if (!rec.checked) {
                    self.updateUrl(rec, field.getRawValue(), true);
                }
            });
            self.updateUrlTask.delay(500);
        };

        this.isRecordEmpty = function() {
            var empty = false;
            config.store.each(function(record) {
                if (record.get('namefr') && record.get('urlfr')) {
                    empty = false;
                } else {
                    empty = true;
                    return false;
                }
            });
            return empty;
        };

        // the column model has information about grid columns
        // dataIndex maps the column to the specific data field in
        // the data store (created below)
        this.cm = new Ext.grid.ColumnModel({
            // specify any defaults for each column
            defaults: {
                sortable: false, // columns are not sortable by default
                orderable: false,
                menuDisabled: true
            },
            columns: [
                {
                    id: 'name',
                    header: "<img src='" + self.imgLeft + "' style='vertical-align: top;' /> " + self.resources['folder-name'],
                    dataIndex: 'name',
                    width: 323,
                    renderer: self.renderFolder.createDelegate(self, ['url'], 0)
                    // use shorthand alias defined above
                },
                {
                    id: 'namefr',
                    header: "<img src='" + self.imgRight + "' style='vertical-align: top;' /> " + self.resources['folder-name'],
                    dataIndex: 'namefr',
                    width: 334,
                    renderer: self.renderFolder.createDelegate(self, ['urlfr'], 0),
                    editor: new Ext.form.TextField({
                        allowBlank: false,
                        enableKeyEvents: true,
                        listeners: {
                            keyup: function(field, event) {
                                self.onKeyUp(field, event);
                            },
                            focus: function() {
                                self.setOkButtonEnabled(false);
                            },
                            blur: function() {
                                self.setOkButtonEnabled(!self.isRecordEmpty());
                            }
                        }
                    })
                }
            ],
            isCellEditable: function(col, row) {
                var record = self.store.getAt(row);
                return record.get('editable');
            }
        });

        Hippo.Translation.Document.superclass.constructor.call(this, {

            renderTo: config.applyTo,

            items: [
                {
                    xtype: 'editorgrid',
                    name: 'grid',
                    store: config.store,
                    width: 666,
                    height: 245,
                    frame: false,
                    clicksToEdit: 1,
                    enableColumnMove: false,
                    enableColumnResize: false,
                    cm: self.cm,
                    sm: new Ext.grid.RowSelectionModel({
                        singleSelect: true,
                        listeners: {
                            rowselect: function(sm, row, rec) {
                                if (rec !== self.record) {
                                    self.record = rec;
                                    self.form.loadRecord(rec);
                                    Ext.getCmp('url-edit').setDisabled(!self.record.get('editable'));

                                    var checked = (self.record.checked || false);
                                    Ext.getCmp('urlfr').setDisabled(!checked);
                                    Ext.getCmp('url-edit').setValue(checked);
                                }
                            }
                        }
                    }),
                    listeners: {
                        afteredit: function(e) {
                            if (e.field === "namefr") {
                                // update url immediately with the current namefr value
                                if (self.updateUrlTask !== null && self.updateUrlTask !== undefined) {
                                    self.updateUrlTask.cancel();
                                }
                                self.updateUrl(e.record, e.value, false);
                            }
                        }
                    }
                },
                {
                    xtype: 'compositefield',
                    width: 666,
                    hideLabel: true,
                    frame: true,
                    items: [
                        {
                            xtype: 'fieldset',
                            title: this.resources['url-name'],
                            height: 60,
                            width: 310,
                            items: [
                                {
                                    xtype: 'compositefield',
                                    hideLabel: true,
                                    frame: true,
                                    items: [
                                        {
                                            xtype: 'displayfield',
                                            value: '<img src="' + this.imgLeft + '" style="vertical-align: top;" />',
                                            width: 25
                                        },
                                        {
                                            xtype: 'displayfield',
                                            name: 'url',
                                            height: 60,
                                            width: 225
                                        }
                                    ]
                                }
                            ]
                        },
                        {
                            xtype: 'fieldset',
                            title: this.resources['url-name'],
                            height: 60,
                            width: 344,
                            items: [
                                {
                                    xtype: 'compositefield',
                                    hideLabel: true,
                                    frame: true,
                                    items: [
                                        {
                                            xtype: 'displayfield',
                                            value: '<img src="' + this.imgRight + '" style="vertical-align: top;" />',
                                            width: 25
                                        },
                                        {
                                            xtype: 'textfield',
                                            disabled: true,
                                            name: 'urlfr',
                                            id: 'urlfr',
                                            width: 225,
                                            listeners: {
                                                blur: function(field) {
                                                    self.record.set('urlfr', field.getRawValue());
                                                }
                                            }
                                        },
                                        {
                                            xtype: 'checkbox',
                                            name: 'edit',
                                            id: 'url-edit',
                                            disabled: true,
                                            value: false,
                                            hideLabel: false,
                                            boxLabel: self.resources.edit,
                                            listeners: {
                                                check: function(chkbox, checked) {
                                                    self.record.checked = checked;
                                                    Ext.getCmp('urlfr').setDisabled(!checked);
                                                }
                                            }
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            ]
        });

        Ext.apply(this, config);
    },

    setOkButtonEnabled: function(arg) {
        var i, dialogButtons = Ext.query('.hippo-window-buttons input');
        for (i=0; i<dialogButtons.length; i++) {    // refactor into method for enabling/disabling ok button
          if (dialogButtons[i].type === 'submit') {
             dialogButtons[i].disabled = !arg ? true : false;
          }
        }
    },

    initComponent: function() {
        Hippo.Translation.Document.superclass.initComponent.call(this);

        this.bodyCfg = {
            tag: "div",
            cls: "x-panel-body"
        };

        this.store.load();

        this.store.on('update', function(store, record, operation) {
            if (record === this.record) {
                Ext.getCmp('urlfr').setRawValue(record.get('urlfr'));
            }
            if (operation === Ext.data.Record.EDIT) {
                if (this.dirty.indexOf(record) === -1) {
                    this.dirty.push(record);
                }
            }
            this.setOkButtonEnabled(!this.isRecordEmpty());
        }, this);
        this.store.on('beforesave', function() {
            var i, len;
            for (i = 0, len = this.dirty.length; i < len; i++) {
                this.dirty[i].markDirty();
            }
            this.dirty = [];
        }, this);
        this.on('render', function() {
            var self = this;
            Hippo.Translation.WicketHook.addListener(this.getEl().id, function() {
                if (self.dirty.length > 0) {
                    self.store.save();
                }
            });
        }, this);
        this.setOkButtonEnabled(false);
    },

    renderFolder: function(col, value, p, record) {
        var indent, txt;

        indent = {
            ancestors: 0
        };
        this.store.each(function(it) {
            if (it === record) {
                return false;
            }
            if ((it.get(col) !== "") || it.get('editable')) {
                indent.ancestors++;
            }
        });

        txt = "<img src='" + this.emptyImg + "' width='" + 15 * indent.ancestors + "' height='1'/>";
        if (value !== "" || record.get('editable')) {
            txt += "<img src='";
            if (record.data.type === "folder") {
                txt += this.folderImg;
            } else {
                txt += this.documentImg;
            }
            txt += "' heigth='10'/> ";
        }

        if (value === "" && record.get('editable')) {
            txt += "<font color='#ff0000'><i>" + this.resources['add-translation'] + "</i></font>";
        } else {
            txt += Ext.util.Format.htmlEncode(value);
        }
        return txt;
    }

});
