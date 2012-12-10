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
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager.HstConfigEditor');

    /**
     * @class Hippo.ChannelManager.HstConfigEditor
     * @extends Ext.Panel
     */
    Hippo.ChannelManager.HstConfigEditor.Container = Ext.extend(Ext.Panel, {
        constructor: function(config) {
            this.resources = config.resources;

            this.title = config.title;
            config.header = false;

            Hippo.ChannelManager.HstConfigEditor.Container.superclass.constructor.call(this, config);


            this.on('titlechange', function(panel, title) {
                this.title = title;
            });
        },

        initEditor: function() {
            Ext.getCmp('rootPanel').showConfigEditor();
            document.getElementById('Hippo.ChannelManager.HstConfigEditor.Instance').className = 'x-panel';
        }

    });

    Ext.reg('Hippo.ChannelManager.HstConfigEditor.Container', Hippo.ChannelManager.HstConfigEditor.Container);

}());