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
                if (table == null) {
//                    console.log('oops: id ' + this.id + ' does no longer exist');
                    return;
                }
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

                Dom.setStyle(thead.parentNode.parentNode, 'margin-right', YAHOO.hippo.HippoAjax.getScrollbarWidth() + 'px');
                var h = Dom.getElementsByClassName(this.config.autoWidthClassName, 'th', headers[0]);
                if (h != null) {
                    Dom.setStyle(h, 'width', 'auto');
                    Dom.setStyle(h, 'min-width', '100px');
                }
                var widthData = this.getWidthData(headers[0], sizes);

                Dom.setStyle(bodyDiv, 'display', 'block');
                this._setColsWidth(tbody, headers[0], widthData);
                Dom.setStyle(thead.parentNode.parentNode, 'display', 'block');

                var availableHeight = this.getHeightData(table, thead, sizes);
                Dom.setStyle(bodyDiv, 'height', availableHeight + 'px');
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

                var cells = headrow.getElementsByTagName('th');
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
                var tbodies = table.getElementsByTagName('tbody');
                if (tbodies.length > 0) {
                    for (var i = 0; i < tbodies.length; i++) {
                        if (Dom.hasClass(tbodies[i].parentNode.parentNode, 'list-data-table-body')) {
                            return tbodies[i];
                        }
                    }
                    return tbodies[0];
                }
                return table.rows[table.rows.length - 1].parentNode;
            },

            _getThead : function(table) {
                var theads = table.getElementsByTagName('thead');
                if (theads.length > 0) {
                    for (var i = 0; i < theads.length; i++) {
                        if (Dom.hasClass(theads[i].parentNode.parentNode, 'list-data-table-header')) {
                            return theads[i];
                        }
                    }
                    return theads[0];
                }
                return null;
            },

            _setColsWidth : function(tbody, headrow, widthData) {
                var widths = widthData.widths;

                if (widthData.autoIndex >= 0) {
                    var rows = tbody.getElementsByTagName('tr');
                    for (var j = 0; j < rows.length; j++) {
                        var cells = rows[j].getElementsByTagName('td');
                        var i = widthData.autoIndex;
                        var cell = cells[i];
                        var padding = this.helper.getMargin(cell).w;
                        var newWidth = (widths[i] - padding);
                        if (newWidth < 100) {
                            newWidth = 100;
                        }
                        Dom.setStyle(cell, 'width', newWidth + 'px');
                        Dom.setStyle(cell, 'float', 'left');
                    }
                }
            }
        });
    })();

    YAHOO.register("HippoDataTable", YAHOO.hippo.DataTable, {
        version: "2.8.1", build: "19"
    });
}
