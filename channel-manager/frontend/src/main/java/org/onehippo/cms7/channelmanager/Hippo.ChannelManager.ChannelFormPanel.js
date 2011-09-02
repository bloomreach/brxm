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

Ext.QuickTips.init();
Ext.namespace('Hippo.ChannelManager');

Ext.apply(Ext.form.VTypes, {
    'hippourl': function(value) {
            var expr = /(^http:\/\/([\-\w]+\.)*\w(\/[%\-\w]+(\.\w{2,})?)*)/i;
            return expr.test(value);
        },
    'hippourlText' : 'This field should be a URL in the format "http:/'+'/www.example.com"'
});

/**
 * @class Hippo.ChannelManager.ChannelFormPanel
 * @extends Ext.form.FormPanel
 */
Hippo.ChannelManager.ChannelFormPanel = Ext.extend(Ext.form.FormPanel, {
            constructor: function(config) {
                this.store = config.store;
                Hippo.ChannelManager.ChannelFormPanel.superclass.constructor.call(this, config);
            },

            initComponent: function() {
                var me = this;
                var config = {
                    padding: 5,
                    url: me.store.url,
                    defaults: {
                        labelAlign: 'top',
                        width: 450,
                        labelWidth: 100
                    },
                    items:[
                        {
                            xtype: 'displayfield',
                            fieldLabel: 'Blueprint',
                            id: 'displayedBlueprintId',
                            style: {
                                textAlign: 'left',
                                paddingLeft: '70px'
                            }
                        },
                        {
                            xtype: 'hidden',
                            fieldLabel: 'Blueprint',
                            id: 'blueprintId'
                        },
                        {
                            xtype: 'textfield',
                            fieldLabel: 'Name',
                            id: 'name',
                            allowBlank: false
                        },
                        {
                            xtype: 'textfield',
                            fieldLabel: 'URL',
                            id: 'url',
                            allowBlank: false,
                            vtype: 'hippourl'
                        },
                        {
                            xtype: 'textarea',
                            fieldLabel: 'Description',
                            id: 'description'
                        },
                        {
                            xtype: 'linkpicker',
                            fieldLabel: 'Content Root',
                            id: 'contentRoot',
                            style: {
                                marginLeft: '69px'
                            },
                            pickerConfig: {
                                configuration: "cms-pickers/folders",
                                selectableNodeTypes: "hippostd:folder"
                            }
                        }
                    ]
                };

                Ext.apply(this, Ext.apply(this.initialConfig, config));
                Hippo.ChannelManager.ChannelFormPanel.superclass.initComponent.apply(this, arguments);
                this.addEvents('channel-created');
                this.on('beforeshow', function () {
                    this.getForm().reset();
                    var blueprint = Ext.getCmp('blueprints-panel').getSelectionModel().getSelected();
                    Ext.getCmp('displayedBlueprintId').setValue(blueprint.id);
                    Ext.getCmp('blueprintId').setValue(blueprint.id);
                    Ext.getCmp('contentRoot').setValue(blueprint.get('contentRoot'));
                }, this);
                this.doLayout();
            },

            submitForm: function() {
                var form = this.getForm();
                var panel = this;
                if (form.isValid()) {
                    form.submit({
                                params: {
                                    xaction: 'create',
                                    records: Ext.encode(form.getValues())
                                },

                                success: function(form, action) {
                                    //Event to hide the window & rerender the channels panel
                                    panel.fireEvent('channel-created');
                                },
                                failure: function(form, action) {
                                    switch (action.failureType) {
                                        case Ext.form.Action.CLIENT_INVALID:
                                            Ext.Msg.alert("Error", "Please check the highlighted fields");
                                            break;
                                        case Ext.form.Action.CONNECT_FAILURE:
                                            Ext.Msg.alert("Error", "Unable to connect to the server");
                                            break;
                                        case Ext.form.Action.SERVER_INVALID:
                                            Ext.Msg.alert("Error", "Error while creating the Channel " + action.result.msg);
                                            break;
                                    }
                                }
                            });
                } else {

                }
            }
        });

Ext.reg('Hippo.ChannelManager.ChannelFormPanel', Hippo.ChannelManager.ChannelFormPanel);
