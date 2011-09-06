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
"use strict";

Ext.namespace('Hippo.ChannelManager');

Hippo.ChannelManager.ExtLinkPickerContainer = Ext.extend(Ext.form.TwinTriggerField,  {

    constructor : function(config) {
        if (config.eventHandlerId !== undefined) {
            Hippo.ChannelManager.ExtLinkPickerContainer.prototype.eventHandlerId = config.eventHandlerId;
        }
        this.pickerConfig = config.pickerConfig;

        Hippo.ChannelManager.ExtLinkPickerContainer.superclass.constructor.call(this, config);
    },

    initComponent : function() {
        Hippo.ChannelManager.ExtLinkPickerContainer.superclass.initComponent.call(this);

        this.addEvents('picked');

        this.on('picked', this.picked);
    },

    editable: false,
    trigger1Class: 'x-form-clear-trigger',
    trigger2Class: 'x-form-search-trigger',

    onDestroy : function(){
        Hippo.ChannelManager.ExtLinkPickerContainer.superclass.onDestroy.call(this);
    },
    
    afterRender: function(){
        Hippo.ChannelManager.ExtLinkPickerContainer.superclass.afterRender.call(this);
    },

    onTrigger1Click : function() {
        this.el.dom.value = '';
    },

    onTrigger2Click : function() {
        if (this.eventHandlerId === undefined) {
            console.error("Cannot open picker dialog: no picker event handler registered");
            return;
        }
        var eventHandler = Ext.getCmp(this.eventHandlerId);
        if (eventHandler !== undefined) {
            eventHandler.fireEvent('pick', this.getId(), this.el.dom.value, Ext.util.JSON.encode(this.pickerConfig));
        } else {
            console.error("No picker event handler registered with id '" + this.eventHandlerId);
        }
    },

    picked: function(value) {
        this.el.dom.value = value;
    }

});

Ext.reg('linkpicker', Hippo.ChannelManager.ExtLinkPickerContainer);
