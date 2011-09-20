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

Ext.namespace('Hippo.PersonaManager');

/**
 * @class Hippo.PersonaManager.PersonaManagerPanel
 * @extends Ext.Panel
 */
Hippo.PersonaManager.PersonaManagerPanel = Ext.extend(Ext.Panel, {

    constructor: function(config) {
        this.store = new Ext.data.JsonStore({
            fields: [
                { name: 'id', mapping: 'id' },
                { name: 'name', mapping: 'name' },
                { name: 'description', mapping: 'description' },
            ],
            idProperty: 'id',
            errorActionFailed: config.resources['error-action-failed'],
            errorInvalidResponse: config.resources['error-invalid-response'],
            errorTitle: config.resources['error-title'],
            listeners: {
                exception: function(store, type, action, options, response, arg) {
                    var errorMsg = this.createErrorMessage(type, action, response);
                    Ext.MessageBox.alert(this.errorTitle, errorMsg);
                    this.removeAll();
                },
            },
            proxy: new Ext.data.HttpProxy({
                url: '/site/_rp/cafebabe-cafe-babe-cafe-babecafebabe./personas/'
            }),
            restful: true,
            root: 'data',

            createErrorMessage: function(type, action, response) {
                if (type === 'remote' || response.status === 500) {
                    // remote error, or probably an error reported by the server which we try to parse
                    try {
                        var jsonResponse = Ext.util.JSON.decode(response.responseText);
                        return String.format(this.errorActionFailed, action, jsonResponse.message);
                    } catch (syntaxError) {
                        console.log('Could not parse server error: ' + syntaxError);
                    }
                }
                return String.format(this.errorInvalidResponse, action, response.status, response.statusText);
            }
        });

        this.personaList = new Ext.grid.GridPanel({
            autoExpandColumn: 'name',
            autoExpandMax: Number.MAX_VALUE,
            cm: new Ext.grid.ColumnModel({
                defaults: {
                    editable: false,
                    menuDisabled: true,
                    sortable: false,
                },
                columns: [
                    {
                        header: 'Name',
                        id: 'name',
                        dataIndex: 'name',
                    }
                ],
            }),
            enableColumnHide: false,
            enableColumnMove: false,
            enableColumnResize: false,
            region: 'center',
            sm: new Ext.grid.RowSelectionModel({
                singleSelect: true,
            }),
            store: this.store,
            stripeRows: true,
            viewConfig: {
                emptyText: config.resources['no-personas-available']
            }
        });

        Ext.apply(config, {
            layout: 'border',
            items: [ this.personaList ]
        });

        Hippo.PersonaManager.PersonaManagerPanel.superclass.constructor.call(this, config);
    },

    initComponent: function() {
        var self = this;

        // recalculate the ExtJs layout when the YUI layout manager fires a resize event
        this.on('afterlayout', function() {
            var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
            YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function() {
                self.setSize(arguments[0].body.w, arguments[0].body.h);
                self.doLayout();
            }, true);
        }, this, {single: true});

        this.store.on('load', this.selectDefaultPersona, this);
        this.store.load();

        Hippo.PersonaManager.PersonaManagerPanel.superclass.initComponent.apply(this, arguments);
    },

    selectDefaultPersona: function() {
        var index = this.store.find('id', 'default');
        if (index >= 0) {
            var selModel = this.personaList.getSelectionModel();
            selModel.selectRow(index);
        }
    }

});

Ext.reg('Hippo.PersonaManager.PersonaManagerPanel', Hippo.PersonaManager.PersonaManagerPanel);
