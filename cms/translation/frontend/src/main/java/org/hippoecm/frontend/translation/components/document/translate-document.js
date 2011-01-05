/*
 * Copyright 2010 Hippo
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


if (Hippo.Translation.Queue == undefined) {
  (function() {

    Hippo.Translation.Queue = {
      tasks: [],
      listeners: [],

      cleanup: function() {
        for (var i = 0; i < this.tasks.length;) {
          var handle = this.tasks[i];
          if (handle.completed) {
            this.tasks.splice(i, 1);
          } else {
            i++;
          }
        }

        for (var i = 0; i < this.listeners.length;) {
          var listener = this.listeners[i];
          if (Ext.get(listener.id) == null) {
            this.listeners.splice(i, 1);
          } else {
            i++;
          }
        }
      },

      enqueue: function(callback, delay) {
        this.cleanup();

        var task = new Ext.util.DelayedTask();
        var handle = {
           extTask: task,
           started: false,
           completed: false,
           callback: callback
        };
        this.tasks.push(handle);
        task.delay(delay, function() {
          if (!handle.started) {
            handle.started = true;
            callback.call(window, function() {
              handle.completed = true;
            });
          }
        });
        return handle;
      },

      cancel: function(handle) {
        if (!handle.started) {
          handle.started = true;
          handle.extTask.cancel();
          handle.completed = true;
        }
      },

      flush: function() {
        this.cleanup();

        for (var i = 0; i < this.tasks.length; i++) {
          var handle = this.tasks[i];
          if (!handle.started) {
            handle.extTask.cancel();
            handle.started = true;
            handle.callback.call(window);
            handle.completed = true;
          }
        }

        for (var i = 0; i < this.listeners.length; i++) {
          var listener = this.listeners[i];
          var el = Ext.get(listener.id);
          if (el != null) {
            listener.callback.call(window);
          }
        }
      },
      
      addListener: function(id, callback) {
        this.listeners.push({ id: id, callback: callback });
      }

    };

  })();
  Wicket.Ajax.registerPreCallHandler(function() { 
    Hippo.Translation.Queue.flush();
  });
}

Hippo.Translation.Document = Ext.extend(Ext.FormPanel, {

  labelWidth: 75, // label settings here cascade unless overridden
  frame: false,
  width: 661,
  defaultType: 'textfield',

  constructor: function(config) {
    var self = this;

    this.dirty = [];
    this.store = config.store;
    this.codecUrl = config.codecUrl;

    this.resources = config.resources;

    this.emptyImg = config.emptyImg;
    this.folderImg = config.folderImg;
    this.documentImg = config.documentImg;

    var imageService = new Hippo.Translation.ImageService(config.imageService);
    this.imgLeft = imageService.getImage(config.sourceLanguage);
    this.imgRight = imageService.getImage(config.targetLanguage);

    this.record = null;
    this.task = null;

    this.updateUrl = function(record, value, callback) {
        var xhr;
        if (window.XMLHttpRequest) {    // Mozilla/Safari
            xhr = new XMLHttpRequest();
        } else if (window.ActiveXObject) {     // IE
            xhr = new ActiveXObject("Microsoft.XMLHTTP");
        }
        var handleResponse = function() {
          var obj = Ext.decode(xhr.responseText);
          if (!record.checked) {
            record.set('urlfr', obj.data);
          }
        };
        xhr.open('POST', self.codecUrl + "&" + Ext.urlEncode({name: value}), callback != undefined);
        xhr.setRequestHeader('Content-Type', 'application/json');
        xhr.setRequestHeader('Wicket-Ajax', "true");
        xhr.onreadystatechange = function() {
          if (xhr.readyState == 4) {
            handleResponse();
            if (callback != undefined) {
              callback.call(this);
            }
          }
        };
        xhr.send(null);
        if (callback == undefined) {
          if (xhr.status == 200) {
            handleResponse();
          }
        }
    },

    this.onKeyUp = function (field, event) {
      var rec = self.record;
      if (self.task == null) {
        self.task = Hippo.Translation.Queue.enqueue(function(callback) {
          self.task = null;
          if (!rec.checked) {
            self.updateUrl(rec, field.getRawValue(), callback);
          } else {
            callback.call(this);
          }
        }, 500);
      }
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
      columns: [{
        id: 'name',
        header: "<img src='" + self.imgLeft + "' style='vertical-align: top;' /> " + self.resources['folder-name'],
        dataIndex: 'name',
        width: 323,
        renderer: self.renderFolder.createDelegate(self, ['url'], 0)
        // use shorthand alias defined above
      }, {
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
            }
          }
        })
      }],
      isCellEditable: function(col, row) {
        var record = self.store.getAt(row);
        return record.get('editable');
      }
    });

    Hippo.Translation.Document.superclass.constructor.call(this, {

      renderTo: config.applyTo,
      
      items: [{
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
              if (rec != self.record) {
                self.task = null;
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
      			if (e.field == "namefr") {
      			  if (self.task != null) {
        			  // update url immediately with the current namefr value
      			    Hippo.Translation.Queue.cancel(self.task);
      			    self.task = null;
      			  }
    			    self.updateUrl(e.record, e.value);
    			  }
      		}
      	}
      }, {
        xtype: 'compositefield',
        width: 666,
        hideLabel: true,
        frame: true,
        items: [{
          xtype: 'fieldset',
          title: this.resources['url-name'],
          height: 60,
          width: 310,
          items: [{
            xtype: 'compositefield',
            hideLabel: true,
            frame: true,
            items: [{
              xtype: 'displayfield',
              value: '<img src="' + this.imgLeft + '" style="vertical-align: top;" />',
              width: 25
            }, {
              xtype: 'displayfield',
              name: 'url',
              height: 60,
              width: 225
            }]
          }]
        }, {
          xtype: 'fieldset',
          title: this.resources['url-name'],
          height: 60,
          width: 344,
          items: [{
            xtype: 'compositefield',
            hideLabel: true,
            frame: true,
            items: [{
              xtype: 'displayfield',
              value: '<img src="' + this.imgRight + '" style="vertical-align: top;" />',
              width: 25
            }, {
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
            }, {
              xtype: 'checkbox',
              name: 'edit',
              id: 'url-edit',
              disabled: true,
              value: false,
              hideLabel: false,
              boxLabel: self.resources['edit'],
              listeners: {
                check: function(chkbox, checked) {
                  self.record.checked = checked;
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
    Hippo.Translation.Document.superclass.initComponent.call(this);

    this.bodyCfg = {
  		tag: "div",
  		cls: "x-panel-body"
	  };

    this.store.load();

    this.saving = false;
    this.store.on('update', function(store, record, operation) {
      if (record == this.record) {
        Ext.getCmp('urlfr').setRawValue(record.get('urlfr'));
      }
      if (this.saving && operation == Ext.data.Record.EDIT) {
        if (this.dirty.indexOf(record) == -1) {
          this.dirty.push(record);
        }
      }
    }.createDelegate(this));
    this.store.on('beforewrite', function() {
      this.saving = true;
    }, this);
    this.store.on('write', function() {
      this.saving = false;
    }, this);
    this.on('render', function() {
      var self = this;
      Hippo.Translation.Queue.addListener(this.getEl().id, function() {
          for (var i = 0; i < self.dirty.length; i++) {
            self.dirty[i].markDirty();
          }
          self.dirty = [];
          self.store.save();
      });
    }, this);
  },

  renderFolder: function(col, value, p, record){
    var indent = {
      ancestors: 0
    };
    this.store.each(function(it) {
      if (it == record) {
        return false;
      }
      if ((it.get(col) != "") || it.get('editable')) {
        indent.ancestors++;
      }
    });

    var txt = "<img src='" + this.emptyImg + "' width='" + 15 * indent.ancestors + "' height='1'/>";
    if (value != "" || record.get('editable')) {
      txt += "<img src='";
      if (record.data.type == "folder") {
        txt += this.folderImg;
      } else {
        txt += this.documentImg;
      }
      txt += "' heigth='10'/> ";
    }

    if (value == "" && record.get('editable')) {
      txt += "<font color='#ff0000'><i>" + this.resources['add-translation'] + "</i></font>";
    } else {
      txt += String.format("{0}", value);
    }
    return txt;
  }

});
