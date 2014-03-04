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

        isClosing: false,

        constructor: function(config) {
            this.iframePanelId = Ext.id();

            Ext.apply(config, {
                title: config.resources['edit-menu'],
                width: 860,
                minWidth: 790,
                height: 517,
                modal: true,
                resizeHandles: 'e w',
                layout: 'fit',
                items: [
                    {
                        xtype: 'Hippo.ChannelManager.TemplateComposer.IFramePanel',
                        id: this.iframePanelId,
                        url: Ext.urlAppend('./angular/menu-manager/index.html', 'parentExtIFramePanelId=' + this.iframePanelId),
                        iframeConfig: {
                            apiUrlPrefix: config.composerRestMountUrl,
                            debug: config.debug,
                            locale: config.locale,
                            menuId: config.menuId
                        }
                    }
                ],
                listeners: {
                    'afterrender': function(self) {
                        self.getIFramePanel().iframeToHost.subscribe('close-reply-ok', function() {
                            self.isClosing = true;
                            self.close();
                        });
                        self.getIFramePanel().iframeToHost.subscribe('close-reply-not-ok', function() {
                            self.isClosing = false;
                        });
                    },
                    'beforeclose': function(self) {
                        if (!self.isClosing) {
                            self.getIFramePanel().hostToIFrame.publish('close-request');
                            return false;
                        }
                    }
                }
            });

            Hippo.ChannelManager.TemplateComposer.EditMenuWindow.superclass.constructor.call(this, config);
        },

        getIFramePanel: function() {
            return Ext.getCmp(this.iframePanelId);
        }

    });

    Ext.reg('Hippo.ChannelManager.TemplateComposer.EditMenuWindow', Hippo.ChannelManager.TemplateComposer.EditMenuWindow);

}());
