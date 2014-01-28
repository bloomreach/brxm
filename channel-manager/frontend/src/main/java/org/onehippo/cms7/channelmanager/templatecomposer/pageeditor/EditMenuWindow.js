/*
 *  Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager.TemplateComposer');

    Hippo.ChannelManager.TemplateComposer.EditMenuWindow = Ext.extend(Ext.Window, {

        constructor: function(config) {
            this.iframePanelId = Ext.id();

            var closeButton = new Ext.Toolbar.Button({
                text: 'Close',
                listeners: {
                    click: {
                        fn: this.close,
                        scope: this
                    }
                }
            });

            Ext.apply(config, {
                title: config.resources['edit-menu'],
                width: 860,
                height: 555,
                modal: true,
                layout: 'fit',
                items: [
                    {
                        xtype: 'Hippo.ChannelManager.TemplateComposer.IFramePanel',
                        id: this.iframePanelId,
                        url: Ext.urlAppend('./angular/index.html', 'parentExtIFramePanelId=' + this.iframePanelId),
                        pageManagementConfig: {
                            apiUrlPrefix: config.composerRestMountUrl,
                            locale: config.locale,
                            menuId: config.menuId
                        }
                    }
                ],
                buttons: [ closeButton ],
                buttonAlign: 'right',
                listeners: {
                    'afterrender': function(self) {
                        var iframePanel = Ext.getCmp(self.iframePanelId);
                        iframePanel.iframeToHost.subscribe('close', self.close, self);
                    }
                }
            });

            Hippo.ChannelManager.TemplateComposer.EditMenuWindow.superclass.constructor.call(this, config);
        },

        initComponent: function() {
            Hippo.ChannelManager.TemplateComposer.EditMenuWindow.superclass.initComponent.apply(this, arguments);
        }

    });

    Ext.reg('Hippo.ChannelManager.TemplateComposer.EditMenuWindow', Hippo.ChannelManager.TemplateComposer.EditMenuWindow);

}());
