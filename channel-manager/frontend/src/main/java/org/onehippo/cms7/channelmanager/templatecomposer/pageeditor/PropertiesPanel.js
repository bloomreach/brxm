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

Hippo.ChannelManager.TemplateComposer.PropertiesPanel = Ext.extend(Ext.TabPanel, {
    
    composerRestMountUrl: null,
    mountId: null,
    resources: null,
    variants: null,
    future: null,
    variantsUuid: null,
    locale: null,
    componentId: null,
    
    constructor: function(config) {
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.variantsUuid = config.variantsUuid;
        this.mountId = config.mountId;
        this.resources = config.resources;
        this.locale = config.locale;
        config = Ext.apply(config, { activeTab: 0 });
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.constructor.call(this, config);
    },
    
    initComponent: function() {
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.initComponent.apply(this, arguments);
        this.future = new Hippo.Future(function(success, fail) {
            var variantsStore = new Ext.data.JsonStore({
                autoLoad: true,
                method: 'GET',
                root: 'data',
                fields:['id', 'name', 'description'],
                url: this.composerRestMountUrl +'/' + this.variantsUuid + './variants/?FORCE_CLIENT_HOST=true'
            });
            variantsStore.on('load', function (store, records, options) {
                this.loadVariants(records);
                success();
            }, this);
            variantsStore.on('exception', function(proxy, type, actions, options, response) {
                this.loadException(response);
                fail();
            }, this);
        }.createDelegate(this));
        this.on('tabchange', function(panel, tab) {
            if (tab) {
                this.fireEvent('variantChange', tab.componentId, tab.variant);
            }
        }, this);
    },
    
    loadVariants: function(records) {
        this.variants = ['default'];
        for (var i = 0; i < records.length; i++) {
            this.variants.push(records[i].get('id'));
        }
    },
    
    loadException: function(response) {
        Hippo.Msg.alert('Failed to get variants.', 'Only default variant will be available: ' + response.status + ':' + response.statusText); 
        this.variants = ['default'];
        this.initTabs();
    },

    initTabs: function() {
        for (var i = 0; i < this.variants.length; i++) {
            var form = new Hippo.ChannelManager.TemplateComposer.PropertiesForm({
                variant: this.variants[i],
                mountId: this.mountId,
                composerRestMountUrl: this.composerRestMountUrl,
                resources: this.resources,
                locale: this.locale,
                componentId: this.componentId
            });
            this.relayEvents(form, ['cancel']);
            this.add(form);
        }
    },
    
    load: function(variant) {
        this.removeAll();
        this.initTabs();
        this.selectVariant(variant);
        var self = this;
        this.future.when(function() {
            self.items.each(function(item) { item.load(); }, self);
        });
    },
    
    setComponentId: function(componentId) {
        this.componentId = componentId;
    },

    selectVariant: function(variant) {
        var self = this;
        this.future.when(function() {
            for (var i = 0; i < self.variants.length; i++) {
                if (self.variants[i] == variant) {
                    self.setActiveTab(i);
                    return;
                }
            }
            self.setActiveTab(0);
        });
    }
});
Hippo.ChannelManager.TemplateComposer.PropertiesForm = Ext.extend(Ext.FormPanel, {
    mountId: null,
    variant: null,
    composerRestMountUrl: null,
    resources: null,
    componentId: null,
    locale: null,
    
    constructor: function(config) {
        this.variant = config.variant;
        this.title = config.variant;
        this.mountId = config.mountId;
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.resources = config.resources;
        this.locale = config.locale;
        this.componentId = config.componentId;

        Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.constructor.call(this, config);

    },
    
    initComponent:function() {
        Ext.apply(this, {
            autoHeight: true,
            border:false,
            padding: 10,
            autoScroll:true,
            labelWidth: 100,
            labelSeparator: '',
            defaults:{
                anchor: '100%'
            },

            buttons:[
                {
                    text: this.resources['properties-panel-button-save'],
                    hidden: true,
                    handler: this.submitForm,
                    scope: this
                },
                {
                    text: this.resources['properties-panel-button-cancel'],
                    scope: this,
                    hidden: true,
                    handler: function () {
                        this.fireEvent('cancel');
                    }
                }
            ]
        });
        Hippo.ChannelManager.TemplateComposer.PropertiesForm.superclass.initComponent.apply(this, arguments);

        this.addEvents('save', 'cancel');
    },

    submitForm:function () {
        this.fireEvent('save');
        // don't send the override checkbox fields
        this.items.each(function(item) {
            if (item.name === 'override') {
                item.setDisabled(true);
            }
        });
        this.getForm().submit({
            headers: {
                    'FORCE_CLIENT_HOST': 'true'
            },
            url: this.composerRestMountUrl +'/'+ this.componentId + './parameters/' + this.variant + '?FORCE_CLIENT_HOST=true',
            method: 'POST',
            success: function () {
                Hippo.ChannelManager.TemplateComposer.Instance.selectVariant(this.componentId, this.variant);
                Ext.getCmp('componentPropertiesPanel').load(this.variant);
            }.bind(this)
        });
    },

    createDocument: function (ev, target, options) {

        var self = this;
        var createUrl = this.composerRestMountUrl +'/'+ this.mountId + './create?FORCE_CLIENT_HOST=true';
        var createDocumentWindow = new Ext.Window({
            title: this.resources['create-new-document-window-title'],
            height: 150,
            width: 400,
            modal: true,
            items:[
                {
                    xtype: 'form',
                    height: 150,
                    padding: 10,
                    labelWidth: 120,
                    id: 'createDocumentForm',
                    defaults:{
                        labelSeparator: '',
                        anchor: '100%'
                    },
                    items:[
                        {
                            xtype: 'textfield',
                            fieldLabel: this.resources['create-new-document-field-name'],
                            allowBlank: false
                        },
                        {
                            xtype: 'textfield',
                            disabled: true,
                            fieldLabel: this.resources['create-new-document-field-location'],
                            value: options.docLocation
                        }
                    ]
                }
            ],
            layout: 'fit',
            buttons:[
                {
                    text: this.resources['create-new-document-button'],
                    handler: function () {
                        var createDocForm = Ext.getCmp('createDocumentForm').getForm()
                        createDocForm.submit();
                        options.docName = createDocForm.items.get(0).getValue();

                        if (options.docName == '') {
                            return;
                        }
                        createDocumentWindow.hide();

                        Ext.Ajax.request({
                            url: createUrl,
                            params: options,
                            success: function () {
                                Ext.getCmp(options.comboId).setValue(options.docLocation + "/" + options.docName);
                            },
                            failure: function() {
                                Hippo.Msg.alert(self.resources['create-new-document-message'], self.resources['create-new-document-failed'],
                                    function () {
                                        Hippo.ChannelManager.TemplateComposer.Instance.initComposer();
                                    }
                                );
                            }
                        });

                    }
                }
            ]
        });
        createDocumentWindow.addButton({text: this.resources['create-new-document-button-cancel']}, function() {
            this.hide();
        }, createDocumentWindow);


        createDocumentWindow.show();

    },

    loadProperties:function(store, records, options) {
        var length = records.length;
        if (length == 0) {
            this.add({
                html: "<div style='padding:5px' align='center'>"+this.resources['properties-panel-no-properties']+"</div>",
                xtype: "panel",
                autoWidth: true,
                layout: 'fit'
            });
            this.buttons[0].hide();
            this.buttons[1].hide();
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
                        root: 'data',
                        url: this.composerRestMountUrl +'/' + this.mountId + './documents/' + property.get('docType') + '?FORCE_CLIENT_HOST=true',
                        fields:['path']
                    });

                    propertyField = this.add({
                        fieldLabel: property.get('label'),
                        xtype: 'combo',
                        allowBlank: !property.get('required'),
                        name: property.get('name'),
                        value: value,
                        defaultValue: defaultValue,
                        store: comboStore,
                        forceSelection: true,
                        triggerAction: 'all',
                        displayField: 'path',
                        valueField: 'path'
                    });

                    if (property.get('allowCreation')) {
                        this.add({
                            bodyCfg: {
                                tag: 'div',
                                cls: 'create-document-link',
                                html: this.resources['create-document-link-text'].format('<a href="#" id="combo'+ i +'">&nbsp;', '&nbsp;</a>&nbsp;')
                            },
                            border: false

                        });

                        this.doLayout(false, true); //Layout the form otherwise we can't use the link in the panel.
                        Ext.get("combo" + i).on("click", this.createDocument, this, 
					    {   
                            docType: property.get('docType'), 
						    docLocation: property.get('docLocation'),
						    comboId: propertyField.id
					     });
                    }

                } else {
                    propertyField = this.add({
                        fieldLabel: property.get('label'),
                        xtype: property.get('type'),
                        value: value,
                        defaultValue: defaultValue,
                        allowBlank: !property.get('required'),
                        name: property.get('name'),
                        listeners: {
                            change: function() {
                                var value = this.getValue();
                                if (!value || value.length === 0 || value === this.defaultValue) {
                                    this.addClass('default-value');
                                    this.setValue(this.defaultValue);
                                } else {
                                    this.removeClass('default-value');
                                }
                            }
                        }
                    });
                    if (isDefaultValue) {
                        propertyField.addClass('default-value');
                    }
                }
            }
            this.buttons[0].show();
            this.buttons[1].show();
        }
        this.doLayout(false, true);
    },

    loadException:function(proxy, type, actions, options, response) {
        console.dir(arguments);

        var errorText = this.resources['properties-panel-load-exception-text'].format(actions);
        if (type == 'response') {
            errorText += '\n'+this.resources['properties-panel-load-exception-response'].format(response.statusText, response.status, options.url);
        }

        this.add({
            xtype: 'label',
            text: errorText,
            fieldLabel: this.resources['properties-panel-error-field-label']
        });

        this.doLayout(false, true);
    },

    load: function() {
        var componentPropertiesStore = new Ext.data.JsonStore({
            autoLoad: true,
            method: 'GET',
            root: 'properties',
            fields:['name', 'value', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation', 'defaultValue' ],
            url: this.composerRestMountUrl +'/'+ this.componentId + './parameters/' + this.locale + '/' + this.variant + '?FORCE_CLIENT_HOST=true'
        });

        componentPropertiesStore.on('load', this.loadProperties, this);
        componentPropertiesStore.on('exception', this.loadException, this);
    }

});

//Add * to the required fields 

Ext.apply(Ext.layout.FormLayout.prototype, {
    originalRenderItem:Ext.layout.FormLayout.prototype.renderItem,
    renderItem:function(c, position, target) {
        if (c && !c.rendered && c.isFormField && c.fieldLabel && c.allowBlank === false) {
            c.fieldLabel = c.fieldLabel + " <span class=\"req\">*</span>";
        }
        this.originalRenderItem.apply(this, arguments);
    }
});
