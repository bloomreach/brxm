/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

    Hippo.ChannelManager.TemplateComposer.ToolkitGridPanel = Ext.extend(Ext.grid.GridPanel, {

        constructor: function(config) {
            Hippo.ChannelManager.TemplateComposer.ToolkitGridPanel.superclass.constructor.call(this, config);
            this.defaultIconUrl = config.defaultIconUrl;
        },

        getView: function() {
            if (!this.view) {
                this.view = new Hippo.ChannelManager.TemplateComposer.IconGridView({
                    defaultIconUrl: this.defaultIconUrl,
                    grid: this
                });
            }

            return this.view;
        }

    });
    Ext.reg('h_toolkit_grid', Hippo.ChannelManager.TemplateComposer.ToolkitGridPanel);

}());
