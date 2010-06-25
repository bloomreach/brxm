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

        	  resize: function(sizes) {
                if(YAHOO.env.ua.gecko > 0 || YAHOO.env.ua.webkit > 0) {
                    this._updateGecko(sizes);
                } else {
                    this.update(sizes);
                }
        	  },
        	
            update: function(sizes) {
                var table = Dom.get(this.id);
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

                if(rows.length <= 1) {
                    return; //no rows in body
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
                var thead = rows[0].parentNode;
                var tbody = rows[rows.length-1].parentNode;

                var prevHeight = Dom.getStyle(tbody, 'height');
                Dom.setStyle(tbody, 'height', 'auto');

                var tbodyRegion = Dom.getRegion(tbody);
                console.log('TBOdy height: ' + Lang.dump(tbodyRegion))
                var availableHeight = sizes.wrap.h - nonBodyHeight;

                var scrolling = tbodyRegion.height > availableHeight;

                if (YAHOO.env.ua.ie > 0) {
                    if(scrolling) {
                            //Couldn't get the scrolling of a tbody working in IE so set the whole
                            //unit to scrolling
                            //TODO: ask Hippo Services
                            var un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                            un.set('scroll', true);
                    } else {
                            Dom.setStyle(tbody, 'height', prevHeight);
                    }
                } else if (YAHOO.env.ua.webkit) {
                        Dom.setStyle(table, 'width', '100%');
                        Dom.setStyle(tbody, 'height', availableHeight + 'px');

                        if(Lang.isUndefined(table.previousScrolling)) {
                            table.previousScrolling != scrolling;
                        }

                        if(scrolling) {
                            if(table.previousScrolling) {
                                Dom.setStyle(tbody, 'width', (sizes.wrap.w-0) + 'px');
                            } else {
                                Dom.setStyle(tbody, 'width', (sizes.wrap.w-0) + 'px');
                                var row = table.rows[1];

                                //save dimensions of columns
                                var colSizes = [row.cells.length];
                                for(var i=0; i<row.cells.length; i++) {
                                    colSizes[i] = Dom.getRegion(row.cells[i]);
                                }

                                //header will be broken after setting these
                                Dom.setStyle(table.rows[0], 'display', 'block');
                                Dom.setStyle(table.rows[0], 'position', 'relative');
                                Dom.setStyle(tbody, 'display', 'block'); //make body scrollable

                                //restore header dimensions
                                for(var i=0; i<colSizes.length; i++) {
                                    Dom.setStyle(table.rows[0].cells[i], 'width', colSizes[i].width + 'px');
                                }
                            }
                        } else {
                            //reset
                            Dom.setStyle(table.rows[0], 'display', 'table-row');
                            Dom.setStyle(table.rows[0], 'position', 'static');
                            Dom.setStyle(tbody, 'display', 'table-row-group');
                            for(var i=0; i<table.rows[0].cells.length; i++) {
                                Dom.setStyle(table.rows[0].cells[i], 'width', 'auto');
                            }

                        }
                        table.previousScrolling = scrolling;
                } else {
                    //firefox
                    if(scrolling) {
                        console.log('set height with scrolling: ' + availableHeight);
                        Dom.setStyle(tbody, 'height', availableHeight + 'px');
                    } else {
                        console.log('set height without scrolling: ' + tbodyRegion);
                        Dom.setStyle(tbody, 'height', tbodyRegion.height + 'px');
                    }
                }
        	  },

            _updateGecko : function(sizes) {

                var table = Dom.get(this.id);
                if(table.rows.length <= 1) {
                     return; //no rows in body
                }

                var thead = this._getThead(table);
                var headers = Dom.getElementsByClassName('headers' , 'tr' , thead);
                if(headers.length == 0) {
                    return;
                }

                var tbody = this._getTbody(table);
                var cells = Dom.getElementsByClassName(this.config.autoWidthClassName , 'td' , tbody);
                if(cells.length == 0) {
                    return;
                }

                var widthData = this.getWidthData(headers, cells, sizes);
                if(widthData.changed) {
                    this._setColsWidth(headers, widthData.headerWidth, cells, widthData.columnWidth);
                } else {
                    console.log('no width changes!');
                }

                var heightData = this.getHeightData(table, thead, tbody, sizes, widthData.changed);
                Dom.setStyle(tbody, 'height', heightData.tbodyHeight + 'px');
//                if(widthData.changed && heightData.scrolling) {
//                    console.log('Fixing width because of scrolling');
//                    var scrollWidth = YAHOO.hippo.HippoAjax.getScrollbarWidth();
//                    this._setColsWidth(headers, widthData.headerWidth, cells, widthData.columnWidth - scrollWidth);
//                }

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
                    console.log('Calculating width sizes');

                    //Number of columns changed, recalculate width
                    this.data.fixedHeaderWidth = 0;
                    this.data.fixedColumnWidth = 0;

                    //TODO: should this before height calc or here?
                    //Dom.setStyle(tbody, 'height', 'auto');

                    //reset tds width for correct calculation
                    for(var i=0; i<cells.length; ++i) {
                        Dom.setStyle(cells[i], 'width', '');
                    }
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
                    //TODO: calculate height
                    this.data.pagingHeight = 65;
                } else {
                    this.data.pagingHeight = 0;
                }

                this.data.tableHeight = sizes.wrap.h - (this.data.fixedTableHeight + this.data.pagingHeight);

                if(this.data.nrOfRows != table.rows || widthChanged) {
                    //Number of rows changed, recalculate height
                    Dom.setStyle(tbody, 'height', 'auto');

                    var tbodyRegion = Dom.getRegion(tbody);
                    this.data.scrolling = tbodyRegion.height > this.data.tableHeight;
                    this.data.nrOfRows = table.rows;

                    if(!this.data.scrolling) {
                        this.data.tableHeight = tbodyRegion.height;
                    }
                }

                return {
                    tbodyHeight: this.data.tableHeight,
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
                Dom.setStyle(h, 'display', 'block');
                Dom.setStyle(h, 'overflow', 'hidden');

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
	
