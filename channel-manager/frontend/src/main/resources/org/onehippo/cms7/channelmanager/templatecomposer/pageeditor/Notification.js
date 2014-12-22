/*
 *  Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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

    Hippo.ChannelManager.TemplateComposer.Notification = Ext.extend(Ext.Window, {

        constructor: function(config) {
            Ext.apply(this, {
                cls: 'x-notification',
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
            Hippo.ChannelManager.TemplateComposer.Notification.superclass.constructor.apply(this, arguments);
        },

        initComponent: function() {
            var self = this;
            this.on('afterrender', function() {
                var yuiLayout = Ext.get(self.getEl().findParent("div.yui-layout-unit"));

                YAHOO.hippo.LayoutManager.registerResizeListener(yuiLayout, this, function() {
                    var yuiLayoutWidth = yuiLayout.getWidth(),
                        iframe = yuiLayout.query('iframe')[0],
                        scrollBarVisible = iframe.scrollHeight < iframe.contentDocument.body.scrollHeight,
                        scrollBarWidth = scrollBarVisible ? Ext.getScrollBarWidth() : 0;

                    self.setWidth(yuiLayoutWidth - 60 - scrollBarWidth);
                }, true);
            }, this, {single: true});

            Hippo.ChannelManager.TemplateComposer.Notification.superclass.initComponent.apply(this, arguments);
        },

        setMessage: function(msg) {
            this.message = msg;
            this.body.update(msg);
        },

        show: function() {
            var self = this,
                yuiLayout = Ext.get(self.getEl().findParent("div.yui-layout-unit")),
                yuiLayoutWidth = yuiLayout.getWidth(),
                iframe = yuiLayout.query('iframe')[0],
                scrollBarVisible = iframe.scrollHeight < iframe.contentDocument.body.scrollHeight,
                scrollBarWidth = scrollBarVisible ? Ext.getScrollBarWidth() : 0;

            Hippo.ChannelManager.TemplateComposer.Notification.superclass.show.apply(self, arguments);
            self.body.update(self.message);
            self.el.alignTo(Ext.getCmp(self.alignToElementId).getEl(), "tl-bl", [30, 3]);
            self.setWidth(yuiLayoutWidth - 60 - scrollBarWidth);
        }

    });

    Ext.reg('Hippo.ChannelManager.TemplateComposer.Notification', Hippo.ChannelManager.TemplateComposer.Notification);

}());
