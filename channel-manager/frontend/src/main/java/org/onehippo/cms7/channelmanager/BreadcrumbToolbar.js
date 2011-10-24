"use strict";

Ext.namespace('Hippo.ChannelManager');

/** TODO fix Ext bug
 * The implementation is using a temporary hack to prevent ext removing the complete toolbar
 * with hiding / removing toolbar items.
 * Instead of using default ext implementation to handle the toolbar items we just set the visibility of the
 * items with pure dom manipulation.
*/
Hippo.ChannelManager.BreadcrumbToolbar = Ext.extend(Ext.Toolbar, {

    constructor: function(config) {
        this.breadcrumbStackSize = 0;
        this.breadcrumbIconUrl = config.breadcrumbIconUrl;

        if (typeof config.items === 'undefined') {
            config.items = [];
        }

        Hippo.ChannelManager.BreadcrumbToolbar.superclass.constructor.call(this, config);
    },

    createBreadcrumbItem: function(text) {
        var item = this.add({
            id: 'breadcrumb-item'+this.breadcrumbStackSize,
            cls: 'breadcrumb-item',
            icon: this.breadcrumbIconUrl,
            margins: {
                top:0,
                right: 10,
                bottom: 0,
                left: 0
            },
            disabled: true,
            text: text,
            scope: this
        });
        this.doLayout();
        this.breadcrumbStackSize++;
        return item;
    },

    getBreadcrumbItem: function(index) {
        if (index >= this.breadcrumbStackSize) {
            return false;
        }
        return this.getComponent('breadcrumb-item'+index);
    },

    // public methods:

    pushItem: function(config) {
        var index = this.breadcrumbStackSize;

        var breadcrumbItem = this.createBreadcrumbItem(config.text);
        breadcrumbItem.on('click', function() {
            if (index === this.breadcrumbStackSize) {
                return;
            }
            while (index+1 < this.breadcrumbStackSize) {
                this.popItem();
            }
            if (config.scope) {
                config.click.apply(config.scope, arguments);
            } else {
                config.click.apply(config, arguments);
            }
        }, this);

        for (var i=0; i<index; i++) {
            breadcrumbItem = this.getBreadcrumbItem(i);
            breadcrumbItem.setDisabled(false);
        }
    },

    popItem: function() {
        if (this.breadcrumbStackSize === 0) {
            return null;
        }

        var lastBreadcrumbItem = this.getBreadcrumbItem(this.breadcrumbStackSize - 1);
        this.remove(lastBreadcrumbItem);
        this.breadcrumbStackSize--;
        if (this.breadcrumbStackSize === 1) {
            var newLastBreadcrumbItem = this.getBreadcrumbItem(this.breadcrumbStackSize - 1);
            newLastBreadcrumbItem.setDisabled(true);
        }
        return lastBreadcrumbItem;
    }

});
Ext.reg('Hippo.ChannelManager.BreadcrumbToolbar', Hippo.ChannelManager.RootPanel);