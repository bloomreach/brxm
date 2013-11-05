/**
 * Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager');

    /**
     * @class Hippo.ChannelManager.RootPanel
     * @extends Ext.Panel
     */
    Hippo.ChannelManager.RootPanel = Ext.extend(Ext.Panel, {

        constructor: function(config) {
            this.channelStore = config.channelStore;
            this.blueprintStore = config.blueprintStore;
            this.resources = config.resources;
            this.selectedChannelId = null;

            this.toolbar = new Hippo.ChannelManager.BreadcrumbToolbar({
                autoWidth: true,
                id: 'hippo-channelmanager-breadcrumb',
                breadcrumbIconUrl: config.breadcrumbIconUrl,
                layoutConfig: {
                    pack: 'left'
                }
            });

            Ext.apply(config, {
                id: 'rootPanel',
                layout: 'card',
                layoutOnCardChange: true,
                deferredRender: true,
                viewConfig: {
                    forceFit: true
                },
                border: false,
                tbar: this.toolbar
            });

            Hippo.ChannelManager.RootPanel.superclass.constructor.call(this, config);

            this.selectCard(config.activeItem);
        },

        initComponent: function() {
            var self, channelSelectedHandler;

            self = this;

            // recalculate the ExtJs layout when the YUI layout manager fires a resize event
            this.on('afterlayout', function() {
                var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
                YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function(sizes) {
                    self.setSize(sizes.body.w, sizes.body.h);
                    self.doLayout();
                }, true);
            }, this, {single: true});

            // get all child components
            this.win = new Hippo.ChannelManager.NewChannelWindow({
                blueprintStore: self.blueprintStore,
                channelStore: self.channelStore,
                resources: self.resources
            });
            this.formPanel = Ext.getCmp('channel-form-panel');
            this.gridPanel = Ext.getCmp('channel-grid-panel');
            this.channelIconPanel = Ext.getCmp('channelIconPanel');
            this.channelOverviewPanel = Ext.getCmp('channelOverview');

            // register channel creation events
            this.channelOverviewPanel.on('add-channel', function() {
                this.win.show();
            }, this);
            this.formPanel.on('channel-created', function() {
                this.win.hide();
                this.channelStore.reload();
            }, this);

            channelSelectedHandler = function(channelId, record) {
                this.selectedChannelId = channelId;
                // don't activate template composer when it is already active
                if (this.layout.activeItem === Hippo.ChannelManager.TemplateComposer.Instance) {
                    return;
                }
                Hippo.ChannelManager.TemplateComposer.Instance.browseTo({ channelId: channelId });
                Ext.getCmp('rootPanel').showTemplateComposer();
                // TODO fix, I have no clue why the template composer card is not getting activated properly
                document.getElementById('Hippo.ChannelManager.TemplateComposer.Instance').className = 'x-panel';
            };

            this.gridPanel.on('channel-selected', channelSelectedHandler, this);
            this.channelIconPanel.on('channel-selected', channelSelectedHandler, this);

            Hippo.ChannelManager.TemplateComposer.Instance.on('mountChanged', function(data) {
                var channelRecord = this.gridPanel.getChannelByMountId(data.mountId),
                        firstChange = data.oldMountId === null;
                if (!firstChange && this.selectedChannelId !== channelRecord.get('id')) {
                    this.gridPanel.selectChannel(channelRecord.get('id'));
                }
            }, this);

            Hippo.ChannelManager.RootPanel.superclass.initComponent.apply(this, arguments);
        },

        selectCard: function(itemId) {
            while (this.toolbar.getBreadcrumbSize() > 0) {
                this.toolbar.popItem();
            }

            this.toolbar.pushItem({
                card: this.items.get(0),
                click: function() {
                    this.layout.setActiveItem(0);
                },
                scope: this
            });
            if ((typeof itemId !== 'undefined') && itemId !== 0) {
                this.toolbar.pushItem({
                    card: this.items.get(itemId),
                    click: function() {
                        this.layout.setActiveItem(itemId);
                    },
                    scope: this
                });

                this.layout.setActiveItem(itemId);
                if (itemId === 1) {
                    // TODO fix, I have no clue why the template composer card is not getting activated properly
                    document.getElementById('Hippo.ChannelManager.TemplateComposer.Instance').className = 'x-panel';
                }
            } else {
                this.layout.setActiveItem(0);
            }
        },

        update: function(config) {
            this.selectCard(config.activeItem);
        },

        showChannelManager: function() {
            this.layout.setActiveItem(0);
        },

        showTemplateComposer: function() {
            this.toolbar.pushItem({
                card: this.items.get(1),
                click: function() {
                    this.layout.setActiveItem(1);
                },
                scope: this
            });
            this.layout.setActiveItem(1);
        },

        showConfigEditor: function() {
            this.toolbar.pushItem({
                card: this.items.get(2),
                click: function() {
                    this.layout.setActiveItem(2);
                },
                scope: this
            });
            this.layout.setActiveItem(2);
        }
    });

    Ext.reg('Hippo.ChannelManager.RootPanel', Hippo.ChannelManager.RootPanel);


    Hippo.ChannelManager.NewChannelWindow = Ext.extend(Ext.Window, {

        constructor: function(config) {
            this.blueprintStore = config.blueprintStore;
            this.channelStore = config.channelStore;
            this.resources = config.resources;

            Hippo.ChannelManager.NewChannelWindow.superclass.constructor.call(this, config);
        },

        initComponent: function() {
            var config = {
                title: this.resources['new-channel-blueprint'],
                width: 720,
                height: 450,
                modal: true,
                resizable: false,
                closeAction: 'hide',
                layout: 'fit',
                items: [
                    {
                        id: 'card-container',
                        layout: 'card',
                        activeItem: 0,
                        layoutConfig: {
                            hideMode: 'offsets',
                            deferredRender: true,
                            layoutOnCardChange: true
                        }
                    }
                ],
                buttonAlign: 'left',
                buttons: [
                    {
                        id: 'previousButton',
                        text: this.resources['new-channel-previous'],
                        handler: this.processPreviousStep,
                        scope: this,
                        hidden: true
                    },
                    '->',
                    {
                        id: 'nextButton',
                        text: this.resources['new-channel-next'],
                        handler: this.processNextStep,
                        scope: this
                    },
                    {
                        id: 'cancelButton',
                        text: this.resources['new-channel-cancel'],
                        scope: this,
                        handler: function() {
                            this.hide();
                        }

                    }
                ]

            };

            Ext.apply(this, Ext.apply(this.initialConfig, config));

            Hippo.ChannelManager.NewChannelWindow.superclass.initComponent.apply(this, arguments);

            this.on('beforeshow', this.resetWizard, this);

            Ext.getCmp('card-container').add(new Hippo.ChannelManager.BlueprintListPanel({
                id: 'blueprints-panel',
                store: this.blueprintStore,
                resources: this.resources
            }));

            Ext.getCmp('card-container').add(new Hippo.ChannelManager.ChannelFormPanel({
                id: 'channel-form-panel',
                store: this.channelStore,
                resources: this.resources
            }));

        },

        showBlueprintChoice: function() {
            this.setTitle(this.resources['new-channel-blueprint']);
            Ext.getCmp('card-container').layout.setActiveItem('blueprints-panel');
            Ext.getCmp('nextButton').setText(this.resources['new-channel-next']);
            Ext.getCmp('previousButton').hide();
        },

        showChannelForm: function() {
            this.setTitle(this.resources['new-channel-properties']);
            Ext.getCmp('card-container').layout.setActiveItem('channel-form-panel');
            Ext.getCmp('nextButton').setText(this.resources['new-channel-create']);
            Ext.getCmp('previousButton').show();
        },

        resetWizard: function() {
            this.showBlueprintChoice();
        },

        processPreviousStep: function() {
            if (Ext.getCmp('card-container').layout.activeItem.id === 'channel-form-panel') {
                this.showBlueprintChoice();
            }
        },

        processNextStep: function() {
            if (Ext.getCmp('card-container').layout.activeItem.id === 'blueprints-panel') {
                this.showChannelForm();
            } else {
                this.submitChannelForm();
            }
        },

        submitChannelForm: function() {
            var nextButton = Ext.getCmp('nextButton');
            nextButton.disable();
            Ext.getCmp('channel-form-panel').submitForm().when(function() {
                nextButton.enable();
            }).otherwise(function() {
                nextButton.enable();
            });
        }

    });

}());
