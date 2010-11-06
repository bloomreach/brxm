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

Hippo.Translation.Document = Ext.extend(Ext.FormPanel, {

  labelWidth: 75, // label settings here cascade unless overridden
  frame: false,
  width: 661,
  defaultType: 'textfield',

  constructor: function(config) {
    var self = this;

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
        header: "<img src='" + self.imgLeft + "' style='vertical-align: top;' /> " + self.resources['folder-name'],
        dataIndex: 'name',
        width: 323,
        renderer: self.renderFolder.createDelegate(self, [], 3),
        // use shorthand alias defined above
      }, {
        id: 'namefr',
        header: "<img src='" + self.imgRight + "' style='vertical-align: top;' /> " + self.resources['folder-name'],
        dataIndex: 'namefr',
        width: 334,
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
        width: 666,
        hideLabel: true,
        frame: true,
        items: [{
          xtype: 'fieldset',
          title: this.resources['url-name'],
          height: 60,
          items: [{
            xtype: 'compositefield',
            width: 297,
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
          title: this.resources['url-name'],
          height: 60,
          items: [{
            xtype: 'compositefield',
            width: 312,
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
              width: 225,
            }, {
              xtype: 'checkbox',
              name: 'edit',
              value: false,
              hideLabel: false,
              boxLabel: self.resources['edit'],
//              fieldLabel: 'edit',
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
    Hippo.Translation.Document.superclass.initComponent.call(this);

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
    var index = this.store.indexOf(record);
    var txt = "<img src='" + this.emptyImg + "' width='" + 15 * index + "' height='1'/><img src='";
    if (record.data.type == "folder") {
      txt += this.folderImg;
    } else {
      txt += this.documentImg;
    }
    txt += "' heigth='10'/> ";

    if (value == "") {
      txt += "<font color='#ff0000'><i>" + this.resources['add-translation'] + "</i></font>";
    } else {
      txt += String.format("{0}", value);
    }
    return txt;
  },

});
