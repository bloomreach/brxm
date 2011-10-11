/*
 *  Copyright 2010 Hippo.
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
Ext.namespace('Hippo.ChannelManager.TemplateComposer.PageModel');

(function() {
    Hippo.ChannelManager.TemplateComposer.PageModel.FactoryImpl = function() {
    };

    Hippo.ChannelManager.TemplateComposer.PageModel.FactoryImpl.prototype = {
        createModel: function(element, cfg) {

            var id, name, path, type, xtype;
            if (typeof element !== 'undefined' && element !== null) {
                id = element.getAttribute(HST.ATTR.ID);
                name = element.getAttribute('hst:name');
                path = element.getAttribute('hst:path');
                type = element.getAttribute('hst:type');
                xtype = element.getAttribute('hst:xtype');
            }

            var config = {
                id:   id,
                name: name,
                path: path,
                type: type,
                xtype: xtype,
                element: element,

                isRoot: false,
                parentId: null,
                children: []
            };
            Ext.apply(config, cfg);

            return new Hippo.ChannelManager.TemplateComposer.PageModel.Component(config);
        },

        createRecord : function(model) {
            return new Hippo.ChannelManager.TemplateComposer.PageModel.ComponentRecord(model);
        }
    };

    Hippo.ChannelManager.TemplateComposer.PageModel.Factory = new Hippo.ChannelManager.TemplateComposer.PageModel.FactoryImpl();

    Hippo.ChannelManager.TemplateComposer.PageModel.Component = function(cfg) {
        Ext.apply(this, cfg);
    };

    Hippo.ChannelManager.TemplateComposer.PageModel.Component.prototype = {
    };

    //TODO: update this one for dropFromParent stuff
    Hippo.ChannelManager.TemplateComposer.PageModel.ComponentRecord = Ext.data.Record.create([
        {name: 'id', mapping: 'id'},
        {name: 'name', mapping: 'name'},
        {name: 'type', mapping: 'type'},
        {name: 'xtype', mapping: 'xtype'},
        {name: 'path', mapping: 'path'},
        {name: 'parentId', mapping: 'parentId'},
        {name: 'componentClassName', mapping: 'componentClassName'},
        {name: 'template', mapping: 'template'},
        {name: 'element', mapping: 'element'}

    ]);

    Hippo.ChannelManager.TemplateComposer.PageModel.ReadRecord = Ext.data.Record.create([
        {name: 'id', mapping: 'id'},
        {name: 'name', mapping: 'name'},
        {name: 'path', mapping: 'path'},
        {name: 'parentId', mapping: 'parentId'},
        {name: 'componentClassName', mapping: 'componentClassName'},
        {name: 'template', mapping: 'template'},
        {name: 'type', mapping: 'type'},
        {name: 'xtype', mapping: 'xtype'},
        {name: 'children', mapping: 'children'},
        {name: 'element', convert: function(v, record) {
            var frameDoc = Ext.getCmp('Iframe').getFrameDocument();
            var element = frameDoc.getElementById(record.id);
            if (element == null) {
                element = document.createElement('div');
                element.id = record.id;
                element.setAttribute(HST.ATTR.ID, record.id);
                element.setAttribute('hst:name', record.name);
                element.setAttribute(HST.ATTR.TYPE, record.type);
                element.setAttribute(HST.ATTR.XTYPE, record.xtype);
                element.setAttribute('hst:temporary', true);
                element.className = HST.CLASS.CONTAINER;
            }
            return element;
        }
        }
    ]);

})();
