/*
 *  Copyright 2010-2012 Hippo.
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
"use strict";
Ext.namespace('Hippo.ChannelManager.TemplateComposer');

Hippo.ChannelManager.TemplateComposer.PlainVariantAdder = Ext.extend(Hippo.ChannelManager.TemplateComposer.VariantAdder, {

    initComponent: function () {
        var comboBox = new Ext.form.ComboBox({
            store : this.getGlobalVariantsStore(),
            valueField : 'id',
            displayField : 'name',
            triggerAction : 'all'
        });

        this.items = [ comboBox ];
        this.buttons = [
            {
                text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-add-variant'],
                handler: function() {
                    var variant = comboBox.getValue();
                    this.saveVariant(variant);
                },
                scope: this
            }
        ];

        Hippo.ChannelManager.TemplateComposer.PlainVariantAdder.superclass.initComponent.apply(this, arguments);
    }

});
