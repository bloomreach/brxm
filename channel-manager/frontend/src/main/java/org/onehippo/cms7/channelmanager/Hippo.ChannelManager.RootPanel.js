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
            },

            openChannelWizard:function() {
                alert("opening new channel wizard")
            }
        });

Ext.reg('Hippo.ChannelManager.RootPanel', Hippo.ChannelManager.RootPanel);
