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
    personas: null,
    
    constructor: function(config) {
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.mountId = config.mountId;
        this.resources = config.resources;
        config = Ext.apply(config, { activeTab: 0 });
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.constructor.call(this, config);
    },
    
    initComponent: function() {
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.initComponent.apply(this, arguments);
        var personasStore = new Ext.data.JsonStore({
            autoLoad: true,
            method: 'GET',
            root: 'data',
            fields:['id', 'name', 'description'],
            url: this.composerRestMountUrl +'/cafebabe-cafe-babe-cafe-babecafebabe./personas/?FORCE_CLIENT_HOST=true'
        });
        personasStore.on('load', this.loadPersonas, this);
        personasStore.on('exception', this.loadException, this);
    },
    
    loadPersonas: function(store, records, options) {
        this.personas = ['default'];
        for (var i = 0; i < records.length; i++) {
            this.personas.push(records[i].get('id'));
        }
        this.initTabs();
    },
    
    loadException: function(proxy, type, actions, options, response) {
        Hippo.Msg.alert('Failed to get personas.', 'Only default persona will be available: ' + response.status + ':' + response.statusText); 
        this.personas = ['default'];
        this.initTabs();
    },

    initTabs: function() {
        for (var i = 0; i < this.personas.length; i++) {
            var form = new Hippo.ChannelManager.TemplateComposer.PropertiesForm({
                persona: this.personas[i],
                mountId: this.mountId,
                composerRestMountUrl: this.composerRestMountUrl,
                resources: this.resources
            });
            this.relayEvents(form, ['cancel']);
            this.add(form);
        }
        this.setActiveTab(0);
    },
    
    reload: function() {
        this.items.each(function(item) { item.reload(); }, this);
    },
    
    setComponentId: function(itemId) {
        this.items.each(function(item) { item.setComponentId(itemId); }, this);
    }

});
Hippo.ChannelManager.TemplateComposer.PropertiesForm = Ext.extend(Ext.FormPanel, {
    mountId: null,
    persona: null,
    composerRestMountUrl: null,
    resources: null,
    componentId: null,
    
    constructor: function(config) {
        this.persona = config.persona;
        this.title = config.persona;
        this.mountId = config.mountId;
        this.composerRestMountUrl = config.composerRestMountUrl;
        this.resources = config.resources;
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
            url: this.composerRestMountUrl +'/'+ this.componentId + './parameters/' + this.persona + '?FORCE_CLIENT_HOST=true',
            method: 'POST',
            success: function () {
                Hippo.ChannelManager.TemplateComposer.Instance.renderComponent(this.componentId, { persona: this.persona });
                Ext.getCmp('componentPropertiesPanel').reload();
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
        var that = this;
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
                var isDefault = false;
                if (!value || value.length === 0) {
                    value = property.get('defaultValue');
                    isDefault = true;
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
                        disabled: isDefault,
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
                    var propertyField = this.add({
                        fieldLabel: property.get('label'),
                        xtype: property.get('type'),
                        value: value,
                        allowBlank: !property.get('required'),
                        name: property.get('name'),
                        disabled: isDefault
                    });
                }
                this.add({
                    xtype: 'checkbox',
                    name: 'override',
                    checked: !isDefault,
                    propertyField: propertyField,
                    property: property,
                    listeners: {
                        check: function(checkbox, checked) {
                            if (checked) {
                                this.propertyField.setDisabled(false);
                            } else {
                                this.propertyField.setDisabled(true);
                                this.propertyField.setValue(this.property.get('defaultValue'));
                            }
                        }
                    }
                });

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

    clearPanel: function() {
        this.removeAll();
        this.buttons[0].hide();
        this.buttons[1].hide();
    },

    reload: function(onloadCallback) {
        this.removeAll();
        if (this.componentPropertiesStore) {
            this.componentPropertiesStore.destroy();
        }

        this.componentPropertiesStore = new Ext.data.JsonStore({
            autoLoad: true,
            method: 'GET',
            root: 'properties',
            fields:['name', 'value', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation', 'defaultValue' ],
            url: this.composerRestMountUrl +'/'+ this.componentId + './parameters/' + this.locale + '/' + this.persona + '?FORCE_CLIENT_HOST=true'
        });

        this.componentPropertiesStore.on('load', this.loadProperties, this);
        if (onloadCallback) {
            this.componentPropertiesStore.on('load', onloadCallback, this);
        }
        this.componentPropertiesStore.on('exception', this.loadException, this);
    },

    setComponentId: function(componentId) {
        this.componentId = componentId;
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
