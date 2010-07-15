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

            this.data = {
                nrOfCols: -1,
                nrOfRows: -1,

                headerWidth: 0,
                columnWidth: 0,

                fixedHeaderWidth: 0,
                fixedColumnWidth: 0,

                fixedTableHeight: 0,
                tableHeight: 0,
                pagingHeight: 0,

                scrolling: false
            };

        };
        
        YAHOO.extend(YAHOO.hippo.DataTable, YAHOO.hippo.Widget, {

        	  resize: function(sizes, preserveScroll) {
                var table = Dom.get(this.id);
                if(table.rows.length <= 1) {
                     return; //no rows in body
                }

                if(YAHOO.env.ua.gecko > 0 || YAHOO.env.ua.webkit > 0) {
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
                if(YAHOO.env.ua.ie == 8 && rows.length == 0) {
                    var r = Dom.getRegion(table);
                    if((r.height-40) > sizes.wrap.h) {
                        var un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                        if(un) {
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
                for(var j=0; j<ch.length; ++j) {
                    sibHeight += Dom.getRegion(ch[j]).height;
                }

                var nonBodyHeight = this.helper.getMargin(table).h + Dom.getRegion(rows[0].parentNode).height + sibHeight;

                var lastRowParentTag = rows[rows.length-1].parentNode.tagName;
                if(lastRowParentTag.toLowerCase() == 'tfoot') {
                    nonBodyHeight += 65;
                } else {
                    //Wicket 1.4 introduces a datatable with two tbody elements instead of thead/tfoot....
                    var pagingRow = Dom.getElementsByClassName ( 'hippo-list-paging', 'tr', table);
                    if(Lang.isArray(pagingRow) && pagingRow.length > 0) {
                        nonBodyHeight += 65;
                    }
                }

                var tbody = this._getTbody();

                var prevHeight = Dom.getStyle(tbody, 'height');
                Dom.setStyle(tbody, 'height', 'auto');

                var tbodyRegion = Dom.getRegion(tbody);
                var availableHeight = sizes.wrap.h - nonBodyHeight;

                var scrolling = tbodyRegion.height > availableHeight;

                if(scrolling) {
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
                var headers = Dom.getElementsByClassName('headers' , 'tr' , thead);
                if(headers.length == 0) {
                    return;
                }
                var tbody = this._getTbody(table);
                var previousScrollTop = tbody.scrollTop;

                var cells = Dom.getElementsByClassName(this.config.autoWidthClassName , 'td' , tbody);
                if(cells.length == 0) {
                    return;
                }

                var widthData = this.getWidthData(headers, cells, sizes);
                //if(widthData.changed) {
                    this._setColsWidth(headers, widthData.headerWidth, cells, widthData.columnWidth);
                //}

                var heightData = this.getHeightData(table, thead, tbody, sizes, widthData.changed);
                //if(heightData.changed) {
                    Dom.setStyle(tbody, 'height', heightData.tbodyHeight + 'px');
                //}
                if(preserveScroll) {
                    tbody.scrollTop = previousScrollTop;
                }

                if(widthData.changed && heightData.scrolling) {
                    var scrollWidth = YAHOO.hippo.HippoAjax.getScrollbarWidth();
//                    for(var i=0; i<cells.length; ++i) {
//                        var td = cells[i];
//                        Dom.setStyle(td, 'width', (widthData.columnWidth - scrollWidth) + 'px');
//                        Dom.setStyle(td, 'display', 'block');
//                        Dom.setStyle(td, 'overflow', 'hidden');
//                    }

                   // this._setColsWidth(headers, widthData.headerWidth - scrollWidth, cells, widthData.columnWidth - scrollWidth);
                }

                if(YAHOO.env.ua.webkit > 0) {
                      Dom.setStyle(headers[0], 'display', 'block');
                      Dom.setStyle(tbody, 'display', 'block');
                }
            },

            getWidthData : function(headers, cells, sizes) {
                var nrOfCols = headers[0].children.length;
                var prevHeaderWidth = this.data.headerWidth;
                var prevColumnWidth = this.data.columnWidth;

                if(this.data.nrOfCols != nrOfCols) {

                    //Number of columns changed, recalculate width
                    this.data.fixedHeaderWidth = 0;
                    this.data.fixedColumnWidth = 0;

                    //TODO: should this before height calc or here?
                    //Dom.setStyle(tbody, 'height', 'auto');

                    //reset tds width for correct calculation
//                    for(var i=0; i<cells.length; ++i) {
//                        Dom.setStyle(cells[i], 'width', '');
//                        Dom.setStyle(cells[i], 'display', '');
//                    }
                    //reset header as well
                    var className = this.config.autoWidthClassName;
                    var h = Dom.getElementBy(function(node) { return Dom.hasClass(node, className); }, 'th', headers[0]);
                    Dom.setStyle(h, 'width', '');

                    this.data.fixedHeaderWidth += this.helper.getMargin(h).w;
                    this.data.fixedColumnWidth += this.helper.getMargin(cells[0]).w;

                    var header = headers[0];
                    for(var i=0; i< header.children.length; i++) {
                        var child = header.children[i];
                        if(!Dom.hasClass(child, this.config.autoWidthClassName)) {
                            var region = Dom.getRegion(child);
                            this.data.fixedHeaderWidth += region.width;
                            this.data.fixedColumnWidth += region.width;
                        }
                    }

                    this.data.nrOfCols = nrOfCols;
                }

                this.data.headerWidth = sizes.wrap.w - this.data.fixedHeaderWidth;
                this.data.columnWidth = sizes.wrap.w - this.data.fixedColumnWidth;

                return {
                    headerWidth: this.data.headerWidth,
                    columnWidth: this.data.columnWidth,
                    changed: prevHeaderWidth != this.data.headerWidth && prevColumnWidth != this.data.columnWidth
                }
            },

            getHeightData : function(table, thead, tbody, sizes, widthChanged) {
                var siblings = Dom.getChildren(table.parentNode);
                if(this.data.nrOfTableSiblings != siblings.length) {
                    //calculate height of table siblings
                    for(var i=0; i<siblings.length; ++i) {
                        var sibling = siblings[i];
                        if(sibling != table) {
                            this.data.fixedTableHeight += Dom.getRegion(sibling).height;
                        }
                    }
                    //add margin/padding/border value of table
                    this.data.fixedTableHeight += this.helper.getMargin(table).h;
                    //add height of header
                    this.data.fixedTableHeight += Dom.getRegion(thead).height;

                    this.data.nrOfTableSiblings = siblings.length;
                }

                var pagingTr = Dom.getElementsByClassName('hippo-list-paging', 'tr', table);
                if(pagingTr.length > 0) {
                    this.data.pagingHeight = Dom.getRegion(pagingTr).height;
                } else {
                    this.data.pagingHeight = 0;
                }

                var prevHeight = this.data.tbodyHeight;
                var availableHeight = sizes.wrap.h - (this.data.fixedTableHeight + this.data.pagingHeight);
                Dom.setStyle(tbody, 'height', 'auto');
                var tbodyHeight = Dom.getRegion(tbody).height;
                this.data.scrolling = tbodyHeight > availableHeight;
                this.data.tbodyHeight = this.data.scrolling ?  availableHeight : tbodyHeight;

                return {
                    changed: prevHeight != this.data.tbodyHeight && true,
                    tbodyHeight: this.data.tbodyHeight,
                    scrolling: this.data.scrolling
                }
            },

            _getTbody : function(table) {
                var tbodies = Dom.getElementsByClassName('datatable-tbody', 'tbody', table);
                if(tbodies.length > 0) {
                    return tbodies[0];
                }
                return table.rows[table.rows.length-1].parentNode;
            },

            _getThead : function(table) {
                return table.rows[0].parentNode;
            },

            _setColsWidth : function(headers, headerWidth, cells, cellWidth) {
                var className = this.config.autoWidthClassName;
                var h = Dom.getElementBy(function(node) { return Dom.hasClass(node, className); }, 'th', headers[0]);
                Dom.setStyle(h, 'width', headerWidth + 'px');
                if(YAHOO.env.ua.gecko > 0) {
                    //Dom.setStyle(h, 'display', 'block');
                    //Dom.setStyle(h, 'overflow', 'hidden');
                }

                for(var i=0; i<cells.length; ++i) {
                    var td = cells[i];
                    Dom.setStyle(td, 'width', cellWidth + 'px');
                    Dom.setStyle(td, 'display', 'block');
                    Dom.setStyle(td, 'overflow', 'hidden');
                }

            }

        });
    })();

    YAHOO.register("HippoDataTable", YAHOO.hippo.DataTable, {
        version: "2.8.1", build: "19"
    });
}
	
