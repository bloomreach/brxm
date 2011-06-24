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

Ext.namespace('Hippo.ChannelManager');

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
                        labelWidth: 75
                    },
                    items:[
                        {
                            xtype: 'textfield',
                            fieldLabel: 'Name',
                            id: 'name',
                            allowBlank: false
                        },
                        {
                            xtype: 'textfield',
                            fieldLabel: 'Domain',
                            id: 'domain',
                            allowBlank: false
                        },
                        {
                            xtype: 'textarea',
                            fieldLabel: 'Description',
                            id: 'description'
                        }
                    ]
                };

                Ext.apply(this, Ext.apply(this.initialConfig, config));
                Hippo.ChannelManager.ChannelFormPanel.superclass.initComponent.apply(this, arguments);
                this.addEvents('channel-created');
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
                                    console.log(action);
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
                    //Do nothing?
                }

            }
        });

Ext.reg('Hippo.ChannelManager.ChannelFormPanel', Hippo.ChannelManager.ChannelFormPanel);
