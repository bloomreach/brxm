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

            this.autoWidthColumnMargin = null;
            this.widthOtherColumns = null;

            this.numberOfColumns = 0;
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
                var rows = table.rows;
                var thead = this._getThead(table);
                var tbody = this._getTbody(table);

                var heightBeforeUpdate = Dom.getStyle(tbody, 'height');
                Dom.setStyle(tbody, 'height', 'auto');

                var availableWidth = sizes.wrap.w;

                var headers = Dom.getElementsByClassName('headers' , 'tr' , thead);
                if(headers.length == 0) {
                    return;
                }
                var nrOfCols = headers[0].children.length;
                this.validateCache(nrOfCols);
                this.numberOfColumns = nrOfCols;

                if(YAHOO.env.ua.webkit > 0) {
//                    Dom.setStyle(headers[0], 'display', 'table-row');
//                    Dom.setStyle(tbody, 'display', 'table-row-group');
                }


                var autoWidthCols = Dom.getElementsByClassName(this.config.autoWidthColumnClassname , 'td' , tbody);
                if(autoWidthCols.length == 0) {
                    return;
                }

                //reset tds width for correct calculation
                for(var i=0; i<autoWidthCols.length; ++i) {
                    Dom.setStyle(autoWidthCols[i], 'width', 'auto');
                }

                //calculate own margin if not set
                if(this.getAutoWidthColumnMargin() == null) {
                    var margin = this.helper.getMargin(autoWidthCols[0]);
                    availableWidth -= margin.w;
                    this.setAutoWidthColumnMargin(margin);
                } else {
                    var margin = this.getAutoWidthColumnMargin();
                    availableWidth -= margin.w;
                }

                if(this.getWidthOtherColumns() == null) {
                    var header = headers[0];
                    var othersWidth = 0;
                    for(var i=0; i< header.children.length; i++) {
                        var child = header.children[i];
                        if(!Dom.hasClass(child, this.config.autoWidthColumnClassname)) {
                            var region = Dom.getRegion(child);
                            othersWidth += region.width;
                        }
                    }
                    console.log('Other width ' + othersWidth);
                    this.setWidthOtherColumns(othersWidth);
                    availableWidth -= othersWidth;
                } else {
                    availableWidth -= this.getWidthOtherColumns();
                }

                for(var i=0; i<autoWidthCols.length; ++i) {
                    var td = autoWidthCols[i];
                    Dom.setStyle(td, 'width', availableWidth + 'px');
                    Dom.setStyle(td, 'display', 'block');
                    Dom.setStyle(td, 'overflow', 'hidden');
                }

                if(YAHOO.env.ua.webkit > 0) {
                    var cols = Dom.getElementsByClassName(this.config.autoWidthColumnClassname , 'th' , thead);
                    Dom.setStyle(cols[0], 'width', availableWidth + 'px');
                }

                //do height
                var availableHeight = sizes.wrap.h;

                //first subtract height of table siblings
                var siblings = Dom.getChildren(table.parentNode);
                for(var i=0; i<siblings.length; ++i) {
                    var sibling = siblings[i];
                    if(sibling != table) {
                        availableHeight -= Dom.getRegion(sibling).height;
                    }
                }

                //then substract margin/padding/border value of table and height of header
                var nonBodyHeight = this.helper.getMargin(table).h + Dom.getRegion(thead).height;
                availableHeight -= nonBodyHeight;

                //then try to detect paging element and subtract height which is a hard-coded value of 65 px
                //TODO: calculate footer height
                var lastRowParentTag = rows[rows.length-1].parentNode.tagName;
                if(lastRowParentTag.toLowerCase() == 'tfoot') {
                    availableHeight -= 65;
                } else {
                    //Wicket 1.4 introduces a datatable with two tbody elements instead of thead/tfoot....
                    var pagingRow = Dom.getElementsByClassName ( 'hippo-list-paging', 'tr', table);
                    if(Lang.isArray(pagingRow) && pagingRow.length > 0) {
                        availableHeight -= 65;
                    }
                }

                var tbodyRegion = Dom.getRegion(tbody);

                console.log('Tbody: previous height: ' + heightBeforeUpdate + ', current height: ' + tbodyRegion.height);
                //var availableHeight = sizes.wrap.h - nonBodyHeight;

                var scrolling = tbodyRegion.height > availableHeight;
                if(scrolling) {
                    availableWidth -= YAHOO.hippo.HippoAjax.getScrollbarWidth();
                    for(var i=0; i<autoWidthCols.length; ++i) {
                        var td = autoWidthCols[i];
                        Dom.setStyle(td, 'width', availableWidth + 'px');
                    }

                    console.log('set height with scrolling: ' + availableHeight);
                    Dom.setStyle(tbody, 'height', availableHeight + 'px');
                } else {
                    console.log('set height without scrolling: ' + tbodyRegion);
                    Dom.setStyle(tbody, 'height', tbodyRegion.height + 'px');
                }

                if(YAHOO.env.ua.webkit > 0) {
//                    window.setTimeout(function() {
                      Dom.setStyle(headers[0], 'display', 'block');
                      Dom.setStyle(tbody, 'display', 'block');
//                    }, 200);
                }

            },

            getAutoWidthColumnMargin : function() {
                return this.autoWidthColumnMargin;
            },

            setAutoWidthColumnMargin : function(margin) {
                if(this.config.cacheEnabled) {
                    this.autoWidthColumnMargin = margin;
                }
            },

            getWidthOtherColumns: function() {
                return this.widthOtherColumns;
            },

            setWidthOtherColumns: function(w) {
                if(this.config.cacheEnabled) {
                    this.widthOtherColumns = w;
                }
            },

            validateCache : function(nrOfCols) {
                if(this.numberOfColumns > 0 && this.numberOfColumns != nrOfCols) {
                    this.autoWidthColumnMargin = null;
                    this.widthOtherColumns = null;
                    console.log('cache invalidated');
                } else {
                    console.log('cache ok ' + nrOfCols);
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
            }

        });
    })();

    YAHOO.register("HippoDataTable", YAHOO.hippo.DataTable, {
        version: "2.8.1", build: "19"
    });
}
	
