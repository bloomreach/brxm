/*
 * Copyright 2010-2016 Hippo B.V. (http://www.onehippo.com)
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

    Ext.namespace('Hippo.ChannelManager.ChannelEditor');

    Hippo.ChannelManager.ChannelEditor.PlainComponentPropertiesEditor = Ext.extend(Hippo.ChannelManager.ChannelEditor.ComponentPropertiesEditor, {

        constructor: function(config) {
            Hippo.ChannelManager.ChannelEditor.PlainComponentPropertiesEditor.superclass.constructor.call(this, Ext.apply(config, {
                items: [ config.componentPropertiesForm ],
                layout: 'fit'
            }));
        },

        initComponent: function() {
            Hippo.ChannelManager.ChannelEditor.ComponentPropertiesEditor.superclass.initComponent.apply(this, arguments);
            this.componentPropertiesForm.on('propertiesLoaded', this.syncVisibleHeight, this);
        },

        syncVisibleHeight: function() {
            var visibleHeight = this.componentPropertiesForm.getVisibleHeight();
            this.fireEvent('visibleHeightChanged', this, visibleHeight);
        },

        destroy: function() {
            Hippo.ChannelManager.ChannelEditor.ComponentPropertiesEditor.superclass.destroy.apply(this, arguments);
            this.componentPropertiesForm.un('propertiesLoaded', this.syncVisibleHeight, this);
        }

    });

}());
