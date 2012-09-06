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

    createBreadcrumbItem: function(card) {
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
            text: card.title,
            scope: this
        });
        item.card = card;
        card.on('titlechange', this._onTitleChange, item);
        
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

    /**
     * listener to be invoked with breadcrumbitem scope
     * Updates the title of the breadcrumb
     */
    _onTitleChange: function(panel, title) {
          this.setText(title);
    },
                              
    // public methods:

    pushItem: function(config) {
        var index, breadcrumbItem, i;
        index = this.breadcrumbStackSize;

        breadcrumbItem = this.createBreadcrumbItem(config.card);
        breadcrumbItem.setDisabled(true);

        // IE ignores all style attributes when a containing element (table in this case) has been disabled
        breadcrumbItem.getEl().dom.disabled = false;

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
                config.click.apply(breadcrumbItem, arguments);
            }
        }, this);

        for (i=0; i<index; i++) {
            breadcrumbItem = this.getBreadcrumbItem(i);
            breadcrumbItem.setDisabled(false);
        }
    },

    popItem: function() {
        var lastBreadcrumbItem, card, newLastBreadcrumbItem;
        if (this.breadcrumbStackSize === 0) {
            return null;
        }

        lastBreadcrumbItem = this.getBreadcrumbItem(this.breadcrumbStackSize - 1);
        
        card = lastBreadcrumbItem.card;
        card.un('titlechange', this._onTitleChange, lastBreadcrumbItem);
        
        this.remove(lastBreadcrumbItem);
        this.breadcrumbStackSize--;
        if (this.breadcrumbStackSize === 1) {
            newLastBreadcrumbItem = this.getBreadcrumbItem(this.breadcrumbStackSize - 1);
            newLastBreadcrumbItem.setDisabled(true);
        }
        return lastBreadcrumbItem;
    },

    getBreadcrumbSize: function() {
        return this.breadcrumbStackSize;
    }
});
Ext.reg('Hippo.ChannelManager.BreadcrumbToolbar', Hippo.ChannelManager.BreadcrumbToolbar);