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

Hippo.ChannelManager.TemplateComposer.PropertiesEditor = Ext.extend(Ext.Panel, {

    componentId: null,
    variant: null,
    propertiesForm: null,

    constructor: function(config) {
        Hippo.ChannelManager.TemplateComposer.PropertiesEditor.superclass.constructor.call(this, config);
        this.componentId = config.componentId;
        this.variant = config.variant;
        this.propertiesForm = config.propertiesForm;
    },

    load: function() {
        return this.propertiesForm.load();
    }

});


