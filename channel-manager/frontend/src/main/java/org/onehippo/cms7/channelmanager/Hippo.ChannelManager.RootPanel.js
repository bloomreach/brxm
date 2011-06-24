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
 * @class Hippo.ChannelManager.RootPanel
 * @extends Ext.Panel
 */
Hippo.ChannelManager.RootPanel = Ext.extend(Ext.Panel, {
            constructor: function(config) {
                this.channelStore = config.channelStore;
                this.blueprintStore = config.blueprintStore;
                Hippo.ChannelManager.RootPanel.superclass.constructor.call(this, config);
            },

            initComponent: function() {
                var me = this;
                var config = {
                    layout: 'border',
                    height: 900,
                    title: "Channel Manager",
                    tbar: [
                        {
                            text: "New Channel",
                            handler: me.openChannelWizard,
                            scope: me
                        }
                    ],
                    viewConfig: {
                        forceFit: true
                    }

                };

                Ext.apply(this, Ext.apply(this.initialConfig, config));

                Hippo.ChannelManager.RootPanel.superclass.initComponent.apply(this, arguments);
                this.win = new Hippo.ChannelManager.NewChannelWindow({
                            blueprintStore: me.blueprintStore,
                            channelStore : me.channelStore
                        });

                //Register event listeners on event
                Ext.getCmp('channel-form-panel').on('channel-created', function() {
                    this.win.hide();
                    this.channelStore.reload();
                }, this);
            },

            openChannelWizard:function() {
                this.win.show();
            }
        });

Ext.reg('Hippo.ChannelManager.RootPanel', Hippo.ChannelManager.RootPanel);


Hippo.ChannelManager.NewChannelWindow = Ext.extend(Ext.Window, {
            constructor: function(config) {
                this.blueprintStore = config.blueprintStore;
                this.channelStore = config.channelStore;
                Hippo.ChannelManager.NewChannelWindow.superclass.constructor.call(this, config);
            },

            initComponent: function() {
                var me = this;
                var config = {
                    title: "Blueprint Chooser",
                    width: 720,
                    height: 450,
                    modal: true,
                    resizable: false,
                    closeAction: 'hide',
                    layout:'fit',
                    items: [
                        {
                            id: 'card-container',
                            layout: 'card',
                            activeItem: 0,
                            layoutConfig: {
                                hideMode:'offsets',
                                deferredRender: true ,
                                layoutOnCardChange: true
                            }
                        }
                    ],
                    buttons: [
                        {
                            id: 'cancelButton',
                            text: 'Cancel',
                            scope: me,
                            handler: function() {
                                this.hide();
                            }

                        },
                        {
                            id: 'createButton',
                            text: 'Choose ...',
                            handler: me.processNextStep,
                            scope: me
                        }
                    ]

                };

                Ext.apply(this, Ext.apply(this.initialConfig, config));

                Hippo.ChannelManager.NewChannelWindow.superclass.initComponent.apply(this, arguments);

                this.on('beforeshow', function () {
                    Ext.getCmp('card-container').layout.setActiveItem('blueprints-panel');
                }, this);

                Ext.getCmp('card-container').add(new Hippo.ChannelManager.BlueprintListPanel({
                            id: 'blueprints-panel',
                            store: me.blueprintStore
                        }));

                Ext.getCmp('card-container').add(new Hippo.ChannelManager.ChannelFormPanel({
                            id: 'channel-form-panel',
                            store: me.channelStore
                        }));


            },

            processNextStep:function() {
                var cc = Ext.getCmp('card-container');
                if (cc.layout.activeItem.id === 'blueprints-panel') {
                    this.setTitle("Channel Properties");
                    Ext.getCmp('createButton').setText("Create Channel");
                    cc.layout.setActiveItem('channel-form-panel');
                } else { //current item is the form panel so call submit on it.
                    Ext.getCmp('channel-form-panel').submitForm();
                }
            }
        }
//end extending Config
);



