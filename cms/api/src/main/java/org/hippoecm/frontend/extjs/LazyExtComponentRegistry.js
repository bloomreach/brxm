/**
 * Copyright 2011-2012 Hippo
 *
 */
"use strict";

Ext.namespace('Hippo');

Hippo.LazyExtComponentRegistry = Ext.extend(Ext.util.Observable, {

    configs: [],

    constructor: function(config) {
        Hippo.LazyExtComponentRegistry.superclass.constructor.call(this, config);
    },

    register: function(config) {
        if (!Ext.isDefined(config.xtype)) {
            throw Ext.Error("Mandatory property 'xtype' missing");
        }
        this.configs[config.xtype] = config;
    },

    contains: function(xtype) {
        return Ext.isDefined(this.configs[xtype]);
    },

    getConfig: function(xtype) {
        return this.configs[xtype];
    }

});

if (!Ext.isDefined(Hippo.LazyExtComponents)) {
    Hippo.LazyExtComponents = new Hippo.LazyExtComponentRegistry();
}
