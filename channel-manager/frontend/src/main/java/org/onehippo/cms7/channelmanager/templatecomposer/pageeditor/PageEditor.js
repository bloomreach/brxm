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
"use strict";
Ext.namespace('Hippo.ChannelManager.TemplateComposer');

Hippo.ChannelManager.TemplateComposer.PageEditor = Ext.extend(Ext.Panel, {

    constructor : function(config) {
        if (config.debug) {
            Ext.Ajax.timeout = 90000; // this changes the 30 second default to 90 seconds
        }

        this.title = config.title;
        config.header = false;

        this.composerRestMountUrl = config.templateComposerContextPath + config.composerRestMountPath;
        this.pageContainer = new Hippo.ChannelManager.TemplateComposer.PageContainer(config);

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
        Ext.apply(config, { items :
            [
                {
                    id: 'Iframe',
                    xtype: 'iframepanel',
                    // loadMask: true,
                    collapsible: false,
                    disableMessaging: false,
                    tbar: {
                        id: 'pageEditorToolbar',
                        cls: 'channel-manager-toolbar',
                        height: 28,
                        items: [
                        ]
                    }
                },
                {
                    id: 'previousLiveNotification',
                    xtype: 'Hippo.ChannelManager.TemplateComposer.Notification',
                    alignToElementId: 'pageEditorToolbar',
                    message: config.resources['previous-live-msg']
                }
            ]
        });

        if (config.debug) {
            Ext.data.DataProxy.addListener('exception', function(proxy, type, action, options, res, e) {
                if (!res.success && res.message) {
                    console.error(res.message);
                }
                else {
                    if (e) {
                        if(typeof console.error == 'function') {
                           console.error(e);
                        }
                    } else if(res.status) {
                        var json = Ext.util.JSON.decode(res.responseText);
                        var msg = '<br/><b>StatusText:</b> ' + res.statusText + '<br/><b>StatusCode:</b> ' + res.status +
                                '<br/><b>Detailed message:</b> ' + json.message;
                        console.error(json.message);
                    } else {
                        console.group("Exception");
                        console.dir(arguments);
                        console.groupEnd();
                    }
                }
            }, this);

            Ext.data.DataProxy.addListener('write', function(proxy, action, result, res, rs) {
                console.log('Data Proxy Action: ' + action + '<br/>Message: ' + res.message);
            }, this);
        }
    },

    enableUI: function(pageContext) {
        Hippo.Msg.hide();

        var toolbar = Ext.getCmp('pageEditorToolbar');
        toolbar.removeAll();

        // exception occurred during loading: hide everything
        if (pageContext === null) {
            toolbar.doLayout();
            if (this.mainWindow) {
                this.mainWindow.hide();
            }
            return;
        }

        if (!this.pageContainer.previewMode) {
            if (!this.mainWindow) {
                this.mainWindow = this.createMainWindow(pageContext.ids.mountId);
            }

            var toolkitGrid = Ext.getCmp('ToolkitGrid');
            toolkitGrid.reconfigure(pageContext.stores.toolkit, toolkitGrid.getColumnModel());

            var propertiesPanel = Ext.getCmp('componentPropertiesPanel');
            propertiesPanel.clearPanel();

            toolbar.add({
                text: this.initialConfig.resources['close-button'],
                iconCls: 'save-close-channel',
                allowDepress: false,
                width: 120,
                listeners: {
                    click: {
                        fn : this.pageContainer.toggleMode,
                        scope: this.pageContainer
                    }
                }
            },
            '->',
            {
                id: 'channel-properties-window-button',
                text: this.initialConfig.resources['show-channel-properties-button'],
                mode: 'show',
                allowDepress: false,
                width: 120,
                listeners: {
                    click: {
                        fn: function() {
                            var propertiesWindow = Ext.getCmp('channel-properties-window');
                            var button = Ext.getCmp('channel-properties-window-button');
                            if (button.mode === 'show') {
                                propertiesWindow.show({
                                    channelId: this.channelId,
                                    channelName: this.channelName
                                });
                                propertiesWindow.on('hide', function() {
                                    button.mode = 'show';
                                    button.setText(this.initialConfig.resources['show-channel-properties-button']);
                                }, this, {single: true});
                                button.mode = 'hide';
                                button.setText(this.initialConfig.resources['close-channel-properties-button']);
                            } else {
                                propertiesWindow.hide();
                                button.mode = 'show';
                                button.setText(this.initialConfig.resources['show-channel-properties-button']);
                            }
                        },
                        scope: this
                    }
                }
            },
            {
                cls: 'toolbarMenuIcon',
                iconCls: 'channel-gear',
                allowDepress: false,
                menu: {
                    items: {
                        text: 'Edit HST Configuration',
                        listeners: {
                            click: {
                                fn : function() {
                                    this.fireEvent('edit-hst-config',
                                        this.channelId,
                                        this.hstMountPoint
                                    );
                                },
                                scope: this
                            }
                        }
                    }
                }
            });

            Ext.getCmp('previousLiveNotification').hide();
            this.mainWindow.show();
        } else {
            toolbar.add({
                text: this.initialConfig.resources['edit-button'],
                iconCls: 'edit-channel',
                allowDepress: false,
                width: 120,
                listeners: {
                    click: {
                        fn : this.pageContainer.toggleMode,
                        scope: this.pageContainer
                    }
                }
            },
            {
                text: this.initialConfig.resources['publish-button'],
                allowDepress: false,
                width: 120,
                hidden: !this.pageContainer.pageContext.hasPreviewHstConfig,
                listeners: {
                    click: {
                        fn : this.pageContainer.publishHstConfiguration,
                        scope: this.pageContainer
                    }
                }
            });

            if (this.mainWindow) {
                this.mainWindow.hide();
            }
            if (this.pageContainer.pageContext.hasPreviewHstConfig) {
                Ext.getCmp('previousLiveNotification').show();
            } else {
                Ext.getCmp('previousLiveNotification').hide();
            }
        }

        toolbar.doLayout();
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
        Hippo.Msg.wait(this.resources['loading-message']);
    },

    update: function(config) {
        this.browseTo(config.channelId, config.contextPath, config.renderPath);
    },

    initComponent : function() {
        Hippo.ChannelManager.TemplateComposer.PageEditor.superclass.initComponent.call(this);
        // recalculate the ExtJs layout when the YUI layout manager fires a resize event
        this.on('afterrender', function() {
            var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
            YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function() {
                Ext.getCmp('Iframe').setSize(arguments[0].body.w, arguments[0].body.h);
            }, true);
            if (this.channelId) {
                this.browseTo(this.channelId, this.contextPath, this.renderPath);
            }
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

        this.on('selectItem', function(record) {
            if (record.get('type') === HST.CONTAINERITEM) {
                this.showProperties(record);
            }
        }, this);

        this.on('mountChanged', function(data) {
            this.channelStoreFuture.when(function(config) {
                var collection = config.store.query('mountId', data.mountId);
                var channelRecord = collection.first()
                if (typeof this.showTitleSwitchTimeout !== 'undefined') {
                    window.clearTimeout(this.showTitleSwitchTimeout);
                }
                this.setTitle(channelRecord.get('name'));
                this.channelId = channelRecord.get('id');
                this.channelName = channelRecord.get('name');
            }.createDelegate(this));
        }, this);
    },

    createMainWindow : function(mountId) {
        var window1 = new Hippo.ux.window.FloatingWindow({
            title: this.resources['main-window-title'],
            x:10, y: 65,
            width: 310,
            height: 650,
            initRegion: 'right',
            layout: 'border',
            closable: false,
            collapsible: true,
            constrainHeader: true,
            bodyStyle: 'background-color: #ffffff',
            renderTo: Ext.getCmp('Iframe').getEl(),
            constrain: true,
            hidden: true,
            items: [
                {
                    xtype: 'h_base_grid',
                    flex:2,
                    region: 'north',
                    height: 300,
                    id: 'ToolkitGrid',
                    title: this.resources['toolkit-grid-title'],
                    cm: new Ext.grid.ColumnModel({
                        columns: [
                            {
                                header: this.resources['toolkit-grid-column-header-name'],
                                dataIndex: 'name',
                                id:'name',
                                viewConfig : {
                                    width: 40
                                }
                            }
                        ],
                        defaults: {
                            sortable: true,
                            menuDisabled: true
                        }
                    }),
                    plugins: [
                        Hippo.ChannelManager.TemplateComposer.DragDropOne
                    ]
                },
                {
                    id: 'componentPropertiesPanel',
                    xtype:'h_properties_panel',
                    region: 'center',
                    split: true,
                    resources: this.resources,
                    locale: this.locale,
                    composerRestMountUrl: this.composerRestMountUrl,
                    ignoreRenderHostParameterName: this.ignoreRenderHostParameterName,
                    mountId: mountId
                }
            ]
        });
        return window1;
    },

    showProperties : function(record) {
        var componentPropertiesPanel = Ext.getCmp('componentPropertiesPanel');
        componentPropertiesPanel.setItemId(record.get('id'));
        componentPropertiesPanel.setTitle(record.get('name'));
        componentPropertiesPanel.reload();
    },

    refreshIframe: function() {
        this.pageContainer.refreshIframe.call(this.pageContainer);
    },

    initComposer: function() {
        this.pageContainer.initComposer.call(this.pageContainer);
    },

    browseTo: function(channelId, contextPath, renderPath) {
        this.channelId = channelId;
        this.channelStoreFuture.when(function(config) {
            var record = config.store.getById(channelId);

            this.title = record.get('name');
            this.hstMountPoint = record.get('hstMountPoint');

            if (contextPath) {
                this.contextPath = contextPath;
            }
            if (renderPath) {
                this.pageContainer.renderPathInfo = renderPath;
            } else {
                this.pageContainer.renderPathInfo = record.get('subMountPath');
            }

            this.pageContainer.renderHost = record.get('hostname');
            this.pageContainer.previewMode = true;
            this.pageContainer.initComposer.call(this.pageContainer);
        }.createDelegate(this));
    }

});
