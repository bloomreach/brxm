/**
 * Copyright 2011 Hippo
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
//"use strict";

Ext.namespace('Hippo.ChannelManager');

Hippo.ChannelManager.ExtLinkPickerFactory = Ext.extend(Ext.util.Observable, {

    constructor : function(config) {
        Hippo.ChannelManager.ExtLinkPickerFactory.Instance = this;

        this.addEvents('pick', 'picked');

        this.listeners = config.listeners;

        Hippo.ChannelManager.ExtLinkPickerFactory.superclass.constructor.call(this, config);
    },

    openPicker: function(currentValue, pickerConfig, cb) {
        this.on('picked', cb, this, {single: true});
        this.fireEvent('pick', currentValue, Ext.util.JSON.encode(pickerConfig));
    }
});

Hippo.ChannelManager.ExtLinkPicker = Ext.extend(Ext.form.TwinTriggerField,  {

    constructor : function(config) {
        this.pickerConfig = config.pickerConfig;
        this.defaultValue = config.defaultValue;
        this.setValue(this.defaultValue);

        Hippo.ChannelManager.ExtLinkPicker.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        Hippo.ChannelManager.ExtLinkPicker.superclass.initComponent.call(this);

        this.on('afterrender', this.updateClearButton);
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
        this.setValue(value);
        this.updateClearButton();
    },

    updateClearButton: function() {
        var clearTrigger = this.getTrigger(0);
        if (this.getValue() === this.defaultValue) {
            clearTrigger.hide();
        } else {
            clearTrigger.show();
        }
    }

});

Ext.reg('linkpicker', Hippo.ChannelManager.ExtLinkPicker);
