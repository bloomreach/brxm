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
            
            register : function(id) {
                var tableEl = Dom.get(id);
                if(Lang.isUndefined(tableEl['tableHelper'])) {
                    tableEl['tableHelper'] = new YAHOO.hippo.Table(id);
                }
                tableEl['tableHelper'].render();
            }
        }
        
        YAHOO.hippo.Table = function(id) {
            this.init(id);
        }
        
        YAHOO.hippo.Table.prototype = {
            id: null,
            helper: new YAHOO.hippo.DomHelper(),
        	
        	init: function(id) {
                this.id = id;
                
                var table = Dom.get(id);
                var me = this;
                YAHOO.hippo.LayoutManager.registerResizeListener(table, this, function(sizes) {
                    me.resize(sizes);
                }, false);
                YAHOO.hippo.HippoAjax.registerDestroyFunction(table, function() {
                    YAHOO.hippo.LayoutManager.unregisterResizeListener(table, me);
                }, this);
        	},
        	
        	resize: function(sizes) {
        	    this.update(sizes);
        	},
        	
            render : function() {
                var table = Dom.get(this.id);
                var unit = YAHOO.hippo.LayoutManager.findLayoutUnit(table);
                if(unit != null) {
                    this.update(unit.getSizes());
                } else {
                    var parent = Dom.getAncestorByTagName(table, 'div');
                    var reg = Dom.getRegion(parent);
                    var margin = this.helper.getMargin(parent);
                    this.update({wrap: {w: reg.width, h: reg.height}});
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
                var nonBodyHeight = this.helper.getMargin(table).h + Dom.getRegion(rows[0].parentNode).height;

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
                        Dom.setStyle(tbody, 'height', availableHeight + 'px');
	        	    } else {
	        	        Dom.setStyle(tbody, 'height', tbodyRegion.height + 'px');   
	        	    }
	        	}
        	}
        }
    })();

    YAHOO.hippo.TableHelper = new YAHOO.hippo.TableHelperImpl();
    
    YAHOO.register("TableHelper", YAHOO.hippo.TableHelper, {
        version: "2.7.0", build: "1799"            
    });
}
	
