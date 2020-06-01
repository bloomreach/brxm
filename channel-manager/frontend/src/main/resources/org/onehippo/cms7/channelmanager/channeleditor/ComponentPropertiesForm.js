/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
(function ($) {

  "use strict";

  Ext.namespace('Hippo.ChannelManager.ChannelEditor');

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

  Hippo.ChannelManager.ChannelEditor.ComponentPropertiesForm = Ext.extend(Ext.FormPanel, {

    mountId: null,
    variant: null,
    newVariantId: null,
    composerRestMountUrl: null,
    siteContextPath: null,
    componentId: null,
    locale: null,
    markedDirty: false,

    store: null,

    constructor: function (config) {
      this.variant = config.variant;
      this.newVariantId = this.variant.id;
      this.mountId = config.mountId;
      this.composerRestMountUrl = config.composerRestMountUrl;
      this.siteContextPath = config.siteContextPath;
      this.locale = config.locale;
      this.componentId = config.componentId;
      this.lastModified = config.lastModified;
      this.isReadOnly = config.isReadOnly;
      this.hasComponent = false;

      Hippo.ChannelManager.ChannelEditor.ComponentPropertiesForm.superclass.constructor.call(this, Ext.apply(config, {
        cls: 'componentPropertiesForm qa-properties-form br-form',
        maskDisabled: false
      }));
    },

    createCopy: function (newVariant) {
      var copy = new Hippo.ChannelManager.ChannelEditor.ComponentPropertiesForm(this.initialConfig);
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
          text: Hippo.ChannelManager.ChannelEditor.Resources['properties-form-button-delete'],
          cls: 'btn btn-default br-delete-variant-btn qa-delete-button',
          handler: function () {
            Ext.Ajax.request({
              method: 'DELETE',
              url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id),
              headers: {
                'lastModifiedTimestamp': this.lastModified,
                'contextPath': this.siteContextPath
              },
              success: function () {
                this.fireEvent('propertiesDeleted', this.variant.id);
              },
              failure: this._onUpdateVariantFailure,
              scope: this
            });
          },
          scope: this
        });
        if (!this.isReadOnly) {
          buttons.push(this.deleteButton);
        }
      }

      Ext.apply(this, {
        autoHeight: true,
        border: false,
        padding: 16,
        autoScroll: true,
        labelWidth: 120,
        labelSeparator: '',
        monitorValid: !this.isReadOnly,
        defaults: {
          anchor: '100%'
        },
        plugins: Hippo.ChannelManager.MarkRequiredFields,
        buttons: buttons
      });

      Hippo.ChannelManager.ChannelEditor.ComponentPropertiesForm.superclass.initComponent.apply(this, arguments);

      this.addEvents('propertiesChanged', 'variantDirty', 'variantPristine', 'propertiesSaved', 'close', 'propertiesDeleted', 'componentLocked');
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

    submitForm: function (onSuccess, onFail) {
      var uncheckedValues = {},
        form = this.getForm();

      if (!this.rendered) {
        // if the form has not been rendered the values were never changed,
        // so no need to submit
        onSuccess();
        return;
      }

      form.items.each(function (item) {
        if (item instanceof Ext.form.Checkbox) {
          if (!item.checked) {
            uncheckedValues[item.name] = 'off';
          }
        }
      });

      form.submit({
        headers: {
          'Move-To': this.newVariantId,
          'lastModifiedTimestamp': this.lastModified,
          'contextPath': this.siteContextPath
        },
        params: uncheckedValues,
        url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id),
        method: 'PUT',
        success: function () {
          this.fireEvent('propertiesSaved', this.newVariantId);
          this._fireVariantDirtyOrPristine();
          onSuccess(this.newVariantId);
        },
        failure: function (form, action) {
          this._onUpdateVariantFailure(action.response);
          onFail();
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
        html: "<div style='padding:5px' align='center'>" + Hippo.ChannelManager.ChannelEditor.Resources['properties-form-no-properties'] + "</div>",
        xtype: "panel",
        autoWidth: true,
        layout: 'fit'
      });
    },

    _initFields: function () {
      var lastGroupLabel = null;

      this.store.each(function (record) {
        var groupLabel;

        if (record.get('hiddenInChannelManager') === false) {
          groupLabel = record.get('groupLabel');
          if (groupLabel !== lastGroupLabel && groupLabel !== null) {
            this.add({
              cls: 'field-group-title',
              text: this._isTemplatePickerField(record) ? '' : Ext.util.Format.htmlEncode(groupLabel),
              xtype: 'label'
            });
            lastGroupLabel = groupLabel;
          }
          this._initField(record);
        }
      }, this);
    },

    _isTemplatePickerField: function (record) {
      return record.get('name') === 'org.hippoecm.hst.core.component.template';
    },

    _initField: function (record) {
      var field = this._addField(record),
        initialValue = record.get('initialValue'),
        sanitizedValue;

      if (Ext.isEmpty(initialValue)) {
        sanitizedValue = field.getValue();

        // 'datefield' converts the initial string values to Date objects while we want to store strings,
        // so convert Dates back to strings.
        if (Ext.isDate(sanitizedValue)) {
          sanitizedValue = sanitizedValue.format(field.format);
        }

        // Store the initial value of each field without triggering 'update' events. The value is stored
        // 'again' in the record to ensure that it can be compared against the initial value to determine
        // the dirty state of the field. Especially checkboxes can accept various values ('on', 'true',
        // etc) that will all be returned as 'true' by getValue(). Also dates have to be converted back
        // to strings again.
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
        displayValue = record.get('displayValue'),
        field;

      if (Ext.isEmpty(value)) {
        value = defaultValue;
      }

      switch (xtype) {
        case 'combo':
          field = this._addComboBox(record, defaultValue, value);
          break;
        default:
          field = this._addComponent(xtype, record, defaultValue, value, displayValue);
      }

      return field;
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

      this.add({
        xtype: 'label',
        cls: 'x-form-item-label',
        text: record.get('label')
      });

      return this.add({
        xtype: 'combo',
        ctCls: 'br-combo',
        hideLabel: true,
        store: new Ext.data.ArrayStore({
          fields: [
            'id',
            'displayText'
          ],
          data: createData()
        }),
        value: initialValue,
        allowBlank: !record.get('required'),
        hiddenName: record.get('name'),
        typeAhead: true,
        mode: 'local',
        triggerAction: 'all',
        selectOnFocus: true,
        valueField: 'id',
        displayField: 'displayText',
        disabled: this.isReadOnly,
        listeners: {
          afterrender: fixComboLeftPadding,
          select: function (combo, comboRecord) {
            record.set('value', comboRecord.get('id') || defaultValue);
            record.commit();
          }
        }
      });
    },

    _addComponent: function (xtype, record, defaultValue, initialValue, displayValue) {

      function getNewValue(field) {
        var newValue = field.getValue();
        if (!Ext.isDefined(newValue) || newValue === '') {
          newValue = field.defaultValue;
        } else if (newValue instanceof Date) {
          newValue = newValue.format(field.format);
        }
        return newValue;
      }

      function commitValueOrDefault (field) {
        if (field.isValid()) {
          record.set('value', getNewValue(field));
          record.commit();
        }
      }

      function updateValue (field) {
        var newValue = field.getValue();
        if (newValue instanceof Date) {
          newValue = newValue.format(field.format);
        }
        record.set('value', newValue);
      }

      this.add({
        xtype: 'label',
        cls: 'x-form-item-label',
        text: this._createLabel(record)
      });

      var propertyFieldConfig = {
          xtype: xtype,
          hideLabel: true,
          value: initialValue,
          defaultValue: defaultValue,
          displayValue: displayValue,
          allowBlank: !record.get('required'),
          name: record.get('name'),
          enableKeyEvents: !this.isReadOnly,
          disabled: this.isReadOnly,
          listeners: {
            change: commitValueOrDefault,
            select: commitValueOrDefault,
            valid: updateValue,
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

        switch (xtype) {
          case 'checkbox':
            propertyFieldConfig.ctCls = 'br-checkbox';
            propertyFieldConfig.checked = (initialValue === true || initialValue === 'true' || initialValue === '1' || String(initialValue).toLowerCase() === 'on');
            propertyFieldConfig.listeners.check = commitValueOrDefault;
            break;
          case 'datefield':
            propertyFieldConfig.ctCls = 'br-datefield';
            propertyFieldConfig.editable = false;
            propertyFieldConfig.format = 'Y-m-d';
            break;
          case 'linkpicker':
            propertyFieldConfig.ctCls = 'br-linkpicker';
            propertyFieldConfig.pickerConfig = {
              configuration: record.get('pickerConfiguration'),
              remembersLastVisited: record.get('pickerRemembersLastVisited'),
              initialPath: record.get('pickerInitialPath'),
              isRelativePath: record.get('pickerPathIsRelative'),
              rootPath: record.get('pickerRootPath'),
              selectableNodeTypes: record.get('pickerSelectableNodeTypes')
            };
            break;
        }

        return this.add(propertyFieldConfig);
    },

    _createLabel: function(record) {
      return record.get('label') + (record.get('required') ? ' *' : '');
    },

    _loadException: function (proxy, type, actions, options, response) {
      var errorText = Hippo.ChannelManager.ChannelEditor.Resources['properties-form-load-exception-text'].format(actions);
      if (type === 'response') {
        errorText += '\n' + Hippo.ChannelManager.ChannelEditor.Resources['properties-form-load-exception-response'].format(response.statusText, response.status, options.url);
      }

      this.add({
        xtype: 'label',
        text: errorText,
        fieldLabel: Hippo.ChannelManager.ChannelEditor.Resources['properties-form-load-exception-field-label']
      });
    },

    load: function () {
      if (this.store) {
        return this._reloadState();
      } else {
        return this._loadStore();
      }
    },

    _reloadState: function () {
      this._fireVariantDirtyOrPristine();
      this.fireEvent('propertiesLoaded', this);
      return $.Deferred().resolve().promise();
    },

    _loadStore: function () {
      var result = $.Deferred(),
      proxy = new Ext.data.HttpProxy({
        method: 'GET',
        url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/' + this.locale,
        headers: {
          'contextPath': this.siteContextPath
        }
      });

      this.store = new Ext.data.JsonStore({
        autoLoad: false,
        proxy: proxy,
        root: 'properties',
        fields: [
          'name', 'value', 'initialValue', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation', 'defaultValue',
          'pickerConfiguration', 'pickerInitialPath', 'pickerRemembersLastVisited', 'pickerPathIsRelative', 'pickerRootPath', 'pickerSelectableNodeTypes',
          'dropDownListValues', 'dropDownListDisplayValues', 'hiddenInChannelManager', 'groupLabel', 'displayValue'
        ]
      });

      this.store.on('load', function () {
        this._loadProperties();
        result.resolve();
      }, this);

      this.store.on('exception', function (proxy, type, actions, options, response) {
        if (type === 'response') {
          result.reject(response);
        } else {
          this._loadException(proxy, type, actions, options, response);
          result.reject();
        }
      }, this);

      this._initStoreListeners();
      this.store.load();

      return result.promise();
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

    _onUpdateVariantFailure: function (response) {
      if (response.status === 400) {
        var jsonData = Ext.util.JSON.decode(response.responseText);
        if (jsonData && jsonData.data.error === 'ITEM_ALREADY_LOCKED') {
          this.fireEvent('componentLocked', jsonData.data);
          return;
        }
      }
      this.fireEvent('componentLocked', {
        error: 'UNKNOWN'
      });
    },

    _fireVariantDirtyOrPristine: function () {
      if (this.isDirty()) {
        this.fireEvent('variantDirty');
      } else {
        this.fireEvent('variantPristine');
      }
    },

    firePropertiesChanged: function () {
      if (this.store !== null) {
        var propertiesMap = {};
        this.store.each(function (record) {
          var name = record.get('name'),
            value = record.get('value');
          propertiesMap[name] = value;
        });
        this.fireEvent('propertiesChanged', this, propertiesMap);
      }
    },

    disableDelete: function () {
      if (this.deleteButton && !this.deleteButton.disabled) {
        this.deleteButton.disable();
      }
    },

    enableDelete: function () {
      if (this.deleteButton && this.deleteButton.disabled) {
        this.deleteButton.enable();
      }
    },

    hideDelete: function () {
      if (this.deleteButton) {
        this.deleteButton.hide();
      }
    }

  });

  Ext.reg('Hippo.ChannelManager.ChannelEditor.ComponentPropertiesForm', Hippo.ChannelManager.ChannelEditor.ComponentPropertiesForm);

}(jQuery));
