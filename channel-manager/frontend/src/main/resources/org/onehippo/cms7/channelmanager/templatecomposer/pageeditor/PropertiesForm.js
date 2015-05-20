/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
(function () {

  "use strict";

  Ext.namespace('Hippo.ChannelManager.TemplateComposer');

  function copyStore (store) {
    var newRecords = [], newStore;
    store.each(function (record) {
      newRecords.push(record.copy());
    });
    newStore = new Ext.data.Store({
      recordType: store.recordType
    });
    newStore.add(newRecords);
    return newStore;
  }

  function fixComboLeftPadding () {
    // Workaround, the padding-left which gets set on a combo element places the combo a little too much to the right.
    // Removing the style attribute after render fixes the layout
    var formElement = this.el.findParent('.x-form-element');
    formElement.removeAttribute('style');
  }

  Hippo.ChannelManager.TemplateComposer.PropertiesForm = Ext.extend(Ext.FormPanel, {

    mountId: null,
    variant: null,
    newVariantId: null,
    composerRestMountUrl: null,
    componentId: null,
    locale: null,
    markedDirty: false,

    store: null,

    constructor: function (config) {
      this.variant = config.variant;
      this.newVariantId = this.variant.id;
      this.mountId = config.mountId;
      this.composerRestMountUrl = config.composerRestMountUrl;
      this.locale = config.locale;
      this.componentId = config.componentId;
      this.lastModifiedTimestamp = config.lastModifiedTimestamp;

      this.saveEnabledChecks = [];

      Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.constructor.call(this, Ext.apply(config, {
        cls: 'templateComposerPropertiesForm'
      }));
    },

    createCopy: function (newVariant) {
      var copy = new Hippo.ChannelManager.TemplateComposer.PropertiesForm(this.initialConfig);
      copy.variant = newVariant;
      copy.store = copyStore(this.store);
      copy._initStoreListeners();
      copy._loadProperties();
      return copy;
    },

    initComponent: function () {
      var buttons = [];

      if (this.variant.id !== 'hippo-default') {
        this.deleteButton = new Ext.Button({
          text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-delete'],
          handler: function () {
            Ext.Ajax.request({
              method: 'DELETE',
              url: this.composerRestMountUrl + '/' + this.componentId + './' +
              encodeURIComponent(this.variant.id) + '?FORCE_CLIENT_HOST=true',
              success: function () {
                this.fireEvent('propertiesDeleted', this.variant.id);
              },
              scope: this
            });
          },
          scope: this
        });
        buttons.push(this.deleteButton);
        buttons.push('->');
      }

      this.saveButton = new Ext.Button({
        text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-save'],
        handler: this._submitForm,
        scope: this,
        formBind: true
      });
      buttons.push(this.saveButton);
      buttons.push({
        text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-close'],
        scope: this,
        handler: function () {
          this.fireEvent('close');
        }
      });

      this.addSaveEnabledCheck(this.isDirty.bind(this));

      Ext.apply(this, {
        autoHeight: true,
        border: false,
        padding: 16,
        autoScroll: true,
        labelWidth: 120,
        labelSeparator: '',
        monitorValid: true,
        defaults: {
          anchor: '100%'
        },
        plugins: Hippo.ChannelManager.MarkRequiredFields,
        buttons: buttons
      });

      Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.initComponent.apply(this, arguments);

      this.addEvents('propertiesChanged', 'variantDirty', 'variantPristine', 'propertiesSaved', 'close', 'propertiesDeleted');
    },

    setNewVariant: function (newVariantId) {
      this.newVariantId = newVariantId;
    },

    getVisibleHeight: function () {
      if (this.rendered) {
        return this.getHeight();
      }
      return 0;
    },

    markDirty: function (isDirty) {
      this.markedDirty = isDirty;
      this._fireVariantDirtyOrPristine();
    },

    isDirty: function () {
      return this.markedDirty || this._isStoreDirty();
    },

    _isStoreDirty: function () {
      var isDirty = false;
      if (this.store !== null) {
        this.store.each(function (record) {
          if (!record.get('hiddenInChannelManager')) {
            var value = record.get('value'),
              initialValue = record.get('initialValue');
            if (String(value) !== String(initialValue)) {
              isDirty = true;
              return false;
            }
          }
        });
      }
      return isDirty;
    },

    _submitForm: function () {
      var uncheckedValues = {},
        form = this.getForm();

      form.items.each(function (item) {
        if (item instanceof Ext.form.Checkbox) {
          if (!item.checked) {
            uncheckedValues[item.name] = 'off';
          }
        }
      });

      form.submit({
        headers: {
          'FORCE_CLIENT_HOST': 'true',
          'X-lastModifiedTimestamp': this.lastModifiedTimestamp
        },
        params: uncheckedValues,
        url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/rename/' + encodeURIComponent(this.newVariantId) + '?FORCE_CLIENT_HOST=true',
        method: 'POST',
        success: function () {
          this.fireEvent('propertiesSaved', this.newVariantId);
          this._fireVariantDirtyOrPristine();
        },
        failure: function () {
          Hippo.Msg.alert(Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['toolkit-store-error-message-title'],
            Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['toolkit-store-error-message'], function () {
              Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').pageContainer.pageContext = null;
              // reload channel manager
              Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').pageContainer.refreshIframe();
            });
        },
        scope: this
      });
    },

    _loadProperties: function () {
      if (this.store.getCount() === 0) {
        this._initZeroFields();
      } else {
        this._initFields();
      }

      // do a shallow layout of the form to ensure our visible height is correct
      this.doLayout(true);

      this.fireEvent('propertiesLoaded', this);
    },

    _initZeroFields: function () {
      this.add({
        html: "<div style='padding:5px' align='center'>" + Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-no-properties'] + "</div>",
        xtype: "panel",
        autoWidth: true,
        layout: 'fit'
      });
      this.saveButton.hide();
    },

    _initFields: function () {
      var lastGroupLabel = null;

      this.store.each(function (record) {
        var groupLabel;

        if (record.get('hiddenInChannelManager') === false) {
          groupLabel = record.get('groupLabel');
          if (groupLabel !== lastGroupLabel) {
            this.add({
              cls: 'field-group-title',
              text: Ext.util.Format.htmlEncode(groupLabel),
              xtype: 'label'
            });
            lastGroupLabel = groupLabel;
          }
          this._initField(record);
        }
      }, this);

      this.add({
        xtype: 'Hippo.ChannelManager.TemplateComposer.ValidatorField',
        validator: this.isSaveEnabled.bind(this)
      });

      this.saveButton.show();
    },

    _initField: function (record) {
      var field = this._addField(record),
        initialValue = record.get('initialValue'),
        sanitizedValue;

      if (Ext.isEmpty(initialValue)) {
        sanitizedValue = field.getValue();
        // Store the initial value of each field without triggering 'update' events. The value is stored
        // 'again' in the record to ensure that it can be compared against the initial value to determine
        // the dirty state of the field. Especially checkboxes can accept various values ('on', 'true',
        // etc) that will all be returned as 'true' by getValue().
        record.beginEdit();
        record.set('initialValue', sanitizedValue);
        record.set('value', sanitizedValue);
        record.commit(true);
      }
    },

    _addField: function (record) {
      var defaultValue = record.get('defaultValue'),
        value = record.get('value'),
        xtype = record.get('type'),
        field;

      if (Ext.isEmpty(value)) {
        value = defaultValue;
      }

      switch (xtype) {
        case 'documentcombobox':
          field = this._addDocumentComboBox(record, defaultValue, value);
          break;
        case 'combo':
          field = this._addComboBox(record, defaultValue, value);
          break;
        default:
          field = this._addComponent(xtype, record, defaultValue, value);
      }

      return field;
    },

    _addDocumentComboBox: function (record, defaultValue, initialValue) {
      var comboStore, propertyField, createDocumentLinkId;

      comboStore = new Ext.data.JsonStore({
        root: 'data',
        url: this.composerRestMountUrl + '/' + this.mountId + './documents/' + record.get('docType') + '?FORCE_CLIENT_HOST=true',
        fields: ['path']
      });

      propertyField = this.add({
        fieldLabel: record.get('label'),
        xtype: 'combo',
        allowBlank: !record.get('required'),
        name: record.get('name'),
        value: initialValue,
        defaultValue: defaultValue,
        store: comboStore,
        forceSelection: true,
        triggerAction: 'all',
        displayField: 'path',
        valueField: 'path',
        listeners: {
          afterrender: fixComboLeftPadding,
          select: function (combo, comboRecord) {
            record.set('value', comboRecord.get('path') || defaultValue);
            record.commit();
          }
        }
      });

      if (record.get('allowCreation')) {
        createDocumentLinkId = Ext.id();

        this.add({
          bodyCfg: {
            tag: 'div',
            cls: 'create-document-link',
            html: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-document-link-text'].format('<a href="#" id="' + createDocumentLinkId + '">&nbsp;', '&nbsp;</a>&nbsp;')
          },
          border: false
        });

        this.on('afterlayout', function () {
          Ext.get(createDocumentLinkId).on("click", this._createDocument, this, {
            docType: record.get('docType'),
            docLocation: record.get('docLocation'),
            comboId: propertyField.id
          });
        }, this, {single: true});
      }

      return propertyField;
    },

    _createDocument: function (ev, target, options) {
      var createUrl, createDocumentWindow;

      createUrl = this.composerRestMountUrl + '/' + this.mountId + './create?FORCE_CLIENT_HOST=true';
      createDocumentWindow = new Ext.Window({
        title: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-window-title'],
        height: 200,
        width: 450,
        modal: true,
        items: [
          {
            xtype: 'form',
            height: 200,
            labelWidth: 120,
            id: 'createDocumentForm',
            defaults: {
              labelSeparator: '',
              anchor: '100%'
            },
            items: [
              {
                xtype: 'textfield',
                fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-field-name'],
                allowBlank: false
              },
              {
                xtype: 'textfield',
                disabled: true,
                fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-field-location'],
                value: options.docLocation
              }
            ]
          }
        ],
        layout: 'fit',
        buttons: [
          {
            text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-button'],
            handler: function () {
              var createDocForm = Ext.getCmp('createDocumentForm').getForm();
              createDocForm.submit();
              options.docName = createDocForm.items.get(0).getValue();

              if (options.docName === '') {
                return;
              }
              createDocumentWindow.hide();

              Ext.Ajax.request({
                url: createUrl,
                params: options,
                success: function () {
                  Ext.getCmp(options.comboId).setValue(options.docLocation + "/" + options.docName);
                },
                failure: function () {
                  Hippo.Msg.alert(Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-message'],
                    Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-failed'],
                    function () {
                      Hippo.ChannelManager.TemplateComposer.Instance.initComposer();
                    }
                  );
                }
              });

            }
          }
        ]
      });
      createDocumentWindow.addButton(
        {
          text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-button-cancel']
        },
        function () {
          this.hide();
        },
        createDocumentWindow
      );
      createDocumentWindow.show();
    },

    _addComboBox: function (record, defaultValue, initialValue) {

      function createData () {
        var data = [],
          comboBoxValues = record.get('dropDownListValues'),
          comboBoxDisplayValues = record.get('dropDownListDisplayValues');

        comboBoxValues.forEach(function (value, index) {
          var displayValue = comboBoxDisplayValues[index];
          data.push([value, displayValue]);
        });

        return data;
      }

      return this.add({
        xtype: 'combo',
        fieldLabel: record.get('label'),
        store: new Ext.data.ArrayStore({
          fields: [
            'id',
            'displayText'
          ],
          data: createData()
        }),
        value: initialValue,
        hiddenName: record.get('name'),
        typeAhead: true,
        mode: 'local',
        triggerAction: 'all',
        selectOnFocus: true,
        valueField: 'id',
        displayField: 'displayText',
        listeners: {
          afterrender: fixComboLeftPadding,
          select: function (combo, comboRecord) {
            record.set('value', comboRecord.get('id') || defaultValue);
            record.commit();
          }
        }
      });
    },

    _addComponent: function (xtype, record, defaultValue, initialValue) {

      function commitValueOrDefault (field) {
        var newValue = field.getValue();
        if (typeof(newValue) === 'undefined' || (typeof(newValue) === 'string' && newValue.length === 0) || newValue === field.defaultValue) {
          field.setValue(field.defaultValue);
        }
        record.set('value', newValue);
        record.commit();
      }

      function updateValue (field) {
        var newValue = field.getValue();
        record.set('value', newValue);
      }

      var propertyFieldConfig = {
          fieldLabel: record.get('label'),
          xtype: xtype,
          value: initialValue,
          defaultValue: defaultValue,
          allowBlank: !record.get('required'),
          name: record.get('name'),
          enableKeyEvents: true,
          listeners: {
            change: commitValueOrDefault,
            select: commitValueOrDefault,
            keyup: updateValue,
            specialkey: function (field, event) {
              if (event.getKey() === event.ENTER) {
                commitValueOrDefault(field);
              }
            },
            afterrender: function (field) {
              // workaround, the padding-left which gets set on the element, let the right box side disappear,
              // removing the style attribute after render fixes the layout
              var formElement = this.el.findParent('.x-form-element');
              formElement.removeAttribute('style');
            }
          }
        };

      if (xtype === 'checkbox') {
        propertyFieldConfig.checked = (initialValue === true || initialValue === 'true' || initialValue === '1' || String(initialValue).toLowerCase() === 'on');
        propertyFieldConfig.listeners.check = commitValueOrDefault;
      } else if (xtype === 'linkpicker') {
        propertyFieldConfig.renderStripValue = /^\/?(?:[^\/]+\/)*/g;
        propertyFieldConfig.pickerConfig = {
          configuration: record.get('pickerConfiguration'),
          remembersLastVisited: record.get('pickerRemembersLastVisited'),
          initialPath: record.get('pickerInitialPath'),
          isRelativePath: record.get('pickerPathIsRelative'),
          rootPath: record.get('pickerRootPath'),
          selectableNodeTypes: record.get('pickerSelectableNodeTypes')
        };
      }

      return this.add(propertyFieldConfig);
    },

    _loadException: function (proxy, type, actions, options, response) {
      var errorText = Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-load-exception-text'].format(actions);
      if (type === 'response') {
        errorText += '\n' + Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-load-exception-response'].format(response.statusText, response.status, options.url);
      }

      this.add({
        xtype: 'label',
        text: errorText,
        fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-error-field-label']
      });
    },

    load: function () {
      if (this.store) {
        return this._loadDirtyState();
      } else {
        return this._loadStore();
      }
    },

    _loadDirtyState: function () {
      return new Hippo.Future(function (success) {
        this._fireVariantDirtyOrPristine();
        success();
      }.createDelegate(this));
    },

    _loadStore: function () {
      return new Hippo.Future(function (success, fail) {
        this.store = new Ext.data.JsonStore({
          autoLoad: false,
          method: 'GET',
          root: 'properties',
          fields: [
            'name', 'value', 'initialValue', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation', 'defaultValue',
            'pickerConfiguration', 'pickerInitialPath', 'pickerRemembersLastVisited', 'pickerPathIsRelative', 'pickerRootPath', 'pickerSelectableNodeTypes',
            'dropDownListValues', 'dropDownListDisplayValues', 'hiddenInChannelManager', 'groupLabel'
          ],
          url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/' + this.locale + '?FORCE_CLIENT_HOST=true'
        });
        this.store.on('load', function () {
          this._loadProperties();
          success();
        }, this);
        this.store.on('exception', function () {
          this._loadException.apply(this, arguments);
          fail();
        }, this);
        this._initStoreListeners();
        this.store.load();
      }.createDelegate(this));
    },

    _initStoreListeners: function () {
      this.store.on('update', this._onPropertiesChanged, this);
    },

    _onPropertiesChanged: function (store, record, operation) {
      this._fireVariantDirtyOrPristine();
      if (operation === Ext.data.Record.COMMIT) {
        this.firePropertiesChanged();
      }
    },

    _fireVariantDirtyOrPristine: function () {
      if (this.isDirty()) {
        this.fireEvent('variantDirty');
      } else {
        this.fireEvent('variantPristine');
      }
    },

    firePropertiesChanged: function () {
      this._doFirePropertiesChanges('value');
    },

    fireInitialPropertiesChanged: function () {
      this._doFirePropertiesChanges('initialValue');
    },

    _doFirePropertiesChanges: function (valueField) {
      if (this.store !== null) {
        var propertiesMap = {};
        this.store.each(function (record) {
          var name = record.get('name'),
            value = record.get(valueField);
          propertiesMap[name] = value;
        });
        this.fireEvent('propertiesChanged', propertiesMap);
      }
    },

    /**
     * Add a function that returns whether the save button of this form should be enabled or not.
     * When all check functions return true the save button is enabled. If any of the check functions
     * returns false, the save button is disabled.
     * @param fn the save-enabled check function to add.
     */
    addSaveEnabledCheck: function (fn) {
      this.saveEnabledChecks.push(fn);
    },

    isSaveEnabled: function () {
      var isEnabled = true;
      this.saveEnabledChecks.some(function (checkSaveEnabled) {
        isEnabled = checkSaveEnabled();
        return !isEnabled;
      });
      return isEnabled;
    },

    disableDelete: function () {
      if (this.deleteButton) {
        this.deleteButton.disable();
      }
    },

    enableDelete: function () {
      if (this.deleteButton) {
        this.deleteButton.enable();
      }
    }

  });

  Ext.reg('Hippo.ChannelManager.TemplateComposer.PropertiesForm', Hippo.ChannelManager.TemplateComposer.PropertiesForm);

}());
