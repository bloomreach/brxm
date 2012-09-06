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

Hippo.ChannelManager.ExtWireframe = function(id, config) {
    Hippo.ChannelManager.ExtWireframe.superclass.constructor.apply(this, arguments);
    this.bodyResizeListenerRegistered = false;
};

YAHOO.extend(Hippo.ChannelManager.ExtWireframe, YAHOO.hippo.Wireframe, {

    getDimensions: function () {
        var wireFrame, extCmp, size;
        wireFrame = this;
        extCmp = Ext.getCmp('Hippo.ChannelManager.HstConfigEditor.Instance');

        if (extCmp != null) {
            if (!this.bodyResizeListenerRegistered) {
                this.bodyResizeListenerRegistered = true;
                extCmp.on('bodyresize', function() {
                    wireframe.resize();
                });
            }
            size = extCmp.body.getSize();
            return { w: size.width, h: size.height };
        }
        return { w: 0, h: 0 };
    }

});
