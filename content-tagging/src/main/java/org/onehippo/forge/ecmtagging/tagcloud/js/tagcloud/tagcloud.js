/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
/**
 * @description
 * <p>
 * TagCloudHelper widget
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, layoutmanager, hippoajax, hippowidget
 * @module tagcloud
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.TagCloudWidget) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.TagCloudWidget = function(id, config) {
            YAHOO.hippo.TagCloudWidget.superclass.constructor.apply(this, arguments);

            this.data = null;
        };

        YAHOO.extend(YAHOO.hippo.TagCloudWidget, YAHOO.hippo.Widget, {

            calculateWidthAndHeight : function(sizes) {
                if(this.data == null) {
                    this.data = {
                        w:10, h:10
                    };
                    var el = Dom.get(this.id);
                    var unit = YAHOO.hippo.LayoutManager.findLayoutUnit(el);
                    var sections = Dom.getElementsByClassName('hippo-accordion-unit', 'div', unit.body);
                    this.data.h += sections.length * 25;
                }
                return {width: sizes.wrap.w-this.data.w, height: sizes.wrap.h-this.data.h};
            },

            render : function() {
                YAHOO.hippo.TagCloudWidget.superclass.render.apply(this, arguments);
                Dom.setStyle(this.id, 'overflow', 'auto');
            }

        });
    })();

    YAHOO.register("TagCloudWidget", YAHOO.hippo.TagCloudWidget, {
        version: "2.8.1", build: "19"
    });
}
