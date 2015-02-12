/*
 *  Copyright 2010-2015 Hippo B.V. (http://www.onehippo.com)
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
(function() {

    "use strict";

    Ext.namespace('Hippo.ChannelManager.TemplateComposer');

    Hippo.ChannelManager.TemplateComposer.PropertiesForm = Ext.extend(Ext.FormPanel, {

        mountId: null,
        variant: null,
        newVariantId: null,
        composerRestMountUrl: null,
        componentId: null,
        locale: null,

        PADDING: 10,

        constructor: function(config) {
            this.variant = config.variant;
            this.newVariantId = this.variant.id;
            this.mountId = config.mountId;
            this.composerRestMountUrl = config.composerRestMountUrl;
            this.locale = config.locale;
            this.componentId = config.componentId;
            this.lastModifiedTimestamp = config.lastModifiedTimestamp;

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

        initComponent: function() {
            var buttons = [];
            if (this.variant.id !== 'hippo-default') {
                buttons.push({
                    text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-delete'],
                    handler: function() {
                        Ext.Ajax.request({
                            method: 'DELETE',
                            url: this.composerRestMountUrl + '/' + this.componentId + './' +
                            encodeURIComponent(this.variant.id) + '?FORCE_CLIENT_HOST=true',
                            success: function() {
                                this.fireEvent('propertiesDeleted', this, this.variant.id);
                            },
                            scope: this
                        });
                    },
                    scope: this
                });
                buttons.push('->');
            }
            this.saveButton = new Ext.Button({
                text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-save'],
                handler: this._submitForm,
                scope: this,
                formBind: true
            });
            buttons.push(this.saveButton);
            buttons.push({
                text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-button-close'],
                scope: this,
                handler: function() {
                    this.fireEvent('close');
                }
            });

            Ext.apply(this, {
                autoHeight: true,
                border: false,
                padding: this.PADDING,
                autoScroll: true,
                labelWidth: 100,
                labelSeparator: '',
                monitorValid: true,
                defaults: {
                    anchor: '100%'
                },
                plugins: Hippo.ChannelManager.MarkRequiredFields,
                buttons: buttons
            });

            Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.initComponent.apply(this, arguments);

            this.addEvents('propertiesChanged', 'variantDirty', 'variantPristine', 'propertiesSaved', 'close', 'propertiesDeleted');
        },

        setNewVariant: function(newVariantId) {
            this.newVariantId = newVariantId;
            this._fireVariantDirtyOrPristine();
        },

        getVisibleHeight: function() {
            if (this.rendered) {
                return this.getHeight() + (2 * this.PADDING);
            }
            return 0;
        },

        _hasNewVariantId: function() {
            return this.variant && this.variant.id !== this.newVariantId;
        },

        isDirty: function() {
            return this.getForm().isDirty() || this._hasNewVariantId();
        },

        _submitForm: function() {
            var uncheckedValues = {},
                form = this.getForm();

            form.items.each(function(item) {
                if (item instanceof Ext.form.Checkbox) {
                    if (!item.checked) {
                        uncheckedValues[item.name] = 'off';
                    }
                }
            });

            form.submit({
                headers: {
                    'FORCE_CLIENT_HOST': 'true',
                    'lastModifiedTimestamp': this.lastModifiedTimestamp
                },
                params: uncheckedValues,
                url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/rename/' + encodeURIComponent(this.newVariantId) + '?FORCE_CLIENT_HOST=true',
                method: 'POST',
                success: function() {
                    this.fireEvent('propertiesSaved', this.newVariantId);
                },
                failure: function() {
                    Hippo.Msg.alert(Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['toolkit-store-error-message-title'],
                        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['toolkit-store-error-message'], function () {
                            Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').pageContainer.pageContext = null;
                            // reload channel manager
                            Ext.getCmp('Hippo.ChannelManager.TemplateComposer.Instance').pageContainer.refreshIframe();
                        });
                },
                scope: this
            });
        },

        _createDocument: function(ev, target, options) {
            var createUrl, createDocumentWindow;

            createUrl = this.composerRestMountUrl + '/' + this.mountId + './create?FORCE_CLIENT_HOST=true';
            createDocumentWindow = new Ext.Window({
                title: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-window-title'],
                height: 150,
                width: 400,
                modal: true,
                items: [
                    {
                        xtype: 'form',
                        height: 150,
                        padding: 10,
                        labelWidth: 120,
                        id: 'createDocumentForm',
                        defaults: {
                            labelSeparator: '',
                            anchor: '100%'
                        },
                        items: [
                            {
                                xtype: 'textfield',
                                fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-field-name'],
                                allowBlank: false
                            },
                            {
                                xtype: 'textfield',
                                disabled: true,
                                fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-field-location'],
                                value: options.docLocation
                            }
                        ]
                    }
                ],
                layout: 'fit',
                buttons: [
                    {
                        text: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-new-document-button'],
                        handler: function() {
                            var createDocForm = Ext.getCmp('createDocumentForm').getForm();
                            createDocForm.submit();
                            options.docName = createDocForm.items.get(0).getValue();

                            if (options.docName === '') {
                                return;
                            }
                            createDocumentWindow.hide();

                            Ext.Ajax.request({
                                url: createUrl,
                                params: options,
                                success: function() {
                                    Ext.getCmp(options.comboId).setValue(options.docLocation + "/" + options.docName);
                                },
                                failure: function() {
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

        _loadProperties: function(records) {
            this.records = records;
            var length = records.length, i, record, groupLabel, lastGroupLabel, value, defaultValue;
            if (length === 0) {
                this.add({
                    html: "<div style='padding:5px' align='center'>" + Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-no-properties'] + "</div>",
                    xtype: "panel",
                    autoWidth: true,
                    layout: 'fit'
                });
                this.saveButton.hide();
            } else {
                for (i = 0; i < length; i++) {
                    record = records[i];
                    if (record.get('hiddenInChannelManager') === false) {
                        groupLabel = record.get('groupLabel');
                        if (groupLabel !== lastGroupLabel) {
                            this.add({
                                cls: 'field-group-title ' + (lastGroupLabel === undefined ? 'first-field-group-title' : ''),
                                text: Ext.util.Format.htmlEncode(groupLabel),
                                xtype: 'label'
                            });
                            lastGroupLabel = groupLabel;
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
                }
                this.saveButton.show();
            }

            // do a shallow layout of the form to ensure our visible height is correct
            this.doLayout(true);

            this.fireEvent('propertiesLoaded', this);
        },

        addDocumentComboBox: function(record, defaultValue, value) {
            var comboStore, propertyField, createDocumentLinkId;

            comboStore = new Ext.data.JsonStore({
                root: 'data',
                url: this.composerRestMountUrl + '/' + this.mountId + './documents/' + record.get('docType') + '?FORCE_CLIENT_HOST=true',
                fields: ['path']
            });

            propertyField = this.add({
                fieldLabel: record.get('label'),
                xtype: 'combo',
                allowBlank: !record.get('required'),
                name: record.get('name'),
                value: value,
                defaultValue: defaultValue,
                store: comboStore,
                forceSelection: true,
                triggerAction: 'all',
                displayField: 'path',
                valueField: 'path',
                listeners: {
                    select: function(combo, comboRecord) {
                        record.set('value', comboRecord.get('path') || defaultValue);
                    }
                }
            });

            if (record.get('allowCreation')) {
                createDocumentLinkId = Ext.id();

                this.add({
                    bodyCfg: {
                        tag: 'div',
                        cls: 'create-document-link',
                        html: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['create-document-link-text'].format('<a href="#" id="' + createDocumentLinkId + '">&nbsp;', '&nbsp;</a>&nbsp;')
                    },
                    border: false
                });

                this.on('afterlayout', function() {
                    Ext.get(createDocumentLinkId).on("click", this._createDocument, this, {
                        docType: record.get('docType'),
                        docLocation: record.get('docLocation'),
                        comboId: propertyField.id
                    });
                }, this, { single: true });
            }
        },

        addComboBox: function(record, defaultValue, value) {
            var comboBoxValues, comboBoxDisplayValues, dataIndex, comboBoxValuesLength, data = [];

            comboBoxValues = record.get('dropDownListValues');
            comboBoxDisplayValues = record.get('dropDownListDisplayValues');

            for (dataIndex = 0, comboBoxValuesLength = comboBoxValues.length; dataIndex < comboBoxValuesLength; dataIndex++) {
                data.push([comboBoxValues[dataIndex], comboBoxDisplayValues[dataIndex]]);
            }

            this.add({
                xtype: 'combo',
                fieldLabel: record.get('label'),
                store: new Ext.data.ArrayStore({
                    fields: [
                        'id',
                        'displayText'
                    ],
                    data: data
                }),
                value: value,
                hiddenName: record.get('name'),
                typeAhead: true,
                mode: 'local',
                triggerAction: 'all',
                selectOnFocus: true,
                valueField: 'id',
                displayField: 'displayText',
                listeners: {
                    afterrender: function() {
                        // workaround, the padding-left which gets set on the element, let the right box side disappear,
                        // removing the style attribute after render fixes the layout
                        var formElement = this.el.findParent('.x-form-element');
                        formElement.removeAttribute('style');
                    },
                    select: function(combo, comboRecord) {
                        record.set('value', comboRecord.get('id') || defaultValue);
                    }
                }
            });
        },

        addComponent: function(record, defaultValue, value) {

            function updateValue() {
                var value = this.getValue();
                if (typeof(value) === 'undefined' || (typeof(value) === 'string' && value.length === 0) || value === this.defaultValue) {
                    this.addClass('default-value');
                    this.setValue(this.defaultValue);
                } else {
                    this.removeClass('default-value');
                }
                record.set('value', value);
            }

            var propertyField, xtype = record.get('type'),
                propertyFieldConfig = {
                    fieldLabel: record.get('label'),
                    xtype: xtype,
                    value: value,
                    defaultValue: defaultValue,
                    allowBlank: !record.get('required'),
                    name: record.get('name'),
                    listeners: {
                        change: updateValue,
                        select: updateValue,
                        specialkey: function(field, event) {
                            if (event.getKey() === event.ENTER) {
                                record.set('value', field.getValue() || defaultValue);
                            }
                        },
                        afterrender: function() {
                            // workaround, the padding-left which gets set on the element, let the right box side disappear,
                            // removing the style attribute after render fixes the layout
                            var formElement = this.el.findParent('.x-form-element');
                            formElement.removeAttribute('style');
                        }
                    }
                };

            if (xtype === 'checkbox') {
                propertyFieldConfig.checked = (value === true || value === 'true' || value === '1' || String(value).toLowerCase() === 'on');
                propertyFieldConfig.listeners.check = updateValue;
            } else if (xtype === 'linkpicker') {
                propertyFieldConfig.renderStripValue = /^\/?(?:[^\/]+\/)*/g;
                propertyFieldConfig.pickerConfig = {
                    configuration: record.get('pickerConfiguration'),
                    remembersLastVisited: record.get('pickerRemembersLastVisited'),
                    initialPath: record.get('pickerInitialPath'),
                    isRelativePath: record.get('pickerPathIsRelative'),
                    rootPath: record.get('pickerRootPath'),
                    selectableNodeTypes: record.get('pickerSelectableNodeTypes')
                };
            }
            propertyField = this.add(propertyFieldConfig);
            if (value === defaultValue) {
                propertyField.addClass('default-value');
            }
        },

        _loadException: function(proxy, type, actions, options, response) {
            var errorText = Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-load-exception-text'].format(actions);
            if (type === 'response') {
                errorText += '\n' + Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-load-exception-response'].format(response.statusText, response.status, options.url);
            }

            this.add({
                xtype: 'label',
                text: errorText,
                fieldLabel: Hippo.ChannelManager.TemplateComposer.PropertiesPanel.Resources['properties-panel-error-field-label']
            });
        },

        load: function() {
            return new Hippo.Future(function(success, fail) {
                var componentPropertiesStore = new Ext.data.JsonStore({
                    autoLoad: false,
                    method: 'GET',
                    root: 'properties',
                    fields: ['name', 'value', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation', 'defaultValue',
                        'pickerConfiguration', 'pickerInitialPath', 'pickerRemembersLastVisited', 'pickerPathIsRelative', 'pickerRootPath', 'pickerSelectableNodeTypes',
                        'dropDownListValues', 'dropDownListDisplayValues', 'hiddenInChannelManager', 'groupLabel' ],
                    url: this.composerRestMountUrl + '/' + this.componentId + './' + encodeURIComponent(this.variant.id) + '/' + this.locale + '?FORCE_CLIENT_HOST=true'
                });

                componentPropertiesStore.on('load', function(store, records) {
                    this._loadProperties(records);
                    success();
                }, this);
                componentPropertiesStore.on('update', function(store, record) {
                    this._onPropertiesChanged(store);
                }, this);
                componentPropertiesStore.on('exception', function() {
                    this._loadException.apply(this, arguments);
                    fail();
                }, this);
                componentPropertiesStore.load();
            }.createDelegate(this));
        },

        _onPropertiesChanged: function(store) {
            this._fireVariantDirtyOrPristine();
            this._firePropertiesChanged(store);
        },

        _fireVariantDirtyOrPristine: function() {
            if (this.isDirty()) {
                this.fireEvent('variantDirty');
            } else {
                this.fireEvent('variantPristine');
            }
        },

        _firePropertiesChanged: function(store) {
            var propertiesMap = {};
            store.each(function(record) {
                var name = record.get('name'),
                    value = record.get('value');
                propertiesMap[name] = value;
            });
            this.fireEvent('propertiesChanged', propertiesMap);
        },

        disableSave: function() {
            this.saveButton.disable();
        },

        enableSave: function() {
            this.saveButton.enable();
        }

    });

    Ext.reg('Hippo.ChannelManager.TemplateComposer.PropertiesForm', Hippo.ChannelManager.TemplateComposer.PropertiesForm);

}());