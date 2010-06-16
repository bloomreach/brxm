/**
 * @description
 * <p>
 * Hippowidgets register with their ancestor layout units for rendering, resizing en destroying
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, layoutmanager, hippoajax
 * @module hippowidget
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.Widget) {
    (function() {
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.WidgetManagerImpl = function() {
            this.NAME = 'HippoWidget';
        };

        YAHOO.hippo.WidgetManagerImpl.prototype = {

            register : function(id, config) {
                var widget = Dom.get(id);
                if(Lang.isUndefined(widget[this.NAME])) {
                    widget[this.NAME] = new YAHOO.hippo.Widget(id, config);
                }
                widget[this.NAME].render();
            }
        };

        YAHOO.hippo.Widget = function(id, config) {
            this.id = id;
            this.config = config;

            var el = Dom.get(id);
            var me = this;
            YAHOO.hippo.LayoutManager.registerResizeListener(el, this, function(sizes) {
                me.resize(sizes);
            }, false);
            YAHOO.hippo.HippoAjax.registerDestroyFunction(el, function() {
                YAHOO.hippo.LayoutManager.unregisterResizeListener(el, me);
            }, this);
        };

        YAHOO.hippo.Widget.prototype = {

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
	            	var rows = table.rows;
	        	}
        }
    })();

    YAHOO.hippo.WidgetManager = new YAHOO.hippo.WidgetManagerImpl();

    YAHOO.register("hippowidget", YAHOO.hippo.WidgetManager, {
        version: "2.8.1", build: "19"
    });
}

