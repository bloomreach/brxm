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

    /**
     * Window that shows a single IFramePanel. The ID of the enclosed IFramePanel is passed to the iframe
     * as a query parameter 'parentExtIFramePanelId'.
     *
     * The 'close' icon of the window triggers a handshake with the iframe. The host sends a 'close-request'
     * message to the iframe, to which the iframe should respond with either a 'close-reply-ok' or
     * 'close-reply-not-ok' message. The former will close the window, the latter will cancel the close.
     *
     * @type {*}
     */
    Hippo.ChannelManager.TemplateComposer.IFrameWindow = Ext.extend(Ext.Window, {

        constructor: function(config) {
            var isClosing = false;

            this.iframePanelId = Ext.id();

            Ext.apply(config, {
                layout: 'fit',
                items: [
                    {
                        xtype: 'Hippo.ChannelManager.TemplateComposer.IFramePanel',
                        id: this.iframePanelId,
                        url: Ext.urlAppend(config.iframeUrl, 'parentExtIFramePanelId=' + this.iframePanelId),
                        iframeConfig: config.iframeConfig
                    }
                ],
                listeners: {
                    'afterrender': function(self) {
                        var messageBus = self.getIFramePanel().iframeToHost;
                        messageBus.subscribe('close-reply-ok', function() {
                            isClosing = true;
                            self.close();
                        });
                        messageBus.subscribe('close-reply-not-ok', function() {
                            isClosing = false;
                        });
                        messageBus.subscribe('browseTo', function (pathInfo) {
                            Hippo.ChannelManager.TemplateComposer.Instance.browseTo({ renderPathInfo: pathInfo });
                            self.close();
                        });
                    },
                    'beforeclose': function(self) {
                        if (!isClosing) {
                            self.getIFramePanel().hostToIFrame.publish('close-request');
                            return false;
                        }
                    }
                }
            });

            Hippo.ChannelManager.TemplateComposer.IFrameWindow.superclass.constructor.call(this, config);
        },

        getIFramePanel: function() {
            return Ext.getCmp(this.iframePanelId);
        }

    });

    Ext.reg('Hippo.ChannelManager.TemplateComposer.IFrameWindow', Hippo.ChannelManager.TemplateComposer.IFrameWindow);

}());
