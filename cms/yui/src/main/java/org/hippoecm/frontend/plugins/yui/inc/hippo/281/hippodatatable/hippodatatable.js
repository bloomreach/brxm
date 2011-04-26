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
                if (table.rows.length <= 1) {
                    return; //no rows in body
                }

                if (YAHOO.env.ua.gecko > 0 || YAHOO.env.ua.webkit > 0) {
                    this._updateGecko(sizes, table, preserveScroll);
                } else {
                    this._updateIE(sizes, table);
                }
            },

            update : function() {
                var table = Dom.get(this.id);
                var un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                this.resize(un.getSizes(), true);
            },

            _updateIE: function(sizes, table) {
                var rows = table.rows;

                //ie8 standards mode fails on rows/cols/cells attributes..
                if (YAHOO.env.ua.ie == 8 && rows.length == 0) {
                    var r = Dom.getRegion(table);
                    if ((r.height - 40) > sizes.wrap.h) {
                        var un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                        if (un) {
                            un.set('scroll', true);
                        }
                    }
                    return; //don't bother
                }

                //if table has top margin, add it to table minheight
                //plus add header height to table minheight
                //Detect toggle-button and add height if needed
                var ch = Dom.getChildrenBy(table.parentNode, function(node) {
                    return node != table;
                });
                var sibHeight = 0;
                for (var j = 0; j < ch.length; ++j) {
                    sibHeight += Dom.getRegion(ch[j]).height;
                }

                var nonBodyHeight = this.helper.getMargin(table).h + Dom.getRegion(rows[0].parentNode).height + sibHeight;

                var lastRowParentTag = rows[rows.length - 1].parentNode.tagName;
                if (lastRowParentTag.toLowerCase() == 'tfoot') {
                    nonBodyHeight += 65;
                } else {
                    //Wicket 1.4 introduces a datatable with two tbody elements instead of thead/tfoot....
                    var pagingRow = Dom.getElementsByClassName('hippo-list-paging', 'tr', table);
                    if (Lang.isArray(pagingRow) && pagingRow.length > 0) {
                        nonBodyHeight += 65;
                    }
                }

                var tbody = this._getTbody(table);

                var prevHeight = Dom.getStyle(tbody, 'height');
                Dom.setStyle(tbody, 'height', 'auto');

                var tbodyRegion = Dom.getRegion(tbody);
                var availableHeight = sizes.wrap.h - nonBodyHeight;

                var scrolling = tbodyRegion.height > availableHeight;

                if (scrolling) {
                    //Couldn't get the scrolling of a tbody working in IE so set the whole
                    //unit to scrolling
                    //TODO: ask Hippo Services
                    var un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                    un.set('scroll', true);
                } else {
                    Dom.setStyle(tbody, 'height', prevHeight);
                }
            },

            _updateGecko : function(sizes, table, preserveScroll) {
                var thead = this._getThead(table);
                var headers = Dom.getElementsByClassName('headers', 'tr', thead);
                if (headers.length == 0) {
                    return;
                }

                var tbody = this._getTbody(table);
                var previousScrollTop = tbody.scrollTop;

                var widthData = this.getWidthData(headers[0], sizes);

                Dom.setStyle(tbody, 'display', 'block');
                this._setColsWidth(tbody, headers[0], widthData);
                Dom.setStyle(thead, 'display', 'block');

                var availableHeight = this.getHeightData(table, thead, tbody, sizes);
                Dom.setStyle(tbody, 'height', 'auto');
                var tbodyHeight = Dom.getRegion(tbody).height;
                var height =  (tbodyHeight > availableHeight ? availableHeight : tbodyHeight );
                Dom.setStyle(tbody, 'height', height + 'px');
                if (preserveScroll) {
                    tbody.scrollTop = previousScrollTop;
                }
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

            getHeightData : function(table, thead, tbody, sizes) {
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
//                var tbodies = Dom.getElementsByClassName('datatable-tbody', 'tbody', table);
                if (tbodies.length > 0) {
                    return tbodies[0];
                }
                return table.rows[table.rows.length - 1].parentNode;
            },

            _getThead : function(table) {
                return table.rows[0].parentNode;
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
