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
            viewConfig: {
                forceFit: true
            }
        };

       // recalculate the ExtJs layout when the YUI layout manager fires a resize event
        this.on('afterlayout', function(portal, layout) {
            var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
            YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, this.doLayout, true);
        }, this, {single: true});

        Ext.apply(this, Ext.apply(this.initialConfig, config));

        Hippo.ChannelManager.RootPanel.superclass.initComponent.apply(this, arguments);

        // get all child components
        this.win = new Hippo.ChannelManager.NewChannelWindow({
            blueprintStore: me.blueprintStore,
            channelStore : me.channelStore
        });
        this.formPanel = Ext.getCmp('channel-form-panel');
        this.gridPanel = Ext.getCmp('channel-grid-panel');
        this.propertiesPanel = Ext.getCmp('channel-properties-panel');

        // register channel creation events
        this.gridPanel.on('add-channel', function() {
            this.win.show();
        }, this);
        this.formPanel.on('channel-created', function() {
            this.win.hide();
            this.channelStore.reload();
        }, this);

        // register properties panel events
        this.gridPanel.on('channel-selected', function(channelId, channelName) {
            this.propertiesPanel.showPanel(channelId, channelName);
        }, this);
        this.gridPanel.on('channel-deselected', function(sm) {
            if (this.propertiesPanel.isShown()) {
                this.propertiesPanel.hidePanel();
            } else {
                this.gridPanel.selectRow(-1);
            }
        }, this);
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



