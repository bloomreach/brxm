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
 * Provides a singleton tables helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, layoutmanager, hippoajax, hippowidget
 * @module tables
 * @beta
 */

(function($) {
    "use strict";

    YAHOO.namespace('hippo');

    if (!YAHOO.hippo.DataTable) {
        (function() {

            YAHOO.hippo.DataTable = function(id, config) {
                YAHOO.hippo.DataTable.superclass.constructor.apply(this, arguments);
            };

            YAHOO.extend(YAHOO.hippo.DataTable, YAHOO.hippo.Widget, {

                resize: function(sizes, preserveScroll) {
                    var table = $('#' + this.id);
                    this._resize(table, sizes, preserveScroll);
                },

                update: function() {
                    var table = $('#' + this.id),
                        layoutUnit = YAHOO.hippo.LayoutManager.findLayoutUnit(table[0]);
                    this._resize(table, layoutUnit.getSizes(), true);
                },

                _resize: function(table, sizes, preserveScroll) {
                    var theadRow = table.find('thead > tr'),
                        tbody = table.children('tbody'),
                        tfootRow = table.find('tfoot > tr');

                    tbody.innerWidth(sizes.wrap.w);
                    tbody.height(sizes.wrap.h - theadRow.height() - tfootRow.height());

                    theadRow.width(tbody[0].scrollWidth);
                }
            });
        }());

        YAHOO.register("HippoDataTable", YAHOO.hippo.DataTable, {
            version: "2.8.1", build: "20"
        });
    }
}(jQuery));
