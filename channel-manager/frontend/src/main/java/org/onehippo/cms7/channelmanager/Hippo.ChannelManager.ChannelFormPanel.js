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
Hippo.ChannelManager.ChannelFormPanel = Ext.extend(Ext.Panel, {
            constructor: function(config) {
                this.store = config.store;
                Hippo.ChannelManager.ChannelFormPanel.superclass.constructor.call(this, config);
            },

            initComponent: function() {
                var me = this;
                var config = {
                    layout: 'fit',
                    padding: 5,
                    height: 400,
                    items:[
                        {
                            xtype: 'form',
                            id: 'channel-form',
                            autoHeight: true,
                            url: me.store.url,
                            height: 400,
                            defaults :{
                                width: 650,
                                labelAlign: 'top'
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
                                }
                            ]
                        }
                    ]
                };

                Ext.apply(this, Ext.apply(this.initialConfig, config));
                Hippo.ChannelManager.ChannelFormPanel.superclass.initComponent.apply(this, arguments);
                Ext.getCmp('channel-form').doLayout();
            },

            submitForm: function() {
                var form = Ext.getCmp('channel-form').getForm();
                if (form.isValid()) {
                    form.submit({
                                params: {
                                    xaction: 'create',
                                    records: Ext.encode(form.getValues())
                                },

                                success: function(form, action) {
                                    //Event to hide the window & rerender the channels panel
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
                                            Ext.Msg.alert("Error", "Error while creating the A/B test" + action.result.msg);
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
