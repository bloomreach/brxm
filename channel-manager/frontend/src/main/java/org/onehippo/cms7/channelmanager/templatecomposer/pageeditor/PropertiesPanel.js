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

Hippo.ChannelManager.TemplateComposer.PropertiesPanel = Ext.extend(Ext.ux.tot2ivn.VrTabPanel, {

    composerRestMountUrl : null,
    mountId : null,
    resources : null,
    variants : null,
    variantsUuid : null,
    locale : null,
    componentId : null,
    silent : true,
    allVariantsStore: null,

    constructor : function (config) {
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.variantsUuid = config.variantsUuid;
        this.mountId = config.mountId;
        this.resources = config.resources;
        this.locale = config.locale;

        this.allVariantsStore = new Hippo.ChannelManager.TemplateComposer.VariantsStore({
            composerRestMountUrl: this.composerRestMountUrl,
            variantsUuid: this.variantsUuid
        });

        config = Ext.apply(config, { activeTab : 0 });
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.constructor.call(this, config);
    },

    initComponent : function () {
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.initComponent.apply(this, arguments);

        this.on('tabchange', function (panel, tab) {
            if (!this.silent && tab) {
                this.fireEvent('variantChange', tab.componentId, tab.variant ? tab.variant.id : undefined);
            }
        }, this);
    },

    setComponentId : function (componentId) {
        this.componentId = componentId;
    },

    load : function (variant) {
        this.silent = true;
        this.beginUpdate();
        this.removeAll();

        this._loadAllVariantsStore().when(function() {
            this._loadVariantStore().when(function () {
                this._initTabs();
                this._selectTabByVariant(variant);

                var tab = this.getActiveTab();
                this.fireEvent('variantChange', tab.componentId, tab.variant.id);
                this.silent = false;

                var futures = [];
                this.items.each(function (item) {
                    futures.push(item.load());
                }, this);

                Hippo.Future.join(futures).when(function () {
                    this.endUpdate();
                }.createDelegate(this)).otherwise(function () {
                    this.endUpdate();
                }.createDelegate(this));
            }.createDelegate(this)).otherwise(function () {
                this.endUpdate();
                this.silent = false;
            }.createDelegate(this));
        }.createDelegate(this)).otherwise(function() {
            this.endUpdate();
            this.silent = false;
        }.createDelegate(this));
    },

    _loadAllVariantsStore: function() {
        return new Hippo.Future(function (success, fail) {
            this.allVariantsStore.on('load', success, {single : true});
            this.allVariantsStore.on('exception', fail, {single : true});
            this.allVariantsStore.load();
        }.createDelegate(this));
    },

    _loadVariantStore : function () {
        return new Hippo.Future(function (success, fail) {
            if (this.componentId) {
                Ext.Ajax.request({
                    url : this.composerRestMountUrl + '/' + this.componentId + './variants/?FORCE_CLIENT_HOST=true',
                    success : function (result) {
                        var jsonData = Ext.util.JSON.decode(result.responseText);
                        this._loadVariants(jsonData.data);
                        success();
                    },
                    failure : function (result) {
                        this._loadException(result.response);
                        fail();
                    },
                    scope : this
                });
            } else {
                this.variants = [{id: 'default', name: 'Default'}];
                success();
            }
        }.createDelegate(this));
    },

    _loadVariants : function (records) {
        this.variants = [];
        for (var i = 0; i < records.length; i++) {
            var id = records[i];
            var record = this.allVariantsStore.getById(id);
            var name = record ? record.get('name') : id;
            this.variants.push({ id: id, name: name});
        }
        this.variants.push({id: 'plus', name: '+'});
    },

    _loadException : function (response) {
        Hippo.Msg.alert('Failed to get variants.', 'Only default variant will be available: ' + response.status + ':' + response.statusText);
        this.variants = [{id: 'default', name: 'Default'}];
    },

    _initTabs : function () {
        for (var i = 0; i < this.variants.length; i++) {
            var form;
            if ('plus' == this.variants[i].id) {
                form = new Hippo.ChannelManager.TemplateComposer.PlusForm({
                    mountId : this.mountId,
                    composerRestMountUrl : this.composerRestMountUrl,
                    resources : this.resources,
                    locale : this.locale,
                    componentId : this.componentId,
                    variants : this.variants,
                    title: this.variants[i].name,
                    variantsUuid : this.variantsUuid,
                    listeners : {
                        'save' : function (tab, variant) {
                            this.load(variant);
                        },
                        scope : this
                    }
                });
            } else {
                form = new Hippo.ChannelManager.TemplateComposer.PropertiesForm({
                    variant : this.variants[i],
                    title: this.variants[i].name,
                    mountId : this.mountId,
                    composerRestMountUrl : this.composerRestMountUrl,
                    resources : this.resources,
                    locale : this.locale,
                    componentId : this.componentId,
                    listeners : {
                        'delete' : function (tab, variant) {
                            this.load();
                        },
                        scope : this
                    }
                });
            }
            this.relayEvents(form, ['cancel']);
            this.add(form);
        }
    },

    _selectTabByVariant : function (variant) {
        for (var i = 0; i < this.variants.length; i++) {
            if (this.variants[i].id == variant) {
                this.setActiveTab(i);
                return;
            }
        }
        this.setActiveTab(0);
    }

});

Hippo.ChannelManager.TemplateComposer.PlusForm = Ext.extend(Ext.FormPanel, {
    mountId : null,
    composerRestMountUrl : null,
    resources : null,
    componentId : null,
    locale : null,

    constructor : function (config) {
        this.title = '<span style="font-size: 140%;">' + config.title + '</span>';
        this.mountId = config.mountId;
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.resources = config.resources;
        this.locale = config.locale;
        this.componentId = config.componentId;
        this.variantsUuid = config.variantsUuid;

        var variantIds = [];
        for (var i = 0; i < config.variants.length; i++) {
            variantIds.push(config.variants[i].id);
        };

        this.variantsStore = new Hippo.ChannelManager.TemplateComposer.VariantsStore({
            composerRestMountUrl: this.composerRestMountUrl,
            skipIds: variantIds,
            variantsUuid: this.variantsUuid
        });

        Hippo.ChannelManager.TemplateComposer.PlusForm.superclass.constructor.call(this, config);
    },

    initComponent : function () {
        var combobox = new Ext.form.ComboBox({
            store : this.variantsStore,
            valueField : 'id',
            displayField : 'name',
            triggerAction : 'all'
        });

        Ext.apply(this, {
            autoHeight : true,
            border : false,
            padding : 10,
            autoScroll : true,
            labelWidth : 100,
            labelSeparator : '',
            defaults : {
                anchor : '100%'
            },

            items : [ combobox ],

            buttons : [
                {
                    text : this.resources['properties-panel-button-add-variant'],
                    handler : function () {
                        var variant = combobox.getValue();
                        Ext.Ajax.request({
                            method : 'POST',
                            url : this.composerRestMountUrl + '/' + this.componentId + './variant/' + variant + '?FORCE_CLIENT_HOST=true',
                            success : function () {
                                this.fireEvent('save', this, variant);
                            },
                            scope : this
                        });
                    },
                    scope : this
                }
            ]
        });
        Hippo.ChannelManager.TemplateComposer.PlusForm.superclass.initComponent.apply(this, arguments);

        this.addEvents('save');
    },

    load : function () {
        return new Hippo.Future(function (success, fail) {
            this.variantsStore.on('load', success, {single : true});
            this.variantsStore.on('exception', fail, {single : true});
            this.variantsStore.load();
        }.createDelegate(this));
    }
});

Hippo.ChannelManager.TemplateComposer.VariantsStore = Ext.extend(Hippo.ChannelManager.TemplateComposer.RestStore, {

    constructor : function(config) {
        var self = this;

        this.skipIds = config.skipIds || [];

        var proxy = new Ext.data.HttpProxy({
            api: {
                read: config.composerRestMountUrl + '/' + config.variantsUuid + './variants/?FORCE_CLIENT_HOST=true',
                create: '#',
                update: '#',
                destroy: '#'
            }
        });

        Ext.apply(config, {
            id: 'VariantsStore',
            proxy: proxy,
            prototypeRecord :  [
                {name: 'id' },
                {name: 'name' },
                {name: 'description' }
            ],
            sortInfo: {
                field: 'name',
                direction: 'ASC'
            },
            listeners: {
                load: function(store, records, options) {
                    for (var i = 0; i < records.length; i++) {
                        if (self.skipIds.indexOf(records[i].get('id')) >= 0) {
                            store.remove(records[i]);
                        }
                    }
                }
            }
        });

        Hippo.ChannelManager.TemplateComposer.VariantsStore.superclass.constructor.call(this, config);
    }

});

Hippo.ChannelManager.TemplateComposer.PropertiesForm = Ext.extend(Ext.FormPanel, {
    mountId : null,
    variant : null,
    composerRestMountUrl : null,
    resources : null,
    componentId : null,
    locale : null,

    constructor : function (config) {
        this.variant = config.variant;
        this.title = config.variant;
        this.mountId = config.mountId;
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.resources = config.resources;
        this.locale = config.locale;
        this.componentId = config.componentId;

        Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.constructor.call(this, config);
    },

    initComponent : function () {
        var buttons = [];
        if (this.variant.id != 'default') {
            buttons.push({
                text : 'delete',
                handler : function () {
                    Ext.Ajax.request({
                        method : 'POST',
                        url : this.composerRestMountUrl + '/' + this.componentId + './variant/delete/' + this.variant.id + '?FORCE_CLIENT_HOST=true',
                        success : function () {
                            this.fireEvent('delete', this, this.variant.id);
                        },
                        scope : this
                    });
                },
                scope : this
            });
            buttons.push('->');
        }
        this.saveButton = new Ext.Button({
            text : this.resources['properties-panel-button-save'],
            handler : this._submitForm,
            scope : this
        });
        buttons.push(this.saveButton);
        buttons.push({
            text : this.resources['properties-panel-button-cancel'],
            scope : this,
            handler : function () {
                this.fireEvent('cancel');
            }
        });

        Ext.apply(this, {
            autoHeight : true,
            border : false,
            padding : 10,
            autoScroll : true,
            labelWidth : 100,
            labelSeparator : '',
            defaults : {
                anchor : '100%'
            },

            buttons : buttons
        });

        Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.initComponent.apply(this, arguments);

        this.addEvents('save', 'cancel', 'delete');
    },

    _submitForm : function () {
        this.fireEvent('save');
        // don't send the override checkbox fields
        this.items.each(function (item) {
            if (item.name === 'override') {
                item.setDisabled(true);
            }
        });
        this.getForm().submit({
            headers : {
                'FORCE_CLIENT_HOST' : 'true'
            },
            url : this.composerRestMountUrl + '/' + this.componentId + './parameters/' + this.variant.id + '?FORCE_CLIENT_HOST=true',
            method : 'POST',
            success : function () {
                Hippo.ChannelManager.TemplateComposer.Instance.selectVariant(this.componentId, this.variant.id);
                Ext.getCmp('componentPropertiesPanel').load(this.variant.id);
            }.bind(this)
        });
    },

    _createDocument : function (ev, target, options) {

        var self = this;
        var createUrl = this.composerRestMountUrl + '/' + this.mountId + './create?FORCE_CLIENT_HOST=true';
        var createDocumentWindow = new Ext.Window({
            title : this.resources['create-new-document-window-title'],
            height : 150,
            width : 400,
            modal : true,
            items : [
                {
                    xtype : 'form',
                    height : 150,
                    padding : 10,
                    labelWidth : 120,
                    id : 'createDocumentForm',
                    defaults : {
                        labelSeparator : '',
                        anchor : '100%'
                    },
                    items : [
                        {
                            xtype : 'textfield',
                            fieldLabel : this.resources['create-new-document-field-name'],
                            allowBlank : false
                        },
                        {
                            xtype : 'textfield',
                            disabled : true,
                            fieldLabel : this.resources['create-new-document-field-location'],
                            value : options.docLocation
                        }
                    ]
                }
            ],
            layout : 'fit',
            buttons : [
                {
                    text : this.resources['create-new-document-button'],
                    handler : function () {
                        var createDocForm = Ext.getCmp('createDocumentForm').getForm();
                        createDocForm.submit();
                        options.docName = createDocForm.items.get(0).getValue();

                        if (options.docName == '') {
                            return;
                        }
                        createDocumentWindow.hide();

                        Ext.Ajax.request({
                            url : createUrl,
                            params : options,
                            success : function () {
                                Ext.getCmp(options.comboId).setValue(options.docLocation + "/" + options.docName);
                            },
                            failure : function () {
                                Hippo.Msg.alert(self.resources['create-new-document-message'], self.resources['create-new-document-failed'], function () {
                                    Hippo.ChannelManager.TemplateComposer.Instance.initComposer();
                                });
                            }
                        });

                    }
                }
            ]
        });
        createDocumentWindow.addButton({text : this.resources['create-new-document-button-cancel']}, function () {
            this.hide();
        }, createDocumentWindow);

        createDocumentWindow.show();
    },

    _loadProperties : function (store, records, options) {
        var length = records.length;
        if (length == 0) {
            this.add({
                html : "<div style='padding:5px' align='center'>" + this.resources['properties-panel-no-properties'] + "</div>",
                xtype : "panel",
                autoWidth : true,
                layout : 'fit'
            });
            this.saveButton.hide();
        } else {
            for (var i = 0; i < length; ++i) {
                var property = records[i];
                var value = property.get('value');
                var defaultValue = property.get('defaultValue');
                var isDefaultValue = false;
                if (!value || value.length === 0) {
                    value = defaultValue;
                    isDefaultValue = true;
                }
                var propertyField;
                if (property.get('type') == 'combo') {
                    var comboStore = new Ext.data.JsonStore({
                        root : 'data',
                        url : this.composerRestMountUrl + '/' + this.mountId + './documents/' + property.get('docType') + '?FORCE_CLIENT_HOST=true',
                        fields : ['path']
                    });

                    propertyField = this.add({
                        fieldLabel : property.get('label'),
                        xtype : 'combo',
                        allowBlank : !property.get('required'),
                        name : property.get('name'),
                        value : value,
                        defaultValue : defaultValue,
                        store : comboStore,
                        forceSelection : true,
                        triggerAction : 'all',
                        displayField : 'path',
                        valueField : 'path'
                    });

                    if (property.get('allowCreation')) {
                        this.add({
                            bodyCfg : {
                                tag : 'div',
                                cls : 'create-document-link',
                                html : this.resources['create-document-link-text'].format('<a href="#" id="combo' + i + '">&nbsp;', '&nbsp;</a>&nbsp;')
                            },
                            border : false

                        });

//                        this.doLayout(false, true); //Layout the form otherwise we can't use the link in the panel.
                        Ext.get("combo" + i).on("click", this._createDocument, this, {
                            docType : property.get('docType'),
                            docLocation : property.get('docLocation'),
                            comboId : propertyField.id
                        });
                    }

                } else {
                    var propertyFieldConfig = {
                        fieldLabel : property.get('label'),
                        xtype : property.get('type'),
                        value : value,
                        defaultValue : defaultValue,
                        allowBlank : !property.get('required'),
                        name : property.get('name'),
                        listeners : {
                            change : function () {
                                var value = this.getValue();
                                if (!value || value.length === 0 || value === this.defaultValue) {
                                    this.addClass('default-value');
                                    this.setValue(this.defaultValue);
                                } else {
                                    this.removeClass('default-value');
                                }
                            }
                        }
                    };
                    if (property.get('type') === 'checkbox') {
                        propertyFieldConfig.checked = (value === true || value === 'true' || value == '1' || String(value).toLowerCase() == 'on');
                    }
                    propertyField = this.add(propertyFieldConfig);
                    if (isDefaultValue) {
                        propertyField.addClass('default-value');
                    }
                }
            }
            this.saveButton.show();
        }
//        this.doLayout(false, true);
    },

    _loadException : function (proxy, type, actions, options, response) {
        var errorText = this.resources['properties-panel-load-exception-text'].format(actions);
        if (type == 'response') {
            errorText += '\n' + this.resources['properties-panel-load-exception-response'].format(response.statusText, response.status, options.url);
        }

        this.add({
            xtype : 'label',
            text : errorText,
            fieldLabel : this.resources['properties-panel-error-field-label']
        });

//        this.doLayout(false, true);
    },

    load : function () {
        return new Hippo.Future(function (success, fail) {
            var componentPropertiesStore = new Ext.data.JsonStore({
                autoLoad : false,
                method : 'GET',
                root : 'properties',
                fields : ['name', 'value', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation', 'defaultValue' ],
                url : this.composerRestMountUrl + '/' + this.componentId + './parameters/' + this.locale + '/' + this.variant.id + '?FORCE_CLIENT_HOST=true'
            });

            componentPropertiesStore.on('load', function () {
                this._loadProperties.apply(this, arguments);
                success();
            }, this);
            componentPropertiesStore.on('exception', function () {
                this._loadException.apply(this, arguments);
                fail();
            }, this);
            componentPropertiesStore.load();
        }.createDelegate(this));
    }

});

//FIXME: don't override Ext provided code; create subclass or patch instance
//Add * to the required fields
Ext.apply(Ext.layout.FormLayout.prototype, {
    originalRenderItem : Ext.layout.FormLayout.prototype.renderItem,
    renderItem : function (c, position, target) {
        if (c && !c.rendered && c.isFormField && c.fieldLabel && c.allowBlank === false) {
            c.fieldLabel = c.fieldLabel + " <span class=\"req\">*</span>";
        }
        this.originalRenderItem.apply(this, arguments);
    }
});
