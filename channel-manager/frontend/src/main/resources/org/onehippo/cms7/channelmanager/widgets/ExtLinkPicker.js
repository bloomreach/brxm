/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager');

    Hippo.ChannelManager.ExtLinkPickerFactory = Ext.extend(Ext.util.Observable, {

        constructor: function(config) {
            Hippo.ChannelManager.ExtLinkPickerFactory.Instance = this;

            this.addEvents('pick', 'picked');

            this.listeners = config.listeners;

            Hippo.ChannelManager.ExtLinkPickerFactory.superclass.constructor.call(this, config);
        },

        openPicker: function(currentValue, pickerConfig, cb) {
            var config = JSON.parse(JSON.stringify(pickerConfig));
            config.rootPath = encodeURI(pickerConfig.rootPath);
            config.initialPath = encodeURI(pickerConfig.initialPath);
            this.on('picked', cb, this, {single: true});
            this.fireEvent('pick', currentValue, Ext.util.JSON.encode(config));
        }
    });

    Hippo.ChannelManager.ExtLinkPicker = Ext.extend(Ext.form.TwinTriggerField, {

        constructor: function(config) {
            this.pickerConfig = config.pickerConfig;
            this.defaultValue = config.defaultValue;
            this.renderStripValue = config.renderStripValue;
            this.renderTextFieldInvalid = false;
            this.setValue(this.defaultValue);

            Hippo.ChannelManager.ExtLinkPicker.superclass.constructor.call(this, config);
        },

        initComponent: function() {
            Hippo.ChannelManager.ExtLinkPicker.superclass.initComponent.call(this);
            this.on('valid', this.validateRenderTextField, this);
            this.on('invalid', this.invalidateRenderTextField, this);
            this.on('resize', this.resizeRenderTextField, this);
            this.on('afterrender', this.updateClearButton, this);
        },

        editable: false,
        trigger1Class: 'x-form-clear-trigger',
        trigger2Class: 'x-form-search-trigger',

        onTriggerClick: function() {
            this.openPicker();
        },

        onTrigger1Click: function() {
            this.picked(this.defaultValue);
        },

        onTrigger2Click: function() {
            this.openPicker();
        },

        setDefaultValue: function(value) {
            var oldDefaultValue = this.defaultValue;
            this.defaultValue = value;
            if (this.getValue() === oldDefaultValue) {
                this.setValue(this.defaultValue);
            }
        },

        openPicker: function() {
            Hippo.ChannelManager.ExtLinkPickerFactory.Instance.openPicker(
                    this.getValue(),
                    this.pickerConfig,
                    Ext.createDelegate(this.picked, this)
            );
        },

        picked: function(value) {
            if (Ext.isDefined(value)) {
                this.setValue(value);
            } else {
                this.setValue('');
            }
            this.updateClearButton();
        },

        updateClearButton: function() {
            var clearTrigger = this.getTrigger(0);
            if (this.getValue() === this.defaultValue) {
                clearTrigger.hide();
            } else {
                clearTrigger.show();
            }
        },

        setValue: function(value) {
            Hippo.ChannelManager.ExtLinkPicker.superclass.setValue.apply(this, arguments);
            if (this.rendered) {
                var valueToRender = this.getValue();
                if (Ext.isDefined(this.renderStripValue)) {
                    valueToRender = valueToRender.replace(this.renderStripValue, '');
                }
                this.renderTextField.dom.value = valueToRender;
            }
        },

        resizeRenderTextField: function(width, height, options) {
            if (this.renderTextField) {
                this.renderTextField.dom.style.width = this.el.dom.style.width;
            }
        },

        validateRenderTextField: function() {
            if (this.rendered) {
                this.renderTextField.removeClass('x-form-invalid');
            } else {
                this.renderTextFieldInvalid = false;
            }
        },

        invalidateRenderTextField: function() {
            if (this.rendered) {
                this.renderTextField.addClass('x-form-invalid');
            } else {
                this.renderTextFieldInvalid = true;
            }
        },

        onRender: function() {
            Hippo.ChannelManager.ExtLinkPicker.superclass.onRender.apply(this, arguments);

            this.el.dom.style.display = "none";

            var value = this.getValue();
            if (Ext.isDefined(this.renderStripValue)) {
                value = value.replace(this.renderStripValue, '');
            }

            this.renderTextField = Ext.DomHelper.insertBefore(this.el, {
                tag: 'input',
                type: 'text',
                readonly: 'readonly',
                value: value,
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