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
     * @class Hippo.ChannelManager.PropertiesPanel
     * @extends Ext.Window
     */
    Hippo.ChannelManager.ChannelPropertiesWindow = Ext.extend(Ext.Window, {
        constructor: function(config) {
            var self, channelPropertiesContainer;
            self = this;

            this.channelId = null;
            this.resources = config.resources;

            // if the properties were shown once, they need to be hidden again on accessing the perspective
            channelPropertiesContainer = Ext.DomQuery.selectNode("div[class=\"channel-properties\"]");
            if (channelPropertiesContainer) {
                channelPropertiesContainer.setAttribute('class', 'hide-channel-properties');
            }

            this.saveButton = new Ext.Button({
                id: 'channel-properties-save-button',
                text: self.resources['channel-properties-save'],
                listeners: {
                    click: {
                        fn: self.saveChannel,
                        scope: this
                    }
                }
            });

            Ext.apply(config, {
                id: 'channel-properties-window',
                cls: 'channel-properties-window',
                title: self.resources['channel-properties-title'],
                width: 334,
                height: 500,
                closable: true,
                collapsible: false,
                resizable: true,
                constrainHeader: true,
                closeAction: 'hide',
                hidden: true,
                bodyStyle: 'background-color: #ffffff',
                padding: 10,
                buttons: [ this.saveButton ]
            });

            this.addEvents('savechannel');

            Hippo.ChannelManager.ChannelPropertiesWindow.superclass.constructor.call(this, config);
        },

        initComponent: function() {
            this.on('beforeexpand', function() {
                return this.channelId !== null;
            }, this);
            this.on('show', function() {
                Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance.hostToIFrame.publish('enablemouseevents');
            });
            this.on('hide', function() {
                Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance.hostToIFrame.publish('disablemouseevents');
            });
            this.addEvents('selectchannel', 'savechannel');
            Hippo.ChannelManager.ChannelPropertiesWindow.superclass.initComponent.apply(this, arguments);
        },

        show: function(data) {
            this.channelId = data.channelId;
            if (data.channelName) {
                this.setTitle(data.channelName);
            }
            this.channelName = data.channelName;

            if (!Ext.isEmpty(data.channelNodeLockedBy) && data.channelNodeLockedBy !== data.cmsUser) {
                this.saveButton.setDisabled(true);
                this.saveButton.setTooltip(this.resources['locked-by'].format(data.channelNodeLockedBy));
            } else {
                this.saveButton.setDisabled(false);
                this.saveButton.setTooltip('');
            }

            this.render(Hippo.ChannelManager.TemplateComposer.IFramePanel.Instance.getEl());
            this.expand();
            this.fireEvent('selectchannel', data.channelId);
            Hippo.ChannelManager.ChannelPropertiesWindow.superclass.show.apply(this, arguments);
        },

        saveChannel: function() {
            this.fireEvent('savechannel');
        }

    });

    Ext.reg('Hippo.ChannelManager.ChannelPropertiesWindow', Hippo.ChannelManager.ChannelPropertiesWindow);

}());
