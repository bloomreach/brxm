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

Ext.namespace('Hippo.ChannelManager.HstConfigEditor');

/**
 * @class Hippo.ChannelManager.HstConfigEditor
 * @extends Ext.Panel
 */
Hippo.ChannelManager.HstConfigEditor.Container = Ext.extend(Ext.Panel, {
    constructor: function(config) {
        var self = this;

        Ext.apply(config, {
            tbar: [
                {
                    text : '< Channel summary',
                    id : 'hstConfigEditor',
                    listeners : {
                        'click' : {
                            fn : function() {
                                Ext.getCmp('rootPanel').layout.setActiveItem(0);
                            },
                            scope: this
                        }
                    }
                }
            ]
        });

        Hippo.ChannelManager.HstConfigEditor.Container.superclass.constructor.call(this, config);
    }
});

Ext.reg('Hippo.ChannelManager.HstConfigEditor.Container', Hippo.ChannelManager.HstConfigEditor.Container);
