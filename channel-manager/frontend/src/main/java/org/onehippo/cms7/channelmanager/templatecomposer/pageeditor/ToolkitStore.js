/*
 *  Copyright 2011 Hippo.
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

    Hippo.ChannelManager.TemplateComposer.ToolkitStore = Ext.extend(Hippo.ChannelManager.TemplateComposer.RestStore, {

        constructor: function(config) {
            var proxy, cfg;
            proxy = new Ext.data.HttpProxy({
                api: {
                    read: config.composerRestMountUrl + '/' + config.mountId + './toolkit?FORCE_CLIENT_HOST=true', create: '#', update: '#', destroy: '#'
                }
            });

            cfg = {
                id: 'ToolkitStore',
                proxy: proxy,
                prototypeRecord: [
                    {name: 'id', mapping: 'id'},
                    {name: 'name', mapping: 'name'},
                    {name: 'label', mapping: 'label'},
                    {name: 'iconURL', mapping: 'iconURL'},
                    {name: 'componentClassName', mapping: 'componentClassName'},
                    {name: 'template', mapping: 'template'},
                    {name: 'xtype', mapping: 'xtype'}
                ]
            };

            Ext.apply(config, cfg);

            Hippo.ChannelManager.TemplateComposer.ToolkitStore.superclass.constructor.call(this, config);
        }
    });

}());