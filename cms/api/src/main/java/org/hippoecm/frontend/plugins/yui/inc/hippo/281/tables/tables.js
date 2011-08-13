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
                if(Lang.isUndefined(tableEl['tableHelper'])) {
                    tableEl['tableHelper'] = new YAHOO.hippo.Table(id, config);
                }
                tableEl['tableHelper'].render();
            }
        }
        
        YAHOO.hippo.Table = function(id, config) {
            this.id = id;
            this.config = config;
            this.helper = new YAHOO.hippo.DomHelper();

            var table = Dom.get(id);
            var me = this;
            YAHOO.hippo.LayoutManager.registerResizeListener(table, this, function(sizes) {
                me.resize(sizes);
            }, false);
            YAHOO.hippo.HippoAjax.registerDestroyFunction(table, function() {
                YAHOO.hippo.LayoutManager.unregisterResizeListener(table, me);
            }, this);
        }
        
        YAHOO.hippo.Table.prototype = {

        	  resize: function(sizes) {
        	      this.update(sizes);
        	  },
        	
            render : function() {
                var table = Dom.get(this.id);
                var unit = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                if(unit != null) {
                    this.update(unit.getSizes());
                } else {
                    //We're not inside a layout unit to provide us with dimension details, thus the 
                    //resize event will never be called. For providing an initial size, the first ancestor
                    //with a classname is used.
                    var parent = Dom.getAncestorBy(table, function(node) {
                       return Lang.isValue(node.className) && Lang.trim(node.className).length > 0; 
                    });
                    if(parent != null) {
                        var reg = Dom.getRegion(parent);
                        var margin = this.helper.getMargin(parent);
                        this.update({wrap: {w: reg.width, h: reg.height}});
                    }
                }
            },
            
            update: function(sizes) {
                var table = Dom.get(this.id);

                if(YAHOO.env.ua.gecko > 0) {
                    this._updateGecko(sizes);
                    return;
                }

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

                var ownMargin = this.getOwnMargin();

                var autoWidthTds = Dom.getElementsByClassName('doclisting-name' , 'td' , tbody);
                if(autoWidthTds.length > 0) {
                    //reset tds width for correct calculation
                    for(var i=0; i<autoWidthTds.length; ++i) {
                        var td = autoWidthTds[i];
                        Dom.setStyle(td, 'width', 'auto');
                    }
                    if(ownMargin == null) {
                        ownMargin = this.helper.getMargin(autoWidthTds[0]);
                        //dim.w += margin.w;
                    }
                }


                var ths = Dom.getElementsByClassName('headers' , 'tr' , thead);
                if(ths.length > 0) {
                    var header = ths[0];
                    var dim = {w: 0, h: 0};
                    for(var i=0; i< header.children.length; i++) {
                        var child = header.children[i];
                        if(!Dom.hasClass(child, 'doclisting-name')) {
                            var region = Dom.getRegion(child);
                            dim.w += region.width;
                        }
                    }

                    var tds = Dom.getElementsByClassName('doclisting-name' , 'td' , tbody);
                    if(tds.length > 0) {
                        var margin = this.helper.getMargin(tds[0]);
                        dim.w += margin.w;
                    }

                    console.log('Unit width=' + sizes.wrap.w + ', other tds width=' + dim.w);
                    var tdWidth = sizes.wrap.w - dim.w;
                    for(var i=0; i<tds.length; ++i) {
                        var td = tds[i];
                        Dom.setStyle(td, 'width', tdWidth + 'px');
                        Dom.setStyle(td, 'display', 'block');
                        Dom.setStyle(td, 'overflow', 'hidden');
                    }
                }

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
                    console.log('set height with scrolling: ' + availableHeight);
                    Dom.setStyle(tbody, 'height', availableHeight + 'px');
                } else {
                    console.log('set height without scrolling: ' + tbodyRegion);
                    Dom.setStyle(tbody, 'height', tbodyRegion.height + 'px');
                }

            },

            getAutoWidthColumnMargin : function() {
                if(this.config.cacheEnabled) {

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

        }
    })();

    YAHOO.hippo.TableHelper = new YAHOO.hippo.TableHelperImpl();
    
    YAHOO.register("TableHelper", YAHOO.hippo.TableHelper, {
        version: "2.8.1", build: "19"
    });
}
	
