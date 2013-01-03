/**
 * @description
 * <p>
 * Provides a singleton tables helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, layoutmanager, hippoajax
 * @module tables
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.TableHelper) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.TableHelperImpl = function() {
        };

        YAHOO.hippo.TableHelperImpl.prototype = {
            id: 'tableHelper',
            
            register : function(id, config) {
                var tableEl = Dom.get(id);
                if(Lang.isUndefined(tableEl.tableHelper)) {
                    tableEl.tableHelper = new YAHOO.hippo.Table(id, config);
                }
                tableEl.tableHelper.render();
            }
        };
        
        YAHOO.hippo.Table = function(id, config) {
            this.id = id;
            this.config = config;
            this.helper = new YAHOO.hippo.DomHelper();

            var table = Dom.get(id),
                me = this;
            YAHOO.hippo.LayoutManager.registerResizeListener(table, this, function(sizes) {
                me.resize(sizes);
            }, false);
            YAHOO.hippo.HippoAjax.registerDestroyFunction(table, function() {
                YAHOO.hippo.LayoutManager.unregisterResizeListener(table, me);
            }, this);
        };
        
        YAHOO.hippo.Table.prototype = {

            resize: function(sizes) {
                this.update(sizes);
            },

            render : function() {
                var table, unit, parent, reg;

                table = Dom.get(this.id);
                unit = YAHOO.hippo.LayoutManager.findLayoutUnit(table);

                if (unit !== null && unit !== undefined) {
                    this.update(unit.getSizes());
                } else {
                    //We're not inside a layout unit to provide us with dimension details, thus the 
                    //resize event will never be called. For providing an initial size, the first ancestor
                    //with a classname is used.
                    parent = Dom.getAncestorBy(table, function(node) {
                       return Lang.isValue(node.className) && Lang.trim(node.className).length > 0; 
                    });
                    if (parent !== null) {
                        reg = Dom.getRegion(parent);
                        this.update({wrap: {w: reg.width, h: reg.height}});
                    }
                }
            },
            
            update: function(sizes) {
                var table, rows, r, un, ch, sibHeight, j, len, nonBodyHeight, lastRowParentTag, pagingRow,
                    tbody, prevHeight, tbodyRegion, availableHeight, scrolling, row, colSizes, i;

                table = Dom.get(this.id);

                if (YAHOO.env.ua.gecko > 0) {
                    this._updateGecko(sizes);
                    return;
                }

                rows = table.rows;

                //ie8 standards mode fails on rows/cols/cells attributes..
                if (YAHOO.env.ua.ie === 8 && rows.length === 0) {
                    r = Dom.getRegion(table);
                    if ((r.height-40) > sizes.wrap.h) {
                        un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                        if (un) {
                            un.set('scroll', true);
                        }
                    }
                    return; //don't bother
                }

                if (rows.length <= 1) {
                    return; //no rows in body
                }

                //if table has top margin, add it to table minheight
                //plus add header height to table minheight
                //Detect toggle-button and add height if needed
                ch = Dom.getChildrenBy(table.parentNode, function(node) {
                    return node !== table;
                });
                sibHeight = 0;
                for (j = 0, len = ch.length; j < len; ++j) {
                    sibHeight += Dom.getRegion(ch[j]).height;
                }

                nonBodyHeight = this.helper.getMargin(table).h + Dom.getRegion(rows[0].parentNode).height + sibHeight;

                lastRowParentTag = rows[rows.length-1].parentNode.tagName;
                if(lastRowParentTag.toLowerCase() === 'tfoot') {
                    nonBodyHeight += 65;
                } else {
                    //Wicket 1.4 introduces a datatable with two tbody elements instead of thead/tfoot....
                    pagingRow = Dom.getElementsByClassName ( 'hippo-list-paging', 'tr', table);
                    if(Lang.isArray(pagingRow) && pagingRow.length > 0) {
                        nonBodyHeight += 65;
                    }
                }
                tbody = rows[rows.length-1].parentNode;

                prevHeight = Dom.getStyle(tbody, 'height');
                Dom.setStyle(tbody, 'height', 'auto');

                tbodyRegion = Dom.getRegion(tbody);
                availableHeight = sizes.wrap.h - nonBodyHeight;

                scrolling = tbodyRegion.height > availableHeight;

                if (YAHOO.env.ua.ie > 0) {
                    if(scrolling) {
                            //Couldn't get the scrolling of a tbody working in IE so set the whole
                            //unit to scrolling
                            //TODO: ask Hippo Services
                            un = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                            un.set('scroll', true);
                    } else {
                            Dom.setStyle(tbody, 'height', prevHeight);
                    }
                } else if (YAHOO.env.ua.webkit) {
                    Dom.setStyle(table, 'width', '100%');
                    Dom.setStyle(tbody, 'height', availableHeight + 'px');

                    if (Lang.isUndefined(table.previousScrolling)) {
                        table.previousScrolling = scrolling;
                    }

                    if (scrolling) {
                        Dom.setStyle(tbody, 'width', sizes.wrap.w + 'px');
                        if (!table.previousScrolling) {
                            row = table.rows[1];

                            //save dimensions of columns
                            colSizes = [row.cells.length];
                            for (i = 0, len = row.cells.length; i < len; i++) {
                                colSizes[i] = Dom.getRegion(row.cells[i]);
                            }

                            //header will be broken after setting these
                            Dom.setStyle(table.rows[0], 'display', 'block');
                            Dom.setStyle(table.rows[0], 'position', 'relative');
                            Dom.setStyle(tbody, 'display', 'block'); //make body scrollable

                            //restore header dimensions
                            for (i = 0, len = colSizes.length; i < len; i++) {
                                Dom.setStyle(table.rows[0].cells[i], 'width', colSizes[i].width + 'px');
                            }
                        }
                    } else {
                        //reset
                        Dom.setStyle(table.rows[0], 'display', 'table-row');
                        Dom.setStyle(table.rows[0], 'position', 'static');
                        Dom.setStyle(tbody, 'display', 'table-row-group');
                        for (i = 0, len = table.rows[0].cells.length; i < len; i++) {
                            Dom.setStyle(table.rows[0].cells[i], 'width', 'auto');
                        }

                    }
                    table.previousScrolling = scrolling;
                } else {
                    //firefox
                    if(scrolling) {
                        Dom.setStyle(tbody, 'height', availableHeight + 'px');
                    } else {
                        Dom.setStyle(tbody, 'height', tbodyRegion.height + 'px');
                    }
                }
            },

            _updateGecko : function(sizes) {
                var table, rows, thead, tbody, autoWidthTds, i, len, td, ths, header, dim, child,
                    region, tds, margin, tdWidth, availableHeight, siblings, sibling, nonBodyHeight, lastRowParentTag,
                    pagingRow, tbodyRegion, scrolling;

                table = Dom.get(this.id);
                if(table.rows.length <= 1) {
                     return; //no rows in body
                }
                rows = table.rows;
                thead = this._getThead(table);
                tbody = this._getTbody(table);

                Dom.setStyle(tbody, 'height', 'auto');

                autoWidthTds = Dom.getElementsByClassName('doclisting-name' , 'td' , tbody);
                if (autoWidthTds.length > 0) {
                    //reset tds width for correct calculation
                    for (i = 0, len = autoWidthTds.length; i < len; i++) {
                        td = autoWidthTds[i];
                        Dom.setStyle(td, 'width', 'auto');
                    }
                }


                ths = Dom.getElementsByClassName('headers' , 'tr' , thead);
                if (ths.length > 0) {
                    header = ths[0];
                    dim = {w: 0, h: 0};
                    for (i = 0, len = header.children.length; i < len; i++) {
                        child = header.children[i];
                        if (!Dom.hasClass(child, 'doclisting-name')) {
                            region = Dom.getRegion(child);
                            dim.w += region.width;
                        }
                    }

                    tds = Dom.getElementsByClassName('doclisting-name' , 'td' , tbody);
                    if(tds.length > 0) {
                        margin = this.helper.getMargin(tds[0]);
                        dim.w += margin.w;
                    }

                    tdWidth = sizes.wrap.w - dim.w;
                    for (i = 0, len = tds.length; i < len; i++) {
                        td = tds[i];
                        Dom.setStyle(td, 'width', tdWidth + 'px');
                        Dom.setStyle(td, 'display', 'block');
                        Dom.setStyle(td, 'overflow', 'hidden');
                    }
                }

                availableHeight = sizes.wrap.h;

                //first subtract height of table siblings
                siblings = Dom.getChildren(table.parentNode);
                for (i = 0, len = siblings.length; i < len; i++) {
                    sibling = siblings[i];
                    if (sibling !== table) {
                        availableHeight -= Dom.getRegion(sibling).height;
                    }
                }

                //then substract margin/padding/border value of table and height of header
                nonBodyHeight = this.helper.getMargin(table).h + Dom.getRegion(thead).height;
                availableHeight -= nonBodyHeight;

                //then try to detect paging element and subtract height which is a hard-coded value of 65 px
                //TODO: calculate footer height
                lastRowParentTag = rows[rows.length-1].parentNode.tagName;
                if (lastRowParentTag.toLowerCase() === 'tfoot') {
                    availableHeight -= 65;
                } else {
                    //Wicket 1.4 introduces a datatable with two tbody elements instead of thead/tfoot....
                    pagingRow = Dom.getElementsByClassName ( 'hippo-list-paging', 'tr', table);
                    if (Lang.isArray(pagingRow) && pagingRow.length > 0) {
                        availableHeight -= 65;
                    }
                }

                tbodyRegion = Dom.getRegion(tbody);

                scrolling = tbodyRegion.height > availableHeight;
                if(scrolling) {
                    Dom.setStyle(tbody, 'height', availableHeight + 'px');
                } else {
                    Dom.setStyle(tbody, 'height', tbodyRegion.height + 'px');
                }

            },

            getAutoWidthColumnMargin : function() {
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

        };
    }());

    YAHOO.hippo.TableHelper = new YAHOO.hippo.TableHelperImpl();
    
    YAHOO.register("TableHelper", YAHOO.hippo.TableHelper, {
        version: "2.8.1", build: "19"
    });
}
	
