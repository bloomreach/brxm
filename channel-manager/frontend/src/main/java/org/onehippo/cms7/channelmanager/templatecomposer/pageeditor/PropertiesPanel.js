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

Ext.namespace('Hippo.ChannelManager.TemplateComposer');

Hippo.ChannelManager.TemplateComposer.PropertiesPanel = Ext.extend(Ext.FormPanel, {
    initComponent:function() {
        Ext.apply(this, {
            autoHeight: true,
            border:false,
            padding: 10,
            autoScroll:true,
            labelWidth: 100,
            labelSeparator: '',
            defaults:{
                width: 170,
                anchor: '100%'
            },

            buttons:[
                {
                    text: "Save",
                    hidden: true,
                    handler: this.submitForm,
                    scope: this
                },
                {
                    text: "Reset",
                    scope: this,
                    hidden: true,
                    handler: function () {
                        this.reload(this.id);
                    }
                }
            ]
        });
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.initComponent.apply(this, arguments);
    },

    submitForm:function () {
        this.getForm().submit({
            url: this.composerRestMountUrl + this.id + './parameters',
            method: 'POST' ,
            waitMsg: 'Saving properties ...',
            success: function () {
                Hippo.ChannelManager.TemplateComposer.Instance.refreshIframe();
            }
        });
    },

    onRender:function() {
        Hippo.ChannelManager.TemplateComposer.PropertiesPanel.superclass.onRender.apply(this, arguments);
    },

    createDocument: function (ev, target, options) {
	  
        var createUrl = this.composerRestMountUrl + this.mountId + './create';
        var createDocumentWindow = new Ext.Window({
            title: "Create a new document",
            height: 150,
            width: 400,
            modal: true,
            items:[{
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
                            fieldLabel:'Document Name',
                            allowBlank: false
                        },
                        {
			    xtype: 'textfield',
                            disabled: true,
                            fieldLabel:'Document Location',
                            value: options.docLocation
                        }
                    ]
                }],
            layout: 'fit',
            buttons:[
                {
                    text: 'Create Document',
                    handler:function () {
                        var createDocForm = Ext.getCmp('createDocumentForm').getForm()
                        createDocForm.submit();
                        options.docName = createDocForm.items.get(0).getValue();

                        if (options.docName == '') {
                            return;
                        }
                        createDocumentWindow.hide();

                        Hippo.Msg.wait("Creating Document ... ");
                        Ext.Ajax.request({
                            url: createUrl,
                            params: options,
                            success: function () {
                                Ext.getCmp(options.comboId).setValue(options.docLocation + "/" + options.docName);
                                Hippo.ChannelManager.TemplateComposer.Instance.on('iFrameInitialized', function() {
                                    Hippo.Msg.hide();
                                }, this, {single : true});
                                Hippo.ChannelManager.TemplateComposer.Instance.refreshIframe();
                            }
                        });

                    }
                }]
        });
	createDocumentWindow.addButton({text: 'Cancel'},function(){this.hide();},createDocumentWindow);
	

        createDocumentWindow.show();

    },

    loadProperties:function(store, records, options) {
        this.removeAll();

        var length = records.length;
        if (length == 0) {
            this.add({
                html: "<div style='padding:5px' align='center'>No editable properties found for this component</div>",
                xtype: "panel",
                autoWidth: true,
                layout: 'fit'
            });
            this.buttons[0].hide();
            this.buttons[1].hide();
        } else {
            for (var i = 0; i < length; ++i) {
                var property = records[i];
                if (property.get('type') == 'combo') {
                    var comboStore = new Ext.data.JsonStore({
                        root: 'data',
                        url: this.composerRestMountUrl + this.mountId + './documents/'+ property.get('docType'),
                        fields:['path']
                    });

                    var field = this.add({
                        fieldLabel: property.get('label'),
                        xtype: property.get('type'),
                        allowBlank: !property.get('required'),
                        name: property.get('name'),
                        value: property.get('value'),
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
                                html: 'Or <a href="#" id="combo' + i  +'" >&nbsp;Create Document&nbsp;</a>&nbsp;'
                            },
                            border: false

                        });

                        this.doLayout(false, true); //Layout the form otherwise we can't use the link in the panel.
                        Ext.get("combo" + i).on("click", this.createDocument, this, 
					    {      docType: property.get('docType'), 
						    docLocation: property.get('docLocation'),
						    comboId: field.id
					     });
                    }

                } else {
                    this.add({
                        fieldLabel: property.get('label'),
                        xtype: property.get('type'),
                        value: property.get('value'),
                        allowBlank: !property.get('required'),
                        name: property.get('name')
                    });
                }


            }
            this.buttons[0].show();
            this.buttons[1].show();
        }

        this.doLayout(false, true);
        this.getForm().clearInvalid();
    },

    loadException:function(proxy, type, actions, options, response) {
        console.dir(arguments);
        this.removeAll();

        var errorText = 'Error during ' + actions + '. ';
        if (type == 'response') {
            errorText += '\nServer returned statusText: ' + response.statusText + ', statusCode: '
                    + response.status + ' for request.url=' + options.url;
        }

        this.add({
            xtype: 'label',
            text: errorText,
            fieldLabel: 'Error information'
        });

        this.doLayout(false, true);
    }
    ,

    reload:function(id, name, path) {
        // the id is set with the onClick of the component
        this.id = id;
        var store = new Ext.data.JsonStore({
            autoLoad: true,
            method: 'GET',
            root: 'properties',
            fields:['name', 'value', 'label', 'required', 'description', 'docType', 'type', 'docLocation', 'allowCreation' ],
            url: this.composerRestMountUrl + id + './parameters'
        });
        store.on('load', this.loadProperties, this);
        store.on('exception', this.loadException, this);
    }

})
        ;
Ext.reg('h_properties_panel', Hippo.ChannelManager.TemplateComposer.PropertiesPanel);

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
