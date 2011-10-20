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
        this.breadcrumbStack = [];
        this.availableItems = 0;
        this.breadcrumbIconUrl = config.breadcrumbIconUrl;

        if (typeof config.items === 'undefined') {
            config.items = [];
        }

        Hippo.ChannelManager.BreadcrumbToolbar.superclass.constructor.call(this, config);
    },

    initComponent: function() {
        Hippo.ChannelManager.BreadcrumbToolbar.superclass.initComponent.call(this);
    },

    createBreadcrumbItem: function(visible) {
        console.log('breadcrumbIconUrl: '+this.breadcrumbIconUrl);
        this.add({
            id: 'breadcrumb-item'+this.availableItems,
            cls: 'breadcrumb-item',
            icon: this.breadcrumbIconUrl,
            margins: {
                top:0,
                right: 10,
                bottom: 0,
                left: 0
            },
            scope: this
        });
        this.doLayout();
        this.availableItems++;
    },

    getBreadcrumbItem: function(index) {
        if (index > this.availableItems) {
            return false;
        }
        console.log('getBreadcrumbItem'+index);
        return this.getComponent('breadcrumb-item'+index);
    },

    showBreadcrumbItem: function(index) {
        if (index > this.availableItems) {
            return false;
        }
        document.getElementById('breadcrumb-item'+index).style.visibility = 'visible';
        return true;
    },

    hideBreadcrumbItem: function(index) {
        if (index > this.availableItems) {
            return false;
        }
        document.getElementById('breadcrumb-item'+index).style.visibility = 'hidden';
        return true;
    },

    // public used methods

    pushItem: function(config) {
        var index = this.breadcrumbStack.length;
        if (index + 1 > this.availableItems) {
            this.createBreadcrumbItem();
        }

        this.breadcrumbStack.push(config);
        var breadcrumbItem = this.getBreadcrumbItem(index);
        breadcrumbItem.setDisabled(true);
        breadcrumbItem.setText(config.text);
        breadcrumbItem.purgeListeners();
        breadcrumbItem.on('click', function() {
            if (index === this.breadcrumbStack.length) {
                return;
            }
            while (index+1 < this.breadcrumbStack.length) {
                this.popItem();
            }
            if (config.scope) {
                config.click.apply(config.scope, arguments);
            } else {
                config.click.apply(config, arguments);
            }
        }, this);
        if (this.rendered) {
            this.showBreadcrumbItem(index);
        }

        for (var i=0; i<index; i++) {
            breadcrumbItem = this.getBreadcrumbItem(i);
            breadcrumbItem.setDisabled(false);
        }
    },

    popItem: function() {
        if (this.breadcrumbStack.length === 0) {
            return null;
        }
        var config = this.breadcrumbStack.pop();
        this.hideBreadcrumbItem(this.breadcrumbStack.length);
        if (this.breadcrumbStack.length === 1) {
            var lastBreadcrumbItem = this.getBreadcrumbItem(this.breadcrumbStack.length - 1);
            lastBreadcrumbItem.setDisabled(true);
        }
        return config;
    },

    clearBreadcrumb: function() {
        for (var i=0, len=this.breadcrumbStack.length; i<len; i++) {
            this.hideBreadcrumbItem(i);
        }
    },

    getSize: function() {
        return this.breadcrumbStack.length;
    }

});
Ext.reg('Hippo.ChannelManager.BreadcrumbToolbar', Hippo.ChannelManager.RootPanel);