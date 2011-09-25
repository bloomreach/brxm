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

        this.pageContainer = new Hippo.ChannelManager.TemplateComposer.PageContainer(config);

        this.creatingPreviewHstConfig = false;
        this.overlayBuild = false;

        this.initUI(config);

        Hippo.ChannelManager.TemplateComposer.PageEditor.superclass.constructor.call(this, config);

        this.relayEvents(this.pageContainer, [
            'beforeMountIdChange',
            'afterBuildOverlay',
            'beforeIFrameDOMReady',
            'afterIFrameDOMReady',
            'beforeRequestHstMetaData',
            'beforeHstMetaDataResponse',
            'afterHstMetaDataResponse',
            'iFrameInitialized',
            'hasPreviewHstConfigChanged',
            'iFrameException',
            'toolkitLoaded',
            'pageModelStoreLoaded',
            'componentRemoved',
            'pageClick'
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
                    tbar: [
                        {
                            text : config.resources['back-to-channel-manager-button'],
                            id : "channelManager",
                            listeners : {
                                'click' : {
                                    fn : function() {
                                        Ext.getCmp('rootPanel').showChannelManager();
                                    },
                                    scope: this
                                }
                            }
                        },
                        {
                            text: config.resources['preview-button'],
                            iconCls: 'title-button',
                            id: 'pagePreviewButton',
                            toggleGroup : 'composerMode',
                            pressed: config.previewMode,
                            allowDepress: false,
                            width: 150,
                            disabled: true
                        },
                        {
                            text: config.resources['edit-button'],
                            iconCls: 'title-button',
                            id: 'pageComposerButton',
                            enableToggle: true,
                            toggleGroup : 'composerMode',
                            pressed: !config.previewMode,
                            allowDepress: false,
                            width: 150,
                            disabled: true,
                            listeners: {
                                'toggle': {
                                    fn : this.toggleMode,
                                    scope: this
                                }
                            }
                        },
                        {
                            text: config.resources['publish-button'],
                            iconCls: 'title-button',
                            id: 'publishHstConfig',
                            width: 150,
                            disabled: true,
                            listeners: {
                                'click': {
                                    fn : function() {
                                        this.publishHstConfiguration();
                                    },
                                    scope: this
                                }
                            }
                        },
                        {
                            id: 'channelName',
                            xtype: 'tbtext',
                            text: '',
                            style: {
                                marginLeft: '150px'
                            }
                        }
                    ]
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
                        } else {
                            throw e;
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

    initComponent : function() {
        Hippo.ChannelManager.TemplateComposer.PageEditor.superclass.initComponent.call(this);
        // recalculate the ExtJs layout when the YUI layout manager fires a resize event
        this.on('afterrender', function() {
            var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
            YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function() {
                Ext.getCmp('Iframe').setSize(arguments[0].body.w, arguments[0].body.h);
            }, true);

            this.pageContainer.initComposer.call(this.pageContainer);
        }, this, {single: true});

        this.on('beforeIFrameDOMReady', function() {
            console.log('beforeIFrameDOMReady');
            this.overlayBuild = false;
            var hideLoading = function() {
                Hippo.Msg.hide();
                Ext.getCmp('pagePreviewButton').setDisabled(false);
                Ext.getCmp('pageComposerButton').setDisabled(false);
            };
            if (this.pageContainer.previewMode) {
                this.on('beforeIFrameDOMReady', function() {
                    this.removeListener('beforeIFrameDOMReady', hideLoading, this);
                }, this, {single: true});
                this.on('iFrameInitialized', hideLoading, this, {single : true});
            } else {
                this.on('beforeIFrameDOMReady', function() {
                    this.removeListener('afterBuildOverlay', hideLoading, this);
                }, this, {single: true});
                this.on('afterBuildOverlay', hideLoading, this, {single : true});
            }
            Ext.getCmp('pagePreviewButton').setDisabled(true);
            Ext.getCmp('pageComposerButton').setDisabled(true);
            Hippo.Msg.wait(this.resources['loading-message']);
        }, this);

        this.on('beforeChangeModeTo', function(data) {
            console.log('beforeChangeModeTo' + JSON.stringify(data));
            this.creatingPreviewHstConfig = !data.previewMode && !data.hasPreviewHstConfig;
        }, this);

        this.on('afterBuildOverlay', function() {
            console.log('afterBuildOverlay');
            this.overlayBuild = true;
        }, this);

        this.on('beforeInitComposer', function() {
            Hippo.Msg.wait(this.resources['loading-message']);
            this.channelName = null;
            Ext.getCmp('pagePreviewButton').toggle(true, true);
            Ext.getCmp('pageComposerButton').toggle(false, true);
            Ext.getCmp('pagePreviewButton').setDisabled(true);
            Ext.getCmp('pageComposerButton').setDisabled(true);
            Ext.getCmp('publishHstConfig').setDisabled(true);
            if (this.mainWindow) {
                this.mainWindow.hide();
            }
        }, this);

        this.on('afterInitComposer', function() {
            Ext.getCmp('pagePreviewButton').setDisabled(false);
            Ext.getCmp('pageComposerButton').setDisabled(false);
            Hippo.Msg.hide();
        }, this);

        this.on('toggleMode', function(data) {
            console.log('toggleMode '+JSON.stringify(data));
            this.creatingPreviewHstConfig = false;
            Ext.getCmp('pagePreviewButton').setDisabled(data.previewMode);
            Ext.getCmp('pageComposerButton').setDisabled(!data.previewMode);
            Hippo.Msg.wait(this.resources['loading-message']);
        }, this);

        this.on('modeChanged', function(data) {
            console.log('mode changed');
            this.creatingPreviewHstConfig = false;
            if (data.previewMode) {
                if (this.mainWindow) {
                    this.mainWindow.hide();
                }
            } else {
                if (this.mainWindow) {
                    this.mainWindow.show();
                }
            }
            Ext.getCmp('pagePreviewButton').setDisabled(false);
            Ext.getCmp('pageComposerButton').setDisabled(false);
            Hippo.Msg.hide();
        }, this);

        this.on('hasPreviewHstConfigChanged', function(data) {
            console.log('hasPreviewHstConfigChanged ' + data.hasPreviewHstConfig);
            Ext.getCmp('publishHstConfig').setDisabled(!data.hasPreviewHstConfig);
        }, this);

        this.on('beforePublishHstConfiguration', function() {
            Ext.getCmp('publishHstConfig').setDisabled(true);
            Ext.getCmp('pagePreviewButton').setDisabled(true);
            Ext.getCmp('pageComposerButton').setDisabled(true);
        }, this);

        this.on('afterPublishHstConfiguration', function() {
            Ext.getCmp('pagePreviewButton').setDisabled(false);
            Ext.getCmp('pageComposerButton').setDisabled(false);
        }, this);

        this.on('toolkitLoaded', function(mountId, toolkit) {
            if (this.mainWindow) {
                var grid = Ext.getCmp('ToolkitGrid');
                grid.reconfigure(toolkit, grid.getColumnModel());
            } else {
                this.mainWindow = this.createMainWindow(mountId);
            }
            this.mainWindow.show();
        }, this);

        this.on('pageModelStoreLoaded', function(store) {
            if (this.mainWindow) {
                console.log('reconfigure page model grid');
                var grid = Ext.getCmp('PageModelGrid');
                grid.reconfigure(store, grid.getColumnModel());
                this.mainWindow.show();
            }
        }, this);

        this.on('beforeMountIdChange', function(data) {
            if (!this.preview && !data.hasPreviewHstConfig && this.pageContainer.hasPreviewHstConfig != data.hasPreviewHstConfig) {
                // switching mount when edit is active and no preview available on the new mount
                Ext.getCmp('pagePreviewButton').toggle(true);
            }
        }, this);

        this.on('componentRemoved', function(record) {
            var grid = Ext.getCmp('PageModelGrid');
            if (grid.getSelectionModel().getSelected() == record) {
                this.deselect(null, null, record);
            }
        }, this);

        this.on('pageClick', this.handleOnClick, this);
    },

    setChannelName : function(name) {
        if (typeof this.showTitleSwitchTimeout !== 'undefined') {
            window.clearTimeout(this.showTitleSwitchTimeout);
        }
        var oldName = this.channelName;
        this.channelName = name;
        var channelNameText = Ext.getCmp('channelName');
        if (typeof oldName !== 'undefined' && oldName !== null) {
            channelNameText.setText(this.resources['channel-switch-text'].format(oldName, name));
            this.showTitleSwitchTimeout = window.setTimeout(function() {
                channelNameText.setText(name);
            }, 5000);
        } else {
            channelNameText.setText(name);
        }
    },

    publishHstConfiguration : function() {
        this.fireEvent('beforePublishHstConfiguration');
        var self = this;
        Ext.Ajax.request({
            method: 'POST',
            url: this.composerRestMountUrl + this.pageContainer.ids.mountId + './publish?'+this.ignoreRenderHostParameterName+'=true',
            success: function () {
                Ext.getCmp('pagePreviewButton').toggle(true);
                self.on.apply(self, ['afterIFrameDOMReady', function() {
                    this.fireEvent('afterPublishHstConfiguration');
                }, self, {single : true}]);
                self.pageContainer.refreshIframe.call(self.pageContainer, null);
            },
            failure: function(result) {
                var jsonData = Ext.util.JSON.decode(result.responseText);
                Hippo.Msg.alert(self.resources['published-hst-config-failed-message-title'], self.resources['published-hst-config-failed-message']+' '+jsonData.message, function() {
                    self.pageContainer.initComposer.call(self.pageContainer);
                });
            }
        });
    },

    toggleMode: function () {
        this.fireEvent('toggleMode', {previewMode : this.pageContainer.previewMode});

        var func = function() {
            // FIXME: changing internal state of container!
            this.pageContainer.previewMode = !this.pageContainer.previewMode;
            var toggleModeClosure = function(mountId, pageId, hasPreviewHstConfig) {
                console.log('hasPreviewHstConfig:' + hasPreviewHstConfig);
                this.fireEvent('beforeChangeModeTo', {previewMode : this.pageContainer.previewMode, hasPreviewHstConfig : hasPreviewHstConfig});
                if (this.pageContainer.previewMode) {
                    var iFrame = Ext.getCmp('Iframe');
                    if (!this.pageContainer.iframeInitialized) {
                        this.on('iFrameInitialized', function() {
                            iFrame.getFrame().sendMessage({}, 'hideoverlay');
                        }, this, {single: true});
                    } else {
                        iFrame.getFrame().sendMessage({}, 'hideoverlay');
                    }
                    this.fireEvent('modeChanged', {previewMode : this.pageContainer.previewMode});
                } else {
                    if (hasPreviewHstConfig) {
                        var iFrame = Ext.getCmp('Iframe');
                        if (!this.pageContainer.iframeInitialized) {
                            this.on('iFrameInitialized', function() {
                                iFrame.getFrame().sendMessage({}, ('showoverlay'));
                            }, this, {single: true});
                        } else {
                            iFrame.getFrame().sendMessage({}, ('showoverlay'));
                        }

                        if (this.overlayBuild) {
                            this.fireEvent('modeChanged', {previewMode : this.pageContainer.previewMode});
                        } else {
                            this.on('afterBuildOverlay', function() {
                                this.fireEvent('modeChanged', {previewMode : this.pageContainer.previewMode});
                            }, this, { single: true});
                        }
                    } else {
                        // create new preview hst configuration
                        var self = this;
                        Ext.Ajax.request({
                            method: 'POST',
                            url: this.composerRestMountUrl + mountId + './edit?'+this.ignoreRenderHostParameterName+'=true',
                            success: function () {
                                // refresh iframe to get new hst config uuids. previewMode=false will initialize
                                // the editor for editing with the refresh
                                self.on('afterBuildOverlay', function() {
                                    self.fireEvent.apply(self, ['modeChanged', {previewMode : self.pageContainer.previewMode}]);
                                }, self, { single: true});
                                self.pageContainer.refreshIframe.call(self.pageContainer, null);
                            },
                            failure: function(result) {
                                var jsonData = Ext.util.JSON.decode(result.responseText);
                                Hippo.Msg.alert(self.resources['preview-hst-config-creation-failed-title'], self.resources['preview-hst-config-creation-failed'] + ' ' + jsonData.message, function() {
                                    self.pageContainer.initComposer.call(self.pageContainer);
                                });
                            }
                        });
                    }
                }
            };
            if (this.pageContainer.isHstMetaDataLoaded()) {
                toggleModeClosure.apply(this, [this.pageContainer.ids.mountId, this.pageContainer.ids.pageId, this.pageContainer.hasPreviewHstConfig]);
            } else {
                this.on('afterHstMetaDataResponse', function(data) {
                    toggleModeClosure.apply(this, [data.mountId, data.pageId, data.hasPreviewHstConfig]);
                }, this, {single : true});
            }
        }

        if (this.pageContainer.iframeDOMReady) {
            func.call(this);
        } else {
            this.on('afterIFrameDOMReady', func, this, {single : true});
        }

        return true;
    },

    createMainWindow : function(mountId) {
        var window1 = new Hippo.ux.window.FloatingWindow({
            title: this.resources['main-window-title'],
            x:10, y: 35,
            width: 310,
            height: 650,
            initRegion: 'right',
            layout: 'border',
            closable: true,
            constrainHeader: true,
            closeAction: 'hide',
            bodyStyle: 'background-color: #ffffff',
            renderTo: Ext.getCmp('Iframe').getEl(),
            constrain: true,
            items: [
                {
                    region: 'north',
                    split:true,
                    layout: 'accordion',
                    height: 300,
                    items:[
                        {
                            xtype: 'h_base_grid',
                            flex:2,
                            id: 'ToolkitGrid',
                            title: this.resources['toolkit-grid-title'],
                            store: this.pageContainer.stores.toolkit,
                            cm: new Ext.grid.ColumnModel({
                                columns: [
                                    { header: this.resources['toolkit-grid-column-header-name'], dataIndex: 'name', id:'name', viewConfig :{width: 40}}
//                                    { header: "Id", dataIndex: 'id', id:'id', viewConfig :{width: 40}},
//                                    { header: "Path", dataIndex: 'path', id:'path', viewConfig :{width: 120}}
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
                            xtype: 'h_base_grid',
                            flex: 3,
                            id: 'PageModelGrid',
                            title: this.resources['page-model-grid-title'],
                            store: this.pageContainer.stores.pageModel,
                            sm: new Ext.grid.RowSelectionModel({
                                singleSelect: true,
                                listeners: {
                                    rowselect: {
                                        fn: this.select,
                                        scope: this
                                    },
                                    rowdeselect: {
                                        fn: this.deselect,
                                        scope: this
                                    }
                                }
                            }),
                            cm : new Ext.grid.ColumnModel({
                                columns: [
                                    { header: this.resources['page-model-grid-column-header-name'], dataIndex: 'name', id:'name', viewConfig :{width: 120}},
                                    { header: this.resources['page-model-grid-column-header-type'], dataIndex: 'type', id:'type'},
                                    { header: this.resources['page-model-grid-column-header-template'], dataIndex: 'template', id:'template'}
                                ],
                                defaults: {
                                    sortable: false,
                                    menuDisabled: true
                                }
                            }),
                            menuProvider: this
                        }
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

    handleOnClick : function(recordIndex) {
        var sm = Ext.getCmp('PageModelGrid').getSelectionModel();
        if (sm.isSelected(recordIndex)) {
            sm.deselectRow(recordIndex);
        } else {
            sm.selectRow(recordIndex);
        }
    },

    select : function(model, index, record) {
        this.pageContainer.sendFrameMessage({element: record.data.element}, 'select');
        if (record.get('type') === HST.CONTAINERITEM) {
            this.showProperties(record);
        }
    },

    deselect : function(model, index, record) {
        this.pageContainer.sendFrameMessage({element: record.data.element}, 'deselect');
        this.hideProperties();
    },

    showProperties : function(record) {
        Ext.getCmp('componentPropertiesPanel').reload(record.get('id'), record.get('name'), record.get('path'));
    },

    hideProperties : function() {
        Ext.getCmp('componentPropertiesPanel').removeAll();
    }

});
