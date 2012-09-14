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

Hippo.ChannelManager.TemplateComposer.VariantsStore = Ext.extend(Hippo.ChannelManager.TemplateComposer.RestStore, {

    constructor : function(config) {
        this.skipIds = config.skipIds || [];

        var proxy = new Ext.data.HttpProxy({
            api: {
                read: config.composerRestMountUrl + '/' + config.variantsUuid + './variants/?FORCE_CLIENT_HOST=true',
                create: '#',
                update: '#',
                destroy: '#'
            },
            listeners: {
                beforewrite: function(proxy, action, records) {
                    return this.api[action].url !== '#';
                }
            }
        });

        Ext.apply(config, {
            id: 'VariantsStore',
            proxy: proxy,
            prototypeRecord :  [
                {name: 'id' },
                {name: 'name' },
                {name: 'description' }
            ],
            listeners: {
                load: this.filterSkippedIds
            }
        });

        Hippo.ChannelManager.TemplateComposer.VariantsStore.superclass.constructor.call(this, config);
    },

    filterSkippedIds: function(store) {
        store.filter({
            fn: function(record) {
                var recordId = record.get('id');
                return this.skipIds.indexOf(recordId) < 0;
            },
            scope: this
        });
    }

});

Hippo.ChannelManager.TemplateComposer.PageEditor = Ext.extend(Ext.Panel, {

    // height of the toolbar (in pixels)
    TOOLBAR_HEIGHT: 28,
    variantsUuid: null,
    locale: null,
    fullscreen: false,

    constructor : function(config) {
        if (config.debug) {
            Ext.Ajax.timeout = 90000; // this changes the 30 second default to 90 seconds
        }

        this.title = config.title;
        config.header = false;

        this.composerRestMountUrl = config.templateComposerContextPath + config.composerRestMountPath;
        this.variantsUuid = config.variantsUuid;
        this.variantAdderXType = config.variantAdderXType;
        this.propertiesEditorXType = config.propertiesEditorXType;
        this.pageContainer = new Hippo.ChannelManager.TemplateComposer.PageContainer(config);
        this.locale = config.locale;

        this.canUnlockChannels = config.canUnlockChannels;

        this.allVariantsStore = null;
        this.allVariantsStoreFuture = null;
        if (typeof(this.variantsUuid) !== 'undefined' && this.variantsUuid !== null) {
            this.allVariantsStore = new Hippo.ChannelManager.TemplateComposer.VariantsStore({
                composerRestMountUrl: this.composerRestMountUrl,
                variantsUuid: this.variantsUuid
            });
            this.allVariantsStoreFuture = new Hippo.Future(function (success, fail) {
                this.allVariantsStore.on('load', function() {
                    success(this.allVariantsStore);
                }, {single : true});
                this.allVariantsStore.on('exception', fail, {single : true});
                this.allVariantsStore.load();
            }.createDelegate(this));
        } else {
            this.allVariantsStore = new Ext.data.ArrayStore({
                fields : [
                    'id', 'name', 'avatar'
                ]
            });
            this.allVariantsStoreFuture = new Hippo.Future(function (success, fail) {
                this.allVariantsStore.on('load', function() {
                    success(this.allVariantsStore);
                }, {single : true});
                this.allVariantsStore.on('exception', fail, {single : true});
                this.allVariantsStore.loadData([['default', 'Default', 'default']]);
            }.createDelegate(this));
        }

        this.initUI(config);

        Hippo.ChannelManager.TemplateComposer.PageEditor.superclass.constructor.call(this, config);

        this.on('titlechange', function(panel, title) {
            this.title = title;
        });

        this.relayEvents(this.pageContainer, [
            'mountChanged',
            'selectItem',
            'lock',
            'unlock',
            'edit-document',
            'documents'
        ]);
    },

    initUI : function(config) {
        Ext.apply(config, {
            items : [
                {
                    id: 'Iframe',
                    xtype: 'iframepanel',
                    // loadMask: true,
                    collapsible: false,
                    disableMessaging: false,
                    tbar: {
                        id: 'pageEditorToolbar',
                        cls: 'channel-manager-toolbar',
                        height: this.TOOLBAR_HEIGHT,
                        items: [
                        ]
                    }
                },
                {
                    id: 'previousLiveNotification',
                    xtype: 'Hippo.ChannelManager.TemplateComposer.Notification',
                    alignToElementId: 'pageEditorToolbar',
                    message: config.resources['previous-live-msg']
                },
                {
                    id: 'icon-toolbar-window',
                    xtype: 'Hippo.ChannelManager.TemplateComposer.IconToolbarWindow',
                    alignToElementId: 'pageEditorToolbar',
                    resources: config.resources
                }
            ]
        });
    },

    getFullScreenButtonConfig : function(fullscreen) {
        return {
            xtype : 'button',
            text : this.resources[fullscreen ? 'expand-button' : 'collapse-button'],
            iconCls : fullscreen ? 'expand' : 'collapse',
            width: 120,
            listeners : {
                click : {
                    fn : function (button) {
                        this.fullscreen = fullscreen;
                        this.createViewToolbar();
                        this.registerResizeListener();
                        var iFrame = Ext.getCmp('Iframe');
                        iFrame.getFrame().sendMessage({}, fullscreen ? 'fullscreen' : 'partscreen');
                    },
                    scope : this
                }
            }
        };
    },

    createVariantsComboBox : function() {
        var self = this;

        var variantsComboBox = new Ext.form.ComboBox({
            store: this.allVariantsStore,
            displayField: 'name',
            typeAhead: true,
            mode: 'local',
            triggerAction: 'all',
            emptyText: this.resources['variants-combo-box-empty-text'],
            editable: false,
            selectOnFocus:true,
            width:135,
            autoSelect: true,
            disabled: !this.pageContainer.previewMode,
            hidden: (this.allVariantsStore instanceof Ext.data.ArrayStore), // hide when only default is available
            listeners: {
                scope: this,
                select : function(combo, record, index) {
                    Ext.Ajax.request({
                        url: self.composerRestMountUrl+'/cafebabe-cafe-babe-cafe-babecafebabe./setvariant?FORCE_CLIENT_HOST=true',
                        method: 'POST',
                        headers: {
                            'FORCE_CLIENT_HOST': 'true'
                        },
                        params: {
                            'variant': record.get('id')
                        },
                        success : function() {
                            self.refreshIframe.call(self);
                        },
                        failure : function() {
                            console.log('failure set variant');
                            combo.clearValue();
                        }
                    });
                }
            }
        });

        variantsComboBox.on('afterRender', function() {
            variantsComboBox.setValue(this.renderedVariant);
            this.allVariantsStoreFuture.when(function() {
                var variantRecord = this.allVariantsStore.getById(this.renderedVariant);
                if (variantRecord) {
                    variantsComboBox.setValue(variantRecord.get('name'));
                }
            }.createDelegate(this));
        }, this);

        return variantsComboBox;
    },

    clearToolbar : function () {
        var toolbar = Ext.getCmp('pageEditorToolbar');

        toolbar.removeAll();
        toolbar.doLayout();
    },

    createVariantLabel: function () {
        return new Ext.Toolbar.TextItem({
            text : this.resources['variants-combo-box-label'],
            hidden : (this.allVariantsStore instanceof Ext.data.ArrayStore) // hide when only default is available
        });
    },

    createViewToolbar : function () {
        var toolbar = Ext.getCmp('pageEditorToolbar'),
            variantsComboBoxLabel = this.createVariantLabel(),
            variantsComboBox = this.createVariantsComboBox(),
            toolbarButtons;

        toolbar.removeAll();
        if (this.fullscreen) {
            toolbar.add(
                variantsComboBoxLabel,
                variantsComboBox,
                this.getFullScreenButtonConfig(false)
            );
        } else if (this.pageContainer.canEdit) {
            toolbarButtons = this.getToolbarButtons();
            toolbar.add(
                toolbarButtons['edit'],
                toolbarButtons['publish'],
                toolbarButtons['discard'],
                toolbarButtons['unlock'],
                toolbarButtons['label'],
                ' ',
                variantsComboBoxLabel,
                variantsComboBox,
                this.getFullScreenButtonConfig(true)
            );
        }
        if (toolbar.rendered) {
            toolbar.doLayout();
        }
    },

    createEditToolbar : function () {
        var toolbar = Ext.getCmp('pageEditorToolbar'),
            variantsComboBoxLabel = this.createVariantLabel(),
            variantsComboBox = this.createVariantsComboBox(),
            toolboxVisible = Ext.get('icon-toolbar-window').isVisible();

        toolbar.removeAll();
        toolbar.add(
            {
                text : this.initialConfig.resources['close-button'],
                iconCls : 'save-close-channel',
                allowDepress : false,
                width : 120,
                listeners : {
                    click : {
                        fn : this.pageContainer.toggleMode,
                        scope : this.pageContainer
                    }
                }
            }, {
                text : this.initialConfig.resources['discard-button'],
                iconCls : 'discard-channel',
                allowDepress : false,
                width : 120,
                listeners : {
                    click : {
                        fn : this.pageContainer.discardChanges,
                        scope : this.pageContainer
                    }
                }
            }, ' ',
            variantsComboBoxLabel,
            variantsComboBox,
            '->',
            {
                id : 'channel-properties-window-button',
                text : this.initialConfig.resources['show-channel-properties-button'],
                mode : 'show',
                allowDepress : false,
                width : 120,
                listeners : {
                    click : {
                        fn : function () {
                            var propertiesWindow = Ext.getCmp('channel-properties-window');
                            var button = Ext.getCmp('channel-properties-window-button');
                            if (button.mode === 'show') {
                                propertiesWindow.show({
                                    channelId : this.channelId,
                                    channelName : this.channelName
                                });
                                propertiesWindow.on('hide', function () {
                                    button.mode = 'show';
                                    button.setText(this.initialConfig.resources['show-channel-properties-button']);
                                }, this, {single : true});
                                button.mode = 'hide';
                                button.setText(this.initialConfig.resources['close-channel-properties-button']);
                            } else {
                                propertiesWindow.hide();
                                button.mode = 'show';
                                button.setText(this.initialConfig.resources['show-channel-properties-button']);
                            }
                        },
                        scope : this
                    }
                }
            },
            {
                id : 'toolkit-window-button',
                text : (toolboxVisible ? this.initialConfig.resources['close-components-button'] : this.initialConfig.resources['add-components-button']),
                mode : (toolboxVisible ? 'hide' : 'show'),
                allowDepress : false,
                width : 120,
                listeners : {
                    click : {
                        fn : function () {
                            var toolkitWindow = Ext.getCmp('icon-toolbar-window');
                            var button = Ext.getCmp('toolkit-window-button');
                            if (button.mode === 'show') {
                                toolkitWindow.show();
                                button.mode = 'hide';
                                button.setText(this.initialConfig.resources['close-components-button']);
                            } else {
                                toolkitWindow.hide();
                                button.mode = 'show';
                                button.setText(this.initialConfig.resources['add-components-button']);
                            }
                        },
                        scope : this
                    }
                }
            },
            {
                cls : 'toolbarMenuIcon',
                iconCls : 'channel-gear',
                allowDepress : false,
                menu : {
                    items : {
                        text : this.initialConfig.resources['edit-hst-configuration'],
                        listeners : {
                            click : {
                                fn : function () {
                                    this.fireEvent('edit-hst-config', this.channelId, (this.initializeHstConfigEditorWithPreviewContext ? this.hstPreviewMountPoint : this.hstMountPoint));
                                },
                                scope : this
                            }
                        }
                    }
                }
            });
        if (toolbar.rendered) {
            toolbar.doLayout();
        }
    },

    enableUI: function(pageContext) {
        Hippo.Msg.hide();

        // exception occurred during loading: hide everything
        if (pageContext === null) {
            this.clearToolbar();
            if (this.propertiesWindow) {
                this.propertiesWindow.hide();
            }
            return;
        }

        this.renderedVariant = pageContext.renderedVariant;

        var frm = Ext.getCmp('Iframe').getFrame();
        if (!this.pageContainer.previewMode) {
            this.createEditToolbar();

            frm.sendMessage({}, ('showoverlay'));
            frm.sendMessage({}, ('hidelinks'));

            if (this.propertiesWindow) {
                this.propertiesWindow.destroy();
            }
            this.propertiesWindow = this.createPropertiesWindow(pageContext.ids.mountId);
            this.propertiesWindow.hide();

            var toolkitGrid = Ext.getCmp('ToolkitGrid');
            toolkitGrid.reconfigure(pageContext.stores.toolkit, toolkitGrid.getColumnModel());

            Ext.getCmp('previousLiveNotification').hide();
        } else {
            this.createViewToolbar();

            frm.sendMessage({}, ('hideoverlay'));
            if (this.fullscreen) {
                frm.sendMessage({}, ('hidelinks'));
            } else {
                frm.sendMessage({}, ('showlinks'));
            }

            if (this.propertiesWindow) {
                this.propertiesWindow.hide();
            }

            if (!this.fullscreen && this.pageContainer.pageContext.hasPreviewHstConfig) {
                Ext.getCmp('previousLiveNotification').show();
            } else {
                Ext.getCmp('previousLiveNotification').hide();
            }

            Ext.getCmp('icon-toolbar-window').hide();
        }

        Ext.getCmp('pageEditorToolbar').doLayout();
    },

    disableUI: function() {
        var toolbar = Ext.getCmp('pageEditorToolbar');
        toolbar.items.each(function(item) {
            item.disable();
        });

        var channelPropertiesWindow = Ext.getCmp('channel-properties-window');
        if (channelPropertiesWindow) {
            channelPropertiesWindow.hide();
        }
    },

    registerResizeListener: function() {
        var yuiLayout,
                element,
                domNode = this.getEl().dom,
                relayout = false,
                rootPanel = Ext.getCmp('rootPanel'),
                iFrame,
                src;
        if (this.fullscreen) {
            iFrame = Ext.getCmp('Iframe');
            src = iFrame.getFrame().getDocumentURI();

            this.getEl().addClass("channel-manager-fullscreen");
            this.getEl().addClass("channel-manager");
            yuiLayout = this.getEl().findParent("div.yui-layout-unit");
            YAHOO.hippo.LayoutManager.unregisterResizeListener(yuiLayout, this, this.resizeListener);

            iFrame.suspendEvents();
            rootPanel.remove(this, false);
            Ext.getBody().dom.appendChild(domNode);
            iFrame.resumeEvents();

            iFrame.getFrame().setSrc(src);

            element = Ext.getBody();
            this.resizeListener = function() {
                var w = element.getWidth(), h = element.getHeight();
                this.setSize(w, h);

                // Correct the width for the border of the outer panel: 1 pixel left and right, so 2px in total.
                // The height of the yui layout div also includes the space for the toolbar, so subtract that.
                Ext.getCmp('Iframe').setSize(w - 2, h);
            }.createDelegate(this);
            YAHOO.hippo.LayoutManager.registerRootResizeListener(this, this.resizeListener);

            this.resizeListener();
        } else {
            if (this.resizeListener) {
                YAHOO.hippo.LayoutManager.unregisterRootResizeListener(this.resizeListener);

                iFrame = Ext.getCmp('Iframe');
                src = iFrame.getFrame().getDocumentURI();

                iFrame.suspendEvents();
                Ext.getBody().dom.removeChild(domNode);
                rootPanel.insert(1, this);
                iFrame.resumeEvents();

                rootPanel.getLayout().setActiveItem(1);
                rootPanel.doLayout();

                iFrame.getFrame().setSrc(src);

                relayout = true;
            }

            element = this.getEl();
            element.removeClass("channel-manager");
            element.removeClass("channel-manager-fullscreen");
            yuiLayout = element.findParent("div.yui-layout-unit");
            this.resizeListener = function() {
                // Correct the width for the border of the outer panel: 1 pixel left and right, so 2px in total.
                // The height of the yui layout div also includes the space for the toolbar, so subtract that.
                Ext.getCmp('Iframe').setSize(arguments[0].body.w - 2, arguments[0].body.h - this.TOOLBAR_HEIGHT);
            };
            YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, this.resizeListener, true);

            if (relayout) {
                this.resizeListener(YAHOO.hippo.LayoutManager.findLayoutUnit(yuiLayout).getSizes());
            }
        }
    },

    initComponent : function() {
        Hippo.ChannelManager.TemplateComposer.PageEditor.superclass.initComponent.call(this);

        // recalculate the ExtJs layout when the YUI layout manager fires a resize event
        this.on('afterrender', function() {
            this.registerResizeListener();
        }, this, {single: true});

        this.on('lock', function() {
            console.log('lock');
            this.disableUI();
        }, this);

        this.on('unlock', function(pageContext) {
            if (pageContext !== null) {
                Hippo.ChannelManager.TemplateComposer.DragDropOne.setPageContext(pageContext);
            }
            this.enableUI(pageContext);
        }, this);

        this.on('selectItem', function(record, variant, inherited) {
            if (record.get('type') === HST.CONTAINERITEM && inherited !== true) {
                this.showProperties(record, variant);
            }
        }, this);

        this.on('mountChanged', function(data) {
            this.channelStoreFuture.when(function(config) {
                var collection = config.store.query('mountId', data.mountId);
                var channelRecord = collection.first();
                if (typeof this.showTitleSwitchTimeout !== 'undefined') {
                    window.clearTimeout(this.showTitleSwitchTimeout);
                }
                this.setTitle(channelRecord.get('name'));
                this.channelId = channelRecord.get('id');
                this.channelName = channelRecord.get('name');
            }.createDelegate(this));
        }, this);

    },

    createPropertiesWindow : function(mountId) {
        var window1 = new Hippo.ux.window.FloatingWindow({
            id: 'componentPropertiesWindow',
            title: this.resources['properties-window-default-title'],
            x:10, y: 120,
            width: 400,
            height: 350,
            layout: 'fit',
            closable: true,
            closeAction: 'hide',
            collapsible: false,
            constrainHeader: true,
            bodyStyle: 'background-color: #ffffff',
            cls: "component-properties",
            renderTo: Ext.getCmp('Iframe').getEl(),
            constrain: true,
            hidden: true,
            listeners: {
                hide: function() {
                    this.pageContainer.deselectComponents();
                },
                scope: this
            },
            items: [
                new Hippo.ChannelManager.TemplateComposer.PropertiesPanel({
                    id: 'componentPropertiesPanel',
                    resources: this.resources,
                    locale: this.locale,
                    composerRestMountUrl: this.composerRestMountUrl,
                    variantsUuid: this.variantsUuid,
                    allVariantsStore : this.allVariantsStore,
                    allVariantsStoreFuture : this.allVariantsStoreFuture,
                    variantAdderXType: this.variantAdderXType,
                    propertiesEditorXType: this.propertiesEditorXType,
                    mountId: mountId,
                    listeners: {
                        cancel: function() {
                            window1.hide();
                        },
                        variantChange: function(id, variantId) {
                            if (id != null) {
                                this.selectVariant(id, variantId);
                            }
                        },
                        scope: this
                    }
                })
            ]
        });
        return window1;
    },

    showProperties : function(record, variant) {
        var componentPropertiesPanel = Ext.getCmp('componentPropertiesPanel');
        componentPropertiesPanel.setComponentId(record.get('id'));
        componentPropertiesPanel.load(variant);
        if (this.propertiesWindow) {
            this.propertiesWindow.setTitle(record.get('name'));
            this.propertiesWindow.show();
        }
    },

    refreshIframe: function() {
        this.pageContainer.refreshIframe.call(this.pageContainer);
    },

    initComposer: function() {
        this.pageContainer.initComposer.call(this.pageContainer);
    },

    browseTo: function(data) {
        this.channelStoreFuture.when(function(config) {
            this.channelId = data.channelId || this.channelId;
            var record = config.store.getById(this.channelId);
            this.title = record.get('name');
            this.hstMountPoint = record.get('hstMountPoint');
            this.hstPreviewMountPoint = record.get('hstPreviewMountPoint');
            this.pageContainer.contextPath = record.get('contextPath') || data.contextPath || this.contextPath;
            this.pageContainer.cmsPreviewPrefix = record.get('cmsPreviewPrefix') || data.cmsPreviewPrefix || this.cmsPreviewPrefix;
            this.pageContainer.renderPathInfo = data.renderPathInfo || this.renderPathInfo || record.get('mountPath');
            this.pageContainer.renderHost = record.get('hostname');
            this.initComposer();
        }.createDelegate(this));
    },

    selectVariant: function(id, variant) {
        this.pageContainer.pageContext.selectVariant(id, variant);
    },

    getToolbarButtons : function() {
        var editButton = new Ext.Toolbar.Button({
            text: this.initialConfig.resources['edit-button'],
            iconCls: 'edit-channel',
            allowDepress: false,
            disabled: this.pageContainer.pageContext.locked,
            width: 120,
            listeners: {
                click: {
                    fn : this.pageContainer.toggleMode,
                    scope: this.pageContainer
                }
            }
        });
        var publishButton = new Ext.Toolbar.Button({
            text: this.initialConfig.resources['publish-button'],
            iconCls: 'publish-channel',
            allowDepress: false,
            disabled: this.pageContainer.pageContext.locked,
            width: 120,
            hidden: !this.pageContainer.pageContext.hasPreviewHstConfig,
            listeners: {
                click: {
                    fn : this.pageContainer.publishHstConfiguration,
                    scope: this.pageContainer
                }
            }
        });
        var discardButton = new Ext.Toolbar.Button({
            text: this.initialConfig.resources['discard-button'],
            iconCls: 'discard-channel',
            allowDespress: false,
            disabled: this.pageContainer.pageContext.locked,
            width: 120,
            hidden: !this.pageContainer.pageContext.hasPreviewHstConfig,
            listeners: {
                click: {
                    fn : this.pageContainer.discardChanges,
                    scope: this.pageContainer
                }
            }
        });
        var unlockButton = new Ext.Toolbar.Button({
            text: this.initialConfig.resources['unlock-button'],
            iconCls: 'remove-lock',
            allowDepress: false,
            hidden: !this.pageContainer.pageContext.locked || !this.canUnlockChannels,
            width: 120,
            listeners: {
                click: {
                    fn : this.pageContainer.unlockMount,
                    scope: this.pageContainer
                }
            }
        });
        var lockLabel = new Ext.Toolbar.TextItem({});
        if (this.pageContainer.pageContext.locked) {
            var lockedOn = new Date(this.pageContainer.pageContext.lockedOn).format(this.initialConfig.resources['mount-locked-format']);
            lockLabel.setText(this.initialConfig.resources['mount-locked-toolbar'].format(this.pageContainer.pageContext.lockedBy, lockedOn));
        }
        return {'edit': editButton, 'publish': publishButton, 'discard': discardButton, 'unlock': unlockButton, 'label': lockLabel};
    }

});
