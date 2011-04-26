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
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.DataTable = function(id, config) {
            YAHOO.hippo.DataTable.superclass.constructor.apply(this, arguments);
        };

        YAHOO.extend(YAHOO.hippo.DataTable, YAHOO.hippo.Widget, {

            resize: function(sizes, preserveScroll) {
                var table = Dom.get(this.id);
                this._updateGecko(sizes, table, preserveScroll);
            },

            update : function() {
                var table = Dom.get(this.id);
                var un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                this.resize(un.getSizes(), true);
            },

            _updateGecko : function(sizes, table, preserveScroll) {
                var thead = this._getThead(table);
                var headers = Dom.getElementsByClassName('headers', 'tr', thead);
                if (headers.length == 0) {
                    return;
                }

                var tbody = this._getTbody(table);
                var bodyDiv = tbody.parentNode.parentNode;
                var previousScrollTop = bodyDiv.scrollTop;

                var widthData = this.getWidthData(headers[0], sizes);

                Dom.setStyle(tbody.parentNode.parentNode, 'display', 'block');
                this._setColsWidth(tbody, headers[0], widthData);
                Dom.setStyle(thead.parentNode.parentNode, 'display', 'block');

                var availableHeight = this.getHeightData(table, thead, sizes);
                Dom.setStyle(bodyDiv, 'height', 'auto');
                var tbodyHeight = Dom.getRegion(bodyDiv).height;
                var height =  (tbodyHeight > availableHeight ? availableHeight : tbodyHeight );
                Dom.setStyle(bodyDiv, 'height', height + 'px');
                if (preserveScroll) {
                    bodyDiv.scrollTop = previousScrollTop;
                }
                Dom.setStyle(bodyDiv, 'overflow-y', 'auto');
            },

            getWidthData : function(headrow, sizes) {
                var result = {
                    autoIndex: -1,
                    widths: []
                };

                var fixedHeaderWidth = YAHOO.hippo.HippoAjax.getScrollbarWidth();

                var cells = new YAHOO.util.Element(headrow).getElementsByTagName('th');
                for (var i = 0; i < cells.length; i++) {
                    var child = cells[i];
                    if (!Dom.hasClass(child, this.config.autoWidthClassName)) {
                        var region = Dom.getRegion(child);
                        fixedHeaderWidth += region.width;
                        result.widths.push(region.width);
                    } else {
                        result.widths.push(0);
                        result.autoIndex = i;
                    }
                }

                if (result.autoIndex >= 0) {
                    var cellWidth = sizes.wrap.w - fixedHeaderWidth;
                    if (cellWidth < 100) {
                        cellWidth = 100;
                    }
                    result.widths[result.autoIndex] = cellWidth;
                }

                return result;
            },

            getHeightData : function(table, thead, sizes) {
                var siblings = Dom.getChildren(table.parentNode);
                var fixedTableHeight = 0;
                //calculate height of table siblings
                for (var i = 0; i < siblings.length; ++i) {
                    var sibling = siblings[i];
                    if (sibling != table) {
                        fixedTableHeight += Dom.getRegion(sibling).height;
                    }
                }
                //add margin/padding/border value of table
                fixedTableHeight += this.helper.getMargin(table).h;
                //add height of header
                fixedTableHeight += Dom.getRegion(thead).height;

                var pagingTr = Dom.getElementsByClassName('hippo-list-paging', 'tr', table);
                var pagingHeight = 0;
                if (pagingTr.length > 0) {
                    for (var i = 0; i < pagingTr.length; i++) {
                        pagingHeight += Dom.getRegion(pagingTr[i]).height;
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
                var tbodies = new YAHOO.util.Element(table).getElementsByTagName('tbody');
                if (tbodies.length > 0) {
                    return tbodies[0];
                }
                return table.rows[table.rows.length - 1].parentNode;
            },

            _getThead : function(table) {
                var theads = new YAHOO.util.Element(table).getElementsByTagName('thead');
                if (theads.length > 0) {
                    return theads[0];
                }
                return null;
            },

            _setColsWidth : function(tbody, headrow, widthData) {
                var widths = widthData.widths;

                if (widthData.autoIndex >= 0) {
                    // update header
                    var h = this._getAutoWidthHeader(headrow);
                    if (h != null) {
                        var padding = this.helper.getMargin(h).w;
                        Dom.setStyle(h, 'width', (widths[widthData.autoIndex] - padding) + 'px');
                    }

                    var rows = new YAHOO.util.Element(tbody).getElementsByTagName('tr');
                    for (var j = 0; j < rows.length; j++) {
                        var cells = rows[j].getElementsByTagName('td');
                        var i = widthData.autoIndex;
                        var cell = cells[i];
                        var padding = this.helper.getMargin(cell).w;
                        Dom.setStyle(cell, 'width', (widths[i] - padding) + 'px');
                        Dom.setStyle(cell, 'float', 'left');
                        Dom.setStyle(cell, 'overflow', 'hidden');
                        Dom.setStyle(cell, 'white-space', 'nowrap');
                    }
                }
            }
        });
    })();

    YAHOO.register("HippoDataTable", YAHOO.hippo.DataTable, {
        version: "2.8.1", build: "19"
    });
}
