/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function() {
    "use strict";

    Ext.namespace('Hippo');

    Hippo.ExtWidgetRegistry = Ext.extend(Ext.util.Observable, {

        configs: [],

        constructor: function(config) {
            Hippo.ExtWidgetRegistry.superclass.constructor.call(this, config);
        },

        /**
         * Registers a new Ext widget. The xtype and class of the widget will also be registered with Ext, so there is no
         * need to explicitly call Ext.reg(xtype, cls).
         *
         * @param config either an object with the initial configuration of the widget, or a String with the xtype of the widget.
         * A configuration object must contain the property 'xtype' with the xtype of the widget. Providing an String
         * parameter 'somestring' is a shorthand for providing a configuration object { xtype: 'somestring' }.
         * @param cls the Ext class of the widget
         */
        register: function(config, cls) {
            var widgetConfig = this.createWidgetConfig(config);
            if (!Ext.isDefined(cls)) {
                throw new Error("Mandatory argument 'cls' is missing for xtype '" + config.xtype + "'");
            }
            Ext.reg(widgetConfig.xtype, cls);
            this.configs[widgetConfig.xtype] = widgetConfig;
        },

        createWidgetConfig: function(config) {
            if (Ext.isString(config)) {
                return {
                    xtype: config
                };
            }
            if (!Ext.isObject(config)) {
                throw new Error("Ext widget config must be an configuration Object or an 'xtype' String, but is of type '" + (typeof config) + "'");
            }
            if (!Ext.isDefined(config.xtype)) {
                throw new Error("Ext widget config object does not contain mandatory property 'xtype'");
            }
            return config;
        },

        /**
         * @param xtype an Ext widget xtype
         *
         * @return true if the registry contains a widget with the given xtype, false otherwise.
         */
        contains: function(xtype) {
            return Ext.isDefined(this.configs[xtype]);
        },

        /**
         * @param xtype the xtype of a registered Ext widget
         *
         * @return the configuration object of the registered Ext widget with the given xtype, or undefined if no widget
         * with the given xtype exists.
         */
        getConfig: function(xtype) {
            return this.configs[xtype];
        },

        /**
         * Instantiates a registered Ext widget.
         *
         * @param xtype the xtype of a registered Ext widget
         * @param config additional constructor configuration to apply. These configuration properties will override any
         * registered configuration properties.
         *
         * @return a new Ext widget instantiation
         */
        create: function(xtype, config) {
            if (!this.contains(xtype)) {
                throw new Error("Unknown Ext widget xtype: '" + xtype + "'");
            }

            var createConfig = Ext.apply({}, this.getConfig(xtype));
            createConfig = Ext.apply(createConfig, config);

            try {
                return Ext.create(createConfig);
            } catch (exception) {
                console.error("Error while creating Ext widget of xtype '" + xtype + "'", exception, createConfig);
                throw new Error("Cannot create Ext widget of xtype '" + xtype + "'");
            }
        }

    });

    if (!Ext.isDefined(Hippo.ExtWidgets)) {
        Hippo.ExtWidgets = new Hippo.ExtWidgetRegistry();
    }
}());
