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

YAHOO.namespace('hippo');

if (!YAHOO.hippo.DataTable) {
    (function() {
        var Dom = YAHOO.util.Dom,
            ie8 = YAHOO.env.ua.ie === 8,
            thin = ie8 ? 1 : 2, medium = ie8 ? 3 : 4, thick = ie8 ? 5 : 6;

        function getPixels(el, attr) {
            var style, pixels;
            style = Dom.getStyle(el, attr);
            pixels = 0;
            if (style && style !== 'auto') {
                switch (style) {
                    case 'thin': return thin;
                    case 'medium': return medium;
                    case 'thick': return thick;
                }
                pixels = parseInt(style, 10);
            }
            if (isNaN(pixels)) {
                pixels = 0;
            }
            return pixels;
        }

        function getRegion(el) {
            return {
                width: getPixels(el, 'width') +
                        getPixels(el, 'margin-left') + getPixels(el, 'margin-right') +
                        getPixels(el, 'padding-left') + getPixels(el, 'padding-right') +
                        getPixels(el, 'border-left-width') + getPixels(el, 'border-right-width'),
                height: getPixels(el, 'height') +
                        getPixels(el, 'margin-top') + getPixels(el, 'margin-bottom') +
                        getPixels(el, 'padding-top') + getPixels(el, 'padding-bottom') +
                        getPixels(el, 'border-top-height') + getPixels(el, 'border-bottom-height')
            };
        }

        YAHOO.hippo.DataTable = function(id, config) {
            YAHOO.hippo.DataTable.superclass.constructor.apply(this, arguments);
        };

        YAHOO.extend(YAHOO.hippo.DataTable, YAHOO.hippo.Widget, {

            resize: function(sizes, preserveScroll) {
                var table = Dom.get(this.id);
                if (table === null || table === undefined) {
                    return;
                }
                this._updateGecko(sizes, table, preserveScroll);
            },

            update : function() {
                var table = Dom.get(this.id),
                    un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                this.resize(un.getSizes(), true);
            },

            _updateGecko : function(sizes, table, preserveScroll) {
                var thead, headers, tbody, bodyDiv, previousScrollTop, h, widthData, availableHeight;

                thead = this._getThead(table);
                headers = Dom.getElementsByClassName('headers', 'tr', thead);
                if (headers.length === 0) {
                    return;
                }

                tbody = this._getTbody(table);
                bodyDiv = tbody.parentNode.parentNode;
                previousScrollTop = bodyDiv.scrollTop;

                Dom.setStyle(thead.parentNode.parentNode, 'margin-right', YAHOO.hippo.HippoAjax.getScrollbarWidth() + 'px');
                h = Dom.getElementsByClassName(this.config.autoWidthClassName, 'th', headers[0]);
                if (h !== null && h !== undefined) {
                    Dom.setStyle(h, 'width', 'auto');
                    Dom.setStyle(h, 'min-width', '100px');
                }
                widthData = this.getWidthData(headers[0], sizes);

                Dom.setStyle(bodyDiv, 'display', 'block');
                this._setColsWidth(tbody, headers[0], widthData);
                Dom.setStyle(thead.parentNode.parentNode, 'display', 'block');

                availableHeight = this.getHeightData(table, thead, sizes);
                Dom.setStyle(bodyDiv, 'height', availableHeight + 'px');
                if (preserveScroll) {
                    bodyDiv.scrollTop = previousScrollTop;
                }
                Dom.setStyle(bodyDiv, 'overflow-y', 'auto');
            },

            getWidthData : function(headrow, sizes) {
                var result, fixedHeaderWidth, cells, i, len, child, region, cellWidth;

                result = {
                    autoIndex: -1,
                    widths: []
                };

                fixedHeaderWidth = YAHOO.hippo.HippoAjax.getScrollbarWidth();

                cells = headrow.getElementsByTagName('th');
                for (i = 0, len = cells.length; i < len; i++) {
                    child = cells[i];
                    if (!Dom.hasClass(child, this.config.autoWidthClassName)) {
                        region = getRegion(child);
                        fixedHeaderWidth += region.width;
                        result.widths.push(region.width);
                    } else {
                        result.widths.push(0);
                        result.autoIndex = i;
                    }
                }

                if (result.autoIndex >= 0) {
                    cellWidth = sizes.wrap.w - fixedHeaderWidth;
                    if (cellWidth < 100) {
                        cellWidth = 100;
                    }
                    result.widths[result.autoIndex] = cellWidth;
                }

                return result;
            },

            getHeightData : function(table, thead, sizes) {
                var siblings, fixedTableHeight, i, len, sibling, pagingTr, pagingHeight;

                siblings = Dom.getChildren(table.parentNode);
                fixedTableHeight = 0;
                //calculate height of table siblings
                for (i = 0, len = siblings.length; i < len; i++) {
                    sibling = siblings[i];
                    if (sibling !== table) {
                        fixedTableHeight += getRegion(sibling).height;
                    }
                }
                //add margin/padding/border value of table
                fixedTableHeight += this.helper.getMargin(table).h;
                //add height of header
                fixedTableHeight += getRegion(thead).height;

                pagingTr = Dom.getElementsByClassName('hippo-list-paging', 'tr', table);
                pagingHeight = 0;
                if (pagingTr.length > 0) {
                    for (i = 0, len = pagingTr.length; i < len; i++) {
                        pagingHeight += getRegion(pagingTr[i]).height;
                    }
                }

                return sizes.wrap.h - (fixedTableHeight + pagingHeight);
            },

            _getAutoWidthHeader : function(headrow) {
                var h = Dom.getElementsByClassName(this.config.autoWidthClassName, 'th', headrow);
                if (h.length > 0) {
                    return h[0];
                }
                return null;
            },

            _getTbody : function(table) {
                var tbodies, i, len;

                tbodies = table.getElementsByTagName('tbody');
                if (tbodies.length > 0) {
                    for (i = 0, len = tbodies.length; i < len; i++) {
                        if (Dom.hasClass(tbodies[i].parentNode.parentNode, 'list-data-table-body')) {
                            return tbodies[i];
                        }
                    }
                    return tbodies[0];
                }
                return table.rows[table.rows.length - 1].parentNode;
            },

            _getThead : function(table) {
                var theads, i, len;

                theads = table.getElementsByTagName('thead');
                if (theads.length > 0) {
                    for (i = 0, len = theads.length; i < len; i++) {
                        if (Dom.hasClass(theads[i].parentNode.parentNode, 'list-data-table-header')) {
                            return theads[i];
                        }
                    }
                    return theads[0];
                }
                return null;
            },

            _setColsWidth : function(tbody, headrow, widthData) {
                var widths, rows, j, len, cells, i, cell, padding, newWidth;

                widths = widthData.widths;

                if (widthData.autoIndex >= 0) {
                    rows = tbody.getElementsByTagName('tr');
                    for (j = 0, len = rows.length; j < len; j++) {
                        cells = rows[j].getElementsByTagName('td');
                        i = widthData.autoIndex;
                        cell = cells[i];
                        padding = this.helper.getMargin(cell).w;
                        newWidth = (widths[i] - padding);
                        if (newWidth < 100) {
                            newWidth = 100;
                        }
                        Dom.setStyle(cell, 'width', newWidth + 'px');
                        Dom.setStyle(cell, 'min-width', newWidth + 'px');
                    }
                }
            }
        });
    }());

    YAHOO.register("HippoDataTable", YAHOO.hippo.DataTable, {
        version: "2.8.1", build: "19"
    });
}
