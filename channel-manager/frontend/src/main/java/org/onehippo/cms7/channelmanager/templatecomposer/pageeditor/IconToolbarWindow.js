/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
Ext.namespace('Hippo.ChannelManager.TemplateComposer');

Hippo.ChannelManager.TemplateComposer.IconToolbarWindow = Ext.extend(Ext.Window, {

    constructor: function(config) {
        Ext.apply(this, {
            cls: 'icon-toolbar-window',
            autoHeight: true,
            plain: false,
            draggable: false,
            hidden: true,
            closable: false,
            hideBorders: true,
            resizable: false,
            border: false,
            shadow: false
        });
        if (!config.items) {
            config.items = [];
        }
        config.items.push({
            xtype: 'h_toolkit_grid',
            height: 65,
            id: 'ToolkitGrid',
            columns: [
                {
                    id       :'id',
                    header   : 'ID',
                    dataIndex: 'id'
                },
                {
                    header   : 'Name',
                    dataIndex: 'name'
                }
            ],
            plugins: [
                Hippo.ChannelManager.TemplateComposer.DragDropOne
            ],
            defaultIconUrl: config.defaultIconUrl
        });

        Hippo.ChannelManager.TemplateComposer.IconToolbarWindow.superclass.constructor.apply(this, arguments);
    },

    initComponent: function() {
        var self = this;
        this.on('afterrender', function() {
            var yuiLayout = this.getEl().findParent("div.yui-layout-unit");
            YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function() {
                // add 5 pixels to make the toolbar as wide as the visible iframe body
                self.setWidth(arguments[0].body.w - Ext.getScrollBarWidth() + 5);
            }, true);
        }, this, {single: true});

        Hippo.ChannelManager.TemplateComposer.IconToolbarWindow.superclass.initComponent.apply(this, arguments);
    },

    show: function() {
        Hippo.ChannelManager.TemplateComposer.IconToolbarWindow.superclass.show.apply(this, arguments);
        this.el.alignTo(Ext.getCmp(this.alignToElementId).getEl(), "tl-bl", [0, 0]);
    }
});

Ext.reg('Hippo.ChannelManager.TemplateComposer.IconToolbarWindow', Hippo.ChannelManager.TemplateComposer.IconToolbarWindow);
