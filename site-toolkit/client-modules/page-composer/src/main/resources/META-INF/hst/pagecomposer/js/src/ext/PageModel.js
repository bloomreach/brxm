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

Ext.namespace('Hippo.App.PageModel');

(function() {
    Hippo.App.PageModel.FactoryImpl = function() {
    };

    Hippo.App.PageModel.FactoryImpl.prototype = {
        createModel: function(element, cfg) {

            var id, name, path, type, xtype;
            if (typeof element !== 'undefined' && element !== null) {
                id = element.getAttribute('hst:id');
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

            return new Hippo.App.PageModel.Component(config);
        },

        createRecord : function(model) {
            return new Hippo.App.PageModel.ComponentRecord(model);
        }
    };

    Hippo.App.PageModel.Factory = new Hippo.App.PageModel.FactoryImpl();

    Hippo.App.PageModel.Component = function(cfg) {
        Ext.apply(this, cfg);
    };

    Hippo.App.PageModel.Component.prototype = {
    };

    //TODO: update this one for dropFromParent stuff
    Hippo.App.PageModel.ComponentRecord = Ext.data.Record.create([
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

    Hippo.App.PageModel.ReadRecord = Ext.data.Record.create([
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
            var element = Hippo.App.Main.findElement(record.id);
            if (element == null) {
                element = document.createElement('div');
                element.id = record.id;
                element.setAttribute('hst:id', record.id);
                element.setAttribute('hst:name', record.name);
                element.setAttribute('hst:type', record.type);
                element.setAttribute('hst:xtype', record.xtype);
                element.setAttribute('hst:temporary', true);
                element.className = 'componentContentWrapper';
            }
            return element;
        }
        }
    ]);

})();
