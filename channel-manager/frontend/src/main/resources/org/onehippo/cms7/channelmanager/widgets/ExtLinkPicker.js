/**
 * Copyright 2011-2018 Hippo B.V. (http://www.onehippo.com)
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
(function () {
  "use strict";

  Ext.namespace('Hippo.ChannelManager');

  Hippo.ChannelManager.ExtLinkPickerFactory = Ext.extend(Ext.util.Observable, {

    constructor: function (config) {
      Hippo.ChannelManager.ExtLinkPickerFactory.Instance = this;

      this.addEvents('pick', 'picked', 'cancel');

      this.listeners = config.listeners;

      Hippo.ChannelManager.ExtLinkPickerFactory.superclass.constructor.call(this, config);
    },

    _addListeners: function (pickedCallback, cancelCallback) {
      this.pickedCallback = pickedCallback;
      this.cancelCallback = cancelCallback;
      this.on('picked', this._onPicked, this);
      this.on('cancel', this._onCancel, this);
    },

    _removeListeners: function () {
      this.pickedCallback = null;
      this.cancelCallback = null;
      this.un('picked', this._onPicked, this);
      this.un('cancel', this._onCancel, this);
    },

    _onPicked: function (path, name) {
      this.pickedCallback.call(this, path, name);
      this._removeListeners();
    },

    _onCancel: function() {
      if (this.cancelCallback) {
        this.cancelCallback.call(this);
      }
      this._removeListeners();
    },

    openPicker: function (currentValue, pickerConfig, pickedCallback, cancelCallback) {
      this._addListeners(pickedCallback, cancelCallback);
      this._firePickEvent(currentValue, pickerConfig);
    },

    _firePickEvent: function (currentValue, pickerConfig) {
      var config = JSON.parse(JSON.stringify(pickerConfig));
      config.rootPath = encodeURI(pickerConfig.rootPath || '');
      config.initialPath = encodeURI(pickerConfig.initialPath || '');

      this.fireEvent('pick', currentValue, Ext.util.JSON.encode(config));
    }
  });

  Hippo.ChannelManager.ExtLinkPicker = Ext.extend(Ext.form.TwinTriggerField, {

    constructor: function (config) {
      this.pickerConfig = config.pickerConfig;
      this.defaultValue = config.defaultValue;
      this.displayValue = config.displayValue;
      this.renderTextFieldInvalid = false;
      this.setValue(this.defaultValue);

      Hippo.ChannelManager.ExtLinkPicker.superclass.constructor.call(this, config);
    },

    initComponent: function () {
      Hippo.ChannelManager.ExtLinkPicker.superclass.initComponent.call(this);

      this.addEvents('select');

      this.on('valid', this.validateRenderTextField, this);
      this.on('invalid', this.invalidateRenderTextField, this);
      this.on('resize', this.resizeRenderTextField, this);
      this.on('afterrender', this.updateClearButton, this);
    },

    editable: false,
    trigger1Class: 'x-form-clear-trigger',
    trigger2Class: 'x-form-search-trigger',

    onTriggerClick: function () {
      if (!this.disabled) {
        this.openPicker();
      }
    },

    onTrigger1Click: function () {
      if (!this.disabled) {
        this.picked(this.defaultValue);
      }
    },

    onTrigger2Click: function () {
      if (!this.disabled) {
        this.openPicker();
      }
    },

    setDefaultValue: function (value) {
      var oldDefaultValue = this.defaultValue;
      this.defaultValue = value;
      if (this.getValue() === oldDefaultValue) {
        this.setValue(this.defaultValue);
      }
    },

    openPicker: function () {
      if (!this.disabled) {
        Hippo.ChannelManager.ExtLinkPickerFactory.Instance.openPicker(
          this.getValue(),
          this.pickerConfig,
          Ext.createDelegate(this.picked, this),
          Ext.createDelegate(this.canceled, this)
        );
      }
    },

    picked: function (value, displayValue) {
      var newValue = Ext.isDefined(value) ? value : '',
          newDisplayValue = Ext.isDefined(displayValue) ? displayValue : '';

      this.displayValue = newDisplayValue;
      this.setValue(newValue);
      this.updateClearButton();
      this.fireEvent('select', this, newValue);
    },

    canceled: function () {
      this.renderTextField.focus();
    },

    updateClearButton: function () {
      var clearTrigger = this.getTrigger(0);
      if (this.getValue() === this.defaultValue) {
        clearTrigger.hide();
      } else {
        clearTrigger.show();
      }
    },

    setValue: function (value) {
      Hippo.ChannelManager.ExtLinkPicker.superclass.setValue.apply(this, arguments);
      if (this.rendered) {
        this.renderTextField.dom.value = this.displayValue;
      }
    },

    resizeRenderTextField: function (width, height, options) {
      if (this.renderTextField) {
        this.renderTextField.dom.style.width = this.el.dom.style.width;
      }
    },

    validateRenderTextField: function () {
      if (this.rendered) {
        this.renderTextField.removeClass('x-form-invalid');
      } else {
        this.renderTextFieldInvalid = false;
      }
    },

    invalidateRenderTextField: function () {
      if (this.rendered) {
        this.renderTextField.addClass('x-form-invalid');
      } else {
        this.renderTextFieldInvalid = true;
      }
    },

    onRender: function () {
      Hippo.ChannelManager.ExtLinkPicker.superclass.onRender.apply(this, arguments);

      this.el.dom.style.display = "none";

      this.renderTextField = Ext.DomHelper.insertBefore(this.el, {
        tag: 'input',
        type: 'text',
        readonly: 'readonly',
        tabindex: -1, // make focusable
        value: Ext.util.Format.htmlEncode(this.displayValue),
        width: this.el.getWidth()
      }, true);
      this.renderTextField.addClass('x-form-text');
      this.renderTextField.addClass('x-form-field');
      if (this.renderTextFieldInvalid) {
        this.renderTextField.addClass('x-form-invalid');
      }
    }

  });

  Ext.reg('linkpicker', Hippo.ChannelManager.ExtLinkPicker);

}());
