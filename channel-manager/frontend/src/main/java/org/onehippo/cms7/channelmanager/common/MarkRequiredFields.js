/*
 *  Copyright 2012 Hippo.
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

    Ext.namespace('Hippo.ChannelManager');

    Hippo.ChannelManager.MarkRequiredFields = Ext.extend(Object, {

        constructor: function(config) {
            config = config || {};
            Ext.apply(this, config);
        },

        init: function(form) {
            Ext.each(form.find(), function(item) {
                this.adjustLabelSeparator(form, item);
            }, this);
            form.on('add', function(form, component) {
                this.adjustLabelSeparator(form, component);
            }, this);
        },

        adjustLabelSeparator: function(form, item) {
            if (item.allowBlank === false) {
                var separator;

                if (Ext.isDefined(item.labelSeparator)) {
                    separator = item.labelSeparator;
                } else if (Ext.isDefined(form.labelSeparator)) {
                    separator = form.labelSeparator;
                } else if (Ext.isDefined(form.defaults.labelSeparator)) {
                    separator = form.defaults.labelSeparator;
                } else {
                    separator = ':';
                }

                item.labelSeparator = ' <span class="req">*</span>' + separator;
            }
        }

    });
    Ext.preg('Hippo.ChannelManager.MarkRequiredFields', Hippo.ChannelManager.MarkRequiredFields);

}());