/**
 * @description
 * <p>
 * Provides a singleton tables helper
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom
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
        	registered: false,
        	prevParentId: null,
        	
        	updateHeight: function() {
        		this.setHeight(this.prevParentId);
        	},	

    		setHeight: function(parentId) {
	        	var parentEl = Dom.get(parentId);
	        	var layoutContainer = Dom.getAncestorByClassName(parentId, 'yui-layout-bd');
	        	var parentRegion = Dom.getRegion(layoutContainer);
	        	var minHeight = 25; //topMargin
	        	var tfoot = Dom.getElementBy(function(el) { return true; }, 'tfoot', parentEl);
	        	if(typeof(tfoot.length) == 'undefined' ) {
	        		minHeight += 65;
	        	}
	        	var thead = Dom.getElementBy(function(el) { return true; }, 'thead', parentEl);
	        	var headRegion = Dom.getRegion(thead);
	        	minHeight += headRegion.height;
	
	        	var tbody = Dom.getElementBy(function(el) { return true; }, 'tbody', parentEl);
	        	var tbodyRegion = Dom.getRegion(tbody);
	        	var theHeight = parentRegion.height - minHeight;
	        	if(tbodyRegion.height > theHeight) {
	                if (YAHOO.env.ua.ie > 0) {
	                    //Couldn't really get the scrolling of a tbody working in IE so set the whole
	                    //unit to scrolling
	                    var firstDiv = Dom.getAncestorByTagName(parentId, 'div');
	                    var un = YAHOO.hippo.LayoutManager.findLayoutUnit(firstDiv);
	                    un.set('scroll', true);
	                } else {
	                    Dom.setStyle(tbody, 'height', theHeight + 'px');
	                }
	        	} else {
	        		Dom.setStyle(tbody, 'height', '');
	        	}
	        	
	            if(!this.registered) {
	            	var me  = this;
	            	YAHOO.hippo.LayoutManager.registerResizeListener(Dom.get(parentId), me, function() {
	            		me.updateHeight();
	            	}, false, false);
	            	this.registered = true;
	            }
	            
	            this.prevParentId = parentId;
        	}
        }
    })();

    YAHOO.hippo.TableHelper = new YAHOO.hippo.TableHelperImpl();
    
    YAHOO.register("TableHelper", YAHOO.hippo.TableHelper, {
        version: "2.7.0", build: "1799"            
    });
}
	
