/**
 * Copyright 2011-2014 Hippo B.V. (http://www.onehippo.com)
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

    Hippo.ChannelManager.ExtWireframe = function(id, config) {
        Hippo.ChannelManager.ExtWireframe.superclass.constructor.apply(this, arguments);
        this.bodyResizeListenerRegistered = false;
    };

    YAHOO.extend(Hippo.ChannelManager.ExtWireframe, YAHOO.hippo.Wireframe, {

        getDimensions: function() {
            var extCmp, size, id;

            id = this.id;
            extCmp = Ext.getCmp('Hippo.ChannelManager.HstConfigEditor.Instance');

            if (extCmp !== undefined) {
                if (!this.bodyResizeListenerRegistered) {
                    this.bodyResizeListenerRegistered = true;
                    extCmp.on('bodyresize', function() {
                        var wireframe = YAHOO.hippo.LayoutManager.getWireframe(id);
                        if (wireframe !== null) {
                            wireframe.resize();
                        }
                    });
                }
                size = extCmp.body.getSize();
                return { w: size.width, h: size.height };
            }
            return { w: 0, h: 0 };
        }

    });

}());
