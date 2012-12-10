/**
 * Copyright 2011 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
(function() {
    "use strict";

    Ext.namespace('Hippo.ChannelManager');

    Hippo.ChannelManager.ExtStoreFuture = Ext.extend(Hippo.Future, {

        constructor: function(config) {
            Hippo.ChannelManager.ExtStoreFuture.superclass.constructor.call(this, function(success, fail) {
                config.store.on('load', function() {
                    success({
                        store: config.store
                    });
                }, this, {single: true});
                config.store.on('exception', function() {
                    fail();
                }, this, {single: true});
                config.store.load();
            }.createDelegate(this));
        }

    });

    Ext.reg('Hippo.ChannelManager.ExtStoreFuture', Hippo.ChannelManager.ExtStoreFuture);

}());