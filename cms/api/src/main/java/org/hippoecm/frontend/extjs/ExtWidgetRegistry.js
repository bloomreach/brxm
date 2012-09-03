/**
 * Copyright 2011-2012 Hippo
 *
 */
"use strict";

Ext.namespace('Hippo');

Hippo.ExtWidgetRegistry = Ext.extend(Ext.util.Observable, {

    configs: [],

    constructor: function(config) {
        Hippo.ExtWidgetRegistry.superclass.constructor.call(this, config);
    },

    register: function(config) {
        if (!Ext.isDefined(config.xtype)) {
            throw new Error("Mandatory property 'xtype' is missing");
        }
        this.configs[config.xtype] = config;
    },

    contains: function(xtype) {
        return Ext.isDefined(this.configs[xtype]);
    },

    getConfig: function(xtype) {
        return this.configs[xtype];
    },

    create: function(xtype) {
        if (!this.contains(xtype)) {
            throw new Error("Unknown Ext widget xtype: '" + xtype + "'");
        }
        var config = this.getConfig(xtype);
        try {
            return Ext.create(config);
        } catch (exception) {
            console.error("Error while creating Ext object of xtype '" + xtype + "'", exception, config);
            throw new Error("Cannot create Ext widget of xtype '" + xtype + "'");
        }
    }

});

if (!Ext.isDefined(Hippo.ExtWidgets)) {
    Hippo.ExtWidgets = new Hippo.ExtWidgetRegistry();
}
