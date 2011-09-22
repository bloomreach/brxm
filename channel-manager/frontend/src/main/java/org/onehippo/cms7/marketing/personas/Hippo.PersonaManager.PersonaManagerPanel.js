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
        var self = this;

        this.smallAvatarUrls = config.avatarUrls.small;

        this.personaStore = new Ext.data.JsonStore({
            fields: [
                { name: 'id', mapping: 'id' },
                { name: 'name', mapping: 'name' },
                { name: 'description', mapping: 'description' },
                { name: 'avatarName', mapping: 'avatarName' },
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
                    this.fireEvent('load', this);
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
                        if (console) console.warn('Could not parse server error: ' + syntaxError);
                    }
                }
                return String.format(this.errorInvalidResponse, action, response.status, response.statusText);
            }
        });

        this.personaGrid = new Ext.grid.GridPanel({
            autoExpandColumn: 'name',
            autoExpandMax: Number.MAX_VALUE,
            border: false,
            cm: new Ext.grid.ColumnModel({
                defaults: {
                    editable: false,
                    menuDisabled: true,
                    sortable: false,
                },
                columns: [
                    {
                        align: 'middle',
                        id: 'avatar',
                        dataIndex: 'avatarName',
                        width: config.smallAvatarWidth + 4,
                        renderer: function(value, p, record) {
                            var url = self.avatarUrls.small[value];
                            return url ? String.format('<img width="{0}" src="{1}"/>', self.smallAvatarWidth, url) : '';
                        },
                        sortable: false
                    },
                    {
                        css: 'vertical-align: middle;',
                        dataIndex: 'name',
                        header: 'Name',
                        id: 'name',
                    }
                ],
            }),
            cls: 'persona-grid',
            enableColumnHide: false,
            enableColumnMove: false,
            enableColumnResize: false,
            region: 'west',
            sm: new Ext.grid.RowSelectionModel({
                singleSelect: true,
            }),
            store: this.personaStore,
            stripeRows: true,
            viewConfig: {
                emptyText: config.resources['no-personas-available'],
                scrollOffset: 2,
            },
            width: config.personaListWidth,
        });

        this.personaDetails = new Hippo.PersonaManager.PersonaDetails({
            avatarUrls: config.avatarUrls.large,
            avatarWidth: config.largeAvatarWidth,
            border: false,
            region: 'center',
        });

        Ext.apply(config, {
            layout: 'border',
            items: [ this.personaGrid, this.personaDetails ]
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

        this.personaStore.on('load', this.init, this);
        this.personaGrid.getSelectionModel().on('rowselect', function(sm, rowIndex, record) {
            this.personaDetails.enable();
            this.personaDetails.setPersona(
                    record.get('name'),
                    record.get('description'),
                    record.get('avatarName')
            )
        }, this);
        this.personaStore.load();

        Hippo.PersonaManager.PersonaManagerPanel.superclass.initComponent.apply(this, arguments);
    },

    init: function() {
        if (this.personaStore.getTotalCount() <= 0) {
            // we cannot show anything useful, so hide the whole panel
            this.personaDetails.hide();
        } else {
            var index = this.personaStore.find('id', 'default');
            if (index >= 0) {
                this.personaGrid.getSelectionModel().selectRow(index);
            }
        }
    }

});
Ext.reg('Hippo.PersonaManager.PersonaManagerPanel', Hippo.PersonaManager.PersonaManagerPanel);


/**
 * @class Hippo.PersonaManager.PersonaDetails
 * @extends Ext.Panel
 */
Hippo.PersonaManager.PersonaDetails = Ext.extend(Ext.Panel, {

    constructor: function(config) {
        this.avatarUrls = config.avatarUrls;

        this.avatar = new Hippo.PersonaManager.Image({
            flex: 0,
            width: config.avatarWidth
        });

        this.name = new Ext.BoxComponent({
            anchor: '100%',
            border: false,
            cls: 'persona-name',
            fieldLabel: 'Name',
            layout: 'anchor',
        });

        this.description = new Ext.BoxComponent({
            anchor: '100% -3',
            border: false,
            boxMinWidth: 50,
            boxMaxWidth: 350,
            cls: 'persona-description',
            fieldLabel: 'Description',
            layout: 'anchor',
        });

        Ext.apply(config, {
            cls: 'persona-details',
            items: [
                this.avatar,
                {
                    border: false,
                    flex: 1,
                    items: [ this.name, this.description ],
                    labelWidth: 100,
                    xtype: 'form',
                }
            ],
            layout: 'hbox',
            layoutConfig: {
                defaultMargins: '10'
            },
            region: 'center'
        });
        Hippo.PersonaManager.PersonaDetails.superclass.constructor.call(this, config);
    },

    setPersona: function(name, description, avatarName) {
        this.avatar.setUrl(this.avatarUrls[avatarName]);
        this.name.update(name);
        this.description.update(description);
    }

});
Ext.reg('Hippo.PersonaManager.PersonaDetails', Hippo.PersonaManager.PersonaDetails);

/**
 * @class Hippo.PersonaManager.Image
 * @extends Ext.Component
 */
Hippo.PersonaManager.Image = Ext.extend(Ext.BoxComponent, {

    constructor: function(config) {
        Ext.apply(config, {
            autoEl: {
                tag: 'img',
                src: config.url || Ext.BLANK_IMAGE_URL
            },
        });
        Hippo.PersonaManager.Image.superclass.constructor.call(this, config);
    },

    onRender: function() {
        Hippo.PersonaManager.Image.superclass.onRender.apply(this, arguments);
        this.el.on('load', this.syncSize, this);
    },

    setUrl: function(url) {
        this.el.dom.src = url;
    }

});
Ext.reg('Hippo.PersonaManager.Image', Hippo.PersonaManager.Image);
