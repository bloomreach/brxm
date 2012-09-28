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

Hippo.ChannelManager.TemplateComposer.VariantAdder = Ext.extend(Ext.FormPanel, {

    autoHeight : true,
    autoScroll : true,
    border : false,
    defaults : {
        anchor : '100%'
    },
    labelSeparator : '',
    labelWidth : 100,
    padding : 10,

    composerRestMountUrl : null,
    componentId : null,

    constructor : function (config) {
        this.title = '<span style="font-size: 140%;">' + config.title + '</span>';
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.componentId = config.componentId;
        this.variantsUuid = config.variantsUuid;

        this.globalVariantsStore = new Hippo.ChannelManager.TemplateComposer.GlobalVariantsStore({
            composerRestMountUrl: this.composerRestMountUrl,
            skipIds: config.skipVariantIds,
            variantsUuid: this.variantsUuid
        });

        Hippo.ChannelManager.TemplateComposer.VariantAdder.superclass.constructor.call(this, config);
    },

    initComponent : function () {
        Hippo.ChannelManager.TemplateComposer.VariantAdder.superclass.initComponent.apply(this, arguments);
        this.addEvents('beforeactive', 'save');
    },

    getGlobalVariantsStore: function() {
        return this.globalVariantsStore;
    },

    addVariant: function(variant) {
        Ext.Ajax.request({
            method : 'POST',
            url : this.composerRestMountUrl + '/' + this.componentId + './variant/' + variant + '?FORCE_CLIENT_HOST=true',
            success : function () {
                this.fireEvent('save', this, variant);
            },
            scope : this
        });
    },

    load : function () {
        return new Hippo.Future(function (success, fail) {
            this.globalVariantsStore.on('load', success, {single : true});
            this.globalVariantsStore.on('exception', fail, {single : true});
            this.globalVariantsStore.load();
        }.createDelegate(this));
    }
});
