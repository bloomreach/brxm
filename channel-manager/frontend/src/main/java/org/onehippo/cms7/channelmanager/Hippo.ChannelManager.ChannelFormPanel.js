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
                    width: 720,
                    height: 400,
                    viewConfig: {
                        forceFit: true
                    },
                    defaults: {
                        labelWidth: 40,
                        width: 270
                    },
                    padding: 5,
                    items:[
                        {
                            xtype: 'textfield',
                            fieldLabel: 'Name',
                            id: 'name',
                            allowBlank: false
                        },
                    ]

                };

                Ext.apply(this, Ext.apply(this.initialConfig, config));
                Hippo.ChannelManager.ChannelFormPanel.superclass.initComponent.apply(this, arguments);

            },

            submitForm: function() {
                console.log('TODO: Submit the form to the store URL');
            }
        });

Ext.reg('Hippo.ChannelManager.ChannelFormPanel', Hippo.ChannelManager.ChannelFormPanel);
