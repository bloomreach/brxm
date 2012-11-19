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
    globalVariantsStore: null,
    globalVariantsStoreFuture : null,
    variantAdderXType: null,
    propertiesEditorXType: null,

    constructor : function (config) {
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.variantsUuid = config.variantsUuid;
        this.mountId = config.mountId;
        this.resources = config.resources;
        this.locale = config.locale;

        this.globalVariantsStore = config.globalVariantsStore;
        this.globalVariantsStoreFuture = config.globalVariantsStoreFuture;

        this.variantAdderXType = config.variantAdderXType;
        this.propertiesEditorXType = config.propertiesEditorXType;

        // also store the resources in a global variable, since pluggable subclasses need to access them too
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources = config.resources;

        config = Ext.apply(config, { activeTab : 0 });
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.constructor.call(this, config);
    },

    initComponent : function () {
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.initComponent.apply(this, arguments);

        this.on('beforetabchange', function(panel, newTab) {
            if (!Ext.isDefined(newTab)) {
                return true;
            } else {
                return newTab.fireEvent('beforeactivate', newTab, panel);
            }
        }, this);

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

        var loadVariantTabs = function() {
            var futures, tab;
            futures = [];
            this._initTabs();
            this.adjustBodyWidth(this.tabWidth);
            this._selectTabByVariant(variant);

            tab = this.getActiveTab();
            this.fireEvent('variantChange', tab.componentId, tab.variant.id);
            this.silent = false;

            futures = [];
            this.items.each(function (item) {
                futures.push(item.load());
            }, this);

            Hippo.Future.join(futures).when(function () {
                this.endUpdate();
            }.createDelegate(this)).otherwise(function () {
                this.endUpdate();
            }.createDelegate(this));
        }.createDelegate(this);

        if (typeof(this.variantsUuid) === 'undefined' || this.variantsUuid === null) {
            this.variants = [{id: 'hippo-default', name: this.resources['properties-panel-variant-default']}];
            this._hideTabs();
            loadVariantTabs();
        } else {
            this._showTabs();
            this.globalVariantsStoreFuture.when(function() {
                this._loadVariants().when(function() {
                    this._showTabs();
                    loadVariantTabs();
                }.createDelegate(this)).otherwise(function() {
                    this.endUpdate();
                    this.silent = false;
                }.createDelegate(this));
            }.createDelegate(this)).otherwise(function() {
                this.endUpdate();
                this.silent = false;
            }.createDelegate(this));
        }
    },

    _loadVariants: function () {
        return new Hippo.Future(function (success, fail) {
            if (this.componentId) {
                Ext.Ajax.request({
                    url : this.composerRestMountUrl + '/' + this.componentId + './?FORCE_CLIENT_HOST=true',
                    success : function (result) {
                        var jsonData = Ext.util.JSON.decode(result.responseText);
                        this._loadComponentVariants(jsonData.data).when(function() {
                            success();
                        }.createDelegate(this)).otherwise(function() {
                            fail();
                        }.createDelegate(this));
                    },
                    failure : function (result) {
                        this._loadException(result.response);
                        fail();
                    },
                    scope : this
                });
            } else {
                this.variants = [{id: 'hippo-default', name: this.resources['properties-panel-variant-default']}];
                success();
            }
        }.createDelegate(this));
    },

    _loadComponentVariants: function(variants) {
        return new Hippo.Future(function (success, fail) {
            Ext.Ajax.request({
                url : this.composerRestMountUrl + '/' + this.variantsUuid + './componentvariants?locale=' + this.locale + '&FORCE_CLIENT_HOST=true',
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                params: Ext.util.JSON.encode(variants),
                success : function (result) {
                    var jsonData = Ext.util.JSON.decode(result.responseText);
                    this._initVariants(jsonData.data);
                    success();
                },
                failure : function (result) {
                    this._loadException(result.response);
                    fail();
                },
                scope : this
            });
        }.createDelegate(this));
    },

    _initVariants: function(records) {
        this.variants = [];
        Ext.each(records, function(record) {
            this.variants.push(record);
        }, this);
        this.variants.push({id: 'plus', name: this.resources['properties-panel-variant-plus']});
    },

    _loadException : function (response) {
        Hippo.Msg.alert('Failed to get variants.', 'Only default variant will be available: ' + response.status + ':' + response.statusText);
        this.variants = [{id: 'hippo-default', name: this.resources['properties-panel-variant-default']}];
    },

    _initTabs : function () {
        var propertiesEditorCount, i, tabComponent, propertiesForm;
        propertiesEditorCount = this.variants.length - 1;
        for (i = 0; i < this.variants.length; i++) {
            if ('plus' == this.variants[i].id) {
                tabComponent = this._createVariantAdder(this.variants[i]);
            } else {
                propertiesForm = new Hippo.ChannelManager.TemplateComposer.PropertiesForm({
                    variant: this.variants[i],
                    mountId: this.mountId,
                    composerRestMountUrl: this.composerRestMountUrl,
                    locale: this.locale,
                    componentId: this.componentId,
                    bubbleEvents: ['cancel'],
                    margins: {
                        top: 0,
                        right: 10,
                        bottom: 0,
                        left: 0
                    },
                    listeners: {
                        'save': function() {
                            this._cleanupVariants();
                        },
                        'delete': function() {
                            this._cleanupVariants();
                            this.load();
                        },
                        scope: this
                    }
                });
                tabComponent = this._createPropertiesEditor(this.variants[i], propertiesEditorCount, propertiesForm);
            }
            this.relayEvents(tabComponent, ['cancel']);
            this.add(tabComponent);
        }
    },

    _createVariantAdder: function(variant) {
        return Hippo.ExtWidgets.create('Hippo.ChannelManager.TemplateComposer.VariantAdder', {
            composerRestMountUrl : this.composerRestMountUrl,
            componentId : this.componentId,
            locale: this.locale,
            skipVariantIds : Ext.pluck(this.variants, 'id'),
            title: variant.name,
            variantsUuid : this.variantsUuid,
            listeners : {
                'save' : function(tab, variant) {
                    this._cleanupVariants();
                    this.load(variant);
                },
                'copy': this._copyVariant,
                scope : this
            }
        });
    },

    _createPropertiesEditor: function(variant, variantCount, propertiesForm) {
        return Hippo.ExtWidgets.create('Hippo.ChannelManager.TemplateComposer.PropertiesEditor', {
            cls: 'component-properties-editor',
            componentId : this.componentId,
            variant: variant,
            variantCount: variantCount,
            title: variant.name,
            propertiesForm: propertiesForm
        });
    },

    _hideTabs : function() {
        this.tabWidth = 0;
    },

    _showTabs : function() {
        this.tabWidth = 130;
    },

    _selectTabByVariant : function (variant) {
        var i;
        for (i = 0; i < this.variants.length; i++) {
            if (this.variants[i].id == variant) {
                this.setActiveTab(i);
                return;
            }
        }
        this.setActiveTab(0);
    },

    _copyVariant: function(existingVariant, newVariant) {
        var existingTab, newPropertiesForm, newTab, newTabIndex;

        existingTab = this._getTab(existingVariant);
        if (Ext.isDefined(existingTab) && existingTab instanceof Hippo.ChannelManager.TemplateComposer.PropertiesEditor) {
            newPropertiesForm = existingTab.propertiesForm.copy(newVariant);
            newTab = this._createPropertiesEditor(newVariant, this.items.length, newPropertiesForm);
            newTabIndex = this.items.length - 1;
            this.insert(newTabIndex, newTab);
            this.setActiveTab(newTabIndex);
            this.syncSize();
        } else {
            console.log("Cannot find tab for variant '" + existingVariant + "', copy to '" + newVariant + "' failed");
        }
    },

    _getTab: function(variantId) {
        var tab;

        this.items.each(function(item) {
            if (Ext.isDefined(item.variant) && item.variant.id === variantId) {
                tab = item;
            }
        });
        return tab;
    },

    _cleanupVariants: function() {
        if (this.variants) {
            var variantIds = [];
            Ext.each(this.variants, function(variant) {
                if (variant.id !== 'hippo-default' && variant.id !== 'plus') {
                    variantIds.push(variant.id);
                }
            });
            Ext.Ajax.request({
                method : 'POST',
                url : this.composerRestMountUrl + '/' + this.componentId + './' + '?FORCE_CLIENT_HOST=true',
                headers: {
                    'Content-Type': 'application/json'
                },
                params: Ext.util.JSON.encode(variantIds),
                scope : this
            });
        }
    }

});

Hippo.ChannelManager.TemplateComposer.PropertiesForm = Ext.extend(Ext.FormPanel, {

    mountId : null,
    variant : null,
    newVariantId : null,
    composerRestMountUrl : null,
    componentId : null,
    locale : null,

    constructor : function (config) {
        this.variant = config.variant;
        this.newVariantId = this.variant.id;
        this.mountId = config.mountId;
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.locale = config.locale;
        this.componentId = config.componentId;

        Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.constructor.call(this, Ext.apply(config, {
            cls: 'templateComposerPropertiesForm'
        }));
    },

    copy: function(newVariant) {
        var copy = new Hippo.ChannelManager.TemplateComposer.PropertiesForm(this.initialConfig);
        copy.variant = newVariant;
        copy._loadProperties(this.records);
        return copy;
    },

    initComponent : function () {
        var buttons = [];
        if (this.variant.id != 'hippo-default') {
            buttons.push({
                text : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-delete'],
                handler : function () {
                    Ext.Ajax.request({
                        method : 'DELETE',
                        url : this.composerRestMountUrl + '/' + this.componentId + './' +
                                encodeURIComponent(this.variant.id) + '?FORCE_CLIENT_HOST=true',
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
            text : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-save'],
            handler : this._submitForm,
            scope : this,
            formBind: true
        });
        buttons.push(this.saveButton);
        buttons.push({
            text : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-cancel'],
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
            monitorValid: true,
            defaults : {
                anchor : '100%'
            },
            plugins: Hippo.ChannelManager.MarkRequiredFields,
            buttons : buttons
        });

        Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.initComponent.apply(this, arguments);

        this.addEvents('save', 'cancel', 'delete');
    },

    setNewVariant: function(newVariantId) {
        this.newVariantId = newVariantId;
    },

    _submitForm : function () {
        var uncheckedValues = {};
        this.fireEvent('save');

        this.getForm().items.each(function(item) {
            if (item instanceof Ext.form.Checkbox) {
                if (!item.checked) {
                    uncheckedValues[item.name] = 'off';
                }
            }
        });

        this.getForm().submit({
            headers : {
                'FORCE_CLIENT_HOST' : 'true'
            },
            params: uncheckedValues,
            url : this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/rename/' + encodeURIComponent(this.newVariantId) + '?FORCE_CLIENT_HOST=true',
            method : 'POST',
            success : function () {
                Hippo.ChannelManager.TemplateComposer.Instance.selectVariant(this.componentId, this.variant.id);
                Ext.getCmp('componentPropertiesPanel').load(this.newVariantId);
            }.bind(this)
        });
    },

    _createDocument : function (ev, target, options) {
        var createUrl, createDocumentWindow;

        createUrl = this.composerRestMountUrl + '/' + this.mountId + './create?FORCE_CLIENT_HOST=true';
        createDocumentWindow = new Ext.Window({
            title : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-window-title'],
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
                            fieldLabel : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-field-name'],
                            allowBlank : false
                        },
                        {
                            xtype : 'textfield',
                            disabled : true,
                            fieldLabel : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-field-location'],
                            value : options.docLocation
                        }
                    ]
                }
            ],
            layout : 'fit',
            buttons : [
                {
                    text : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-button'],
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
                                Hippo.Msg.alert(Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-message'],
                                        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-failed'],
                                        function() {
                                            Hippo.ChannelManager.TemplateComposer.Instance.initComposer();
                                        }
                                );
                            }
                        });

                    }
                }
            ]
        });
        createDocumentWindow.addButton(
                {
                    text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-button-cancel']
                },
                function() {
                    this.hide();
                },
                createDocumentWindow
        );
        createDocumentWindow.show();
    },

    _loadProperties : function(records) {
        this.records = records;
        var length = records.length, i, record, value, defaultValue;
        if (length == 0) {
            this.add({
                html : "<div style='padding:5px' align='center'>" + Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-no-properties'] + "</div>",
                xtype : "panel",
                autoWidth : true,
                layout : 'fit'
            });
            this.saveButton.hide();
        } else {
            for (i = 0; i < length; i++) {
                record = records[i];
                if (record.get('hiddenInChannelManager')) {
                    continue;
                }
                value = record.get('value');
                defaultValue = record.get('defaultValue');
                if (!value || value.length === 0) {
                    value = defaultValue;
                }

                if (record.get('type') === 'documentcombobox') {
                    this.addDocumentComboBox(record, defaultValue, value);
                } else if (record.get('type') === 'combo') {
                    this.addComboBox(record, defaultValue, value);
                } else {
                    this.addComponent(record, defaultValue, value);
                }
            }
            this.saveButton.show();
        }
    },

    addDocumentComboBox : function(record, defaultValue, value) {
        var propertyField, comboStore = new Ext.data.JsonStore({
            root : 'data',
            url : this.composerRestMountUrl + '/' + this.mountId + './documents/' + record.get('docType') + '?FORCE_CLIENT_HOST=true',
            fields : ['path']
        });

        propertyField = this.add({
            fieldLabel : record.get('label'),
            xtype : 'combo',
            allowBlank : !record.get('required'),
            name : record.get('name'),
            value : value,
            defaultValue : defaultValue,
            store : comboStore,
            forceSelection : true,
            triggerAction : 'all',
            displayField : 'path',
            valueField : 'path'
        });

        if (record.get('allowCreation')) {
            this.add({
                bodyCfg : {
                    tag : 'div',
                    cls : 'create-document-link',
                    html : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-document-link-text'].format('<a href="#" id="combo' + i + '">&nbsp;', '&nbsp;</a>&nbsp;')
                },
                border : false

            });

            Ext.get("combo" + i).on("click", this._createDocument, this, {
                docType : record.get('docType'),
                docLocation : record.get('docLocation'),
                comboId : propertyField.id
            });
        }
    },

    addComboBox : function(record, defaultValue, value) {
        var comboBoxValues, comboBoxDisplayValues, dataIndex, comboBoxValuesLength, data = [];
        comboBoxValues = record.get(
                'dropDownListValues'
        );
        comboBoxDisplayValues = record.get(
                'dropDownListDisplayValues'
        );

        for (dataIndex=0, comboBoxValuesLength=comboBoxValues.length; dataIndex<comboBoxValuesLength; dataIndex++) {
            data.push([comboBoxValues[dataIndex], comboBoxDisplayValues[dataIndex]]);
        }

        this.add({
            xtype: 'combo',
            fieldLabel : record.get('label'),
            store : new Ext.data.ArrayStore({
                fields: [
                    'id',
                    'displayText'
                ],
                data: data
            }),
            value : value,
            hiddenName : record.get('name'),
            typeAhead: true,
            mode: 'local',
            triggerAction: 'all',
            selectOnFocus:true,
            valueField : 'id',
            displayField : 'displayText',
            listeners : {
                afterrender : function() {
                    // workaround, the padding-left which gets set on the element, let the right box side disappear,
                    // removing the style attribute after render fixes the layout
                    var formElement = this.el.findParent('.x-form-element');
                    formElement.removeAttribute('style');
                }
            }
        });
    },

    addComponent : function(record, defaultValue, value) {
        var propertyField, xtype = record.get('type'),
        propertyFieldConfig = {
            fieldLabel : record.get('label'),
            xtype : xtype,
            value : value,
            defaultValue : defaultValue,
            allowBlank : !record.get('required'),
            name : record.get('name'),
            listeners : {
                change : function () {
                    var value = this.getValue();
                    if (!value || value.length === 0 || value === this.defaultValue) {
                        this.addClass('default-value');
                        this.setValue(this.defaultValue);
                    } else {
                        this.removeClass('default-value');
                    }
                },
                afterrender : function() {
                    // workaround, the padding-left which gets set on the element, let the right box side disappear,
                    // removing the style attribute after render fixes the layout
                    var formElement = this.el.findParent('.x-form-element');
                    formElement.removeAttribute('style');
                }
            }
        };

        if (xtype === 'checkbox') {
            propertyFieldConfig.checked = (value === true || value === 'true' || value == '1' || String(value).toLowerCase() == 'on');
        } else if (xtype === 'linkpicker') {
            propertyFieldConfig.renderStripValue = /^\/?(?:[^\/]+\/)*/g;
            propertyFieldConfig.pickerConfig = {
                configuration: record.get('pickerConfiguration'),
                remembersLastVisited: record.get('pickerRemembersLastVisited'),
                initialPath: record.get('pickerInitialPath'),
                isRelativePath: record.get('pickerPathIsRelative'),
                rootPath: record.get('pickerRootPath'),
                selectableNodeTypes: record.get('pickerSelectableNodeTypes')
            }
        }
        propertyField = this.add(propertyFieldConfig);
        if (value === defaultValue) {
            propertyField.addClass('default-value');
        }
    },

    _loadException : function (proxy, type, actions, options, response) {
        var errorText = Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-load-exception-text'].format(actions);
        if (type == 'response') {
            errorText += '\n' + Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-load-exception-response'].format(response.statusText, response.status, options.url);
        }

        this.add({
            xtype : 'label',
            text : errorText,
            fieldLabel : Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-error-field-label']
        });
    },

    load : function () {
        return new Hippo.Future(function (success, fail) {
            var componentPropertiesStore = new Ext.data.JsonStore({
                autoLoad : false,
                method : 'GET',
                root : 'properties',
                fields : ['name', 'value', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation', 'defaultValue',
                    'pickerConfiguration', 'pickerInitialPath', 'pickerRemembersLastVisited', 'pickerPathIsRelative', 'pickerRootPath', 'pickerSelectableNodeTypes',
                    'dropDownListValues', 'dropDownListDisplayValues', 'hiddenInChannelManager' ],
                url : this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/' + this.locale + '?FORCE_CLIENT_HOST=true'
            });

            componentPropertiesStore.on('load', function(store, records) {
                this._loadProperties(records);
                success();
            }, this);
            componentPropertiesStore.on('exception', function () {
                this._loadException.apply(this, arguments);
                fail();
            }, this);
            componentPropertiesStore.load();
        }.createDelegate(this));
    },

    disableSave: function() {
        this.saveButton.disable();
    },

    enableSave: function() {
        this.saveButton.enable();
    }

});
Ext.reg('Hippo.ChannelManager.TemplateComposer.PropertiesForm', Hippo.ChannelManager.TemplateComposer.PropertiesForm);
