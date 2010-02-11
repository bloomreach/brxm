
/**
 * @description
 * <p>
 * Provides an interface for components to register on handling WicketAjax 
 * component update
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo, dom, hashmap
 * @module hippoajax
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.HippoAjax) { // Ensure only one hippo ajax exists
    ( function() {
        
        var Dom = YAHOO.util.Dom, Lang = YAHOO.lang;

        YAHOO.hippo.HippoAjaxImpl = function() {}
        
        YAHOO.hippo.HippoAjaxImpl.prototype = {
            prefix : 'hippo-destroyable-',
            callbacks : new YAHOO.hippo.HashMap(),
            _scrollbarWidth : null,

            getScrollbarWidth : function() {
                if(this._scrollbarWidth == null) {
                    var inner = document.createElement('p');
                    inner.style.width = "100%";
                    inner.style.height = "200px";

                    var outer = document.createElement('div');
                    outer.style.position = "absolute";
                    outer.style.top = "0px";
                    outer.style.left = "0px";
                    outer.style.visibility = "hidden";
                    outer.style.width = "200px";
                    outer.style.height = "150px";
                    outer.style.overflow = "hidden";
                    outer.appendChild (inner);

                    document.body.appendChild (outer);
                    var w1 = inner.offsetWidth;
                    outer.style.overflow = 'scroll';
                    var w2 = inner.offsetWidth;
                    if (w1 == w2) w2 = outer.clientWidth;

                    document.body.removeChild (outer);

                    this._scrollbarWidth = w1 - w2;
                }
                return this._scrollbarWidth;
            },
            
            getScrollbarHeight : function() {
                //I'm lazy so return scrollbarWidth for now
                return this.getScrollbarWidth();
            },

            registerDestroyFunction : function(el, func, context, args) {
                var id = this.prefix + Dom.generateId();
                el.HippoDestroyID = id;
                if(!Lang.isArray(args)) {
                    args = [args];
                }
                this.callbacks.put(id, {func: func, context: context, args: args});
            },
            
            callDestroyFunction : function(id) {
                if(this.callbacks.containsKey(id)) {
                    var callback = this.callbacks.remove(id);
                    callback.func.apply(callback.context, callback.args)
                }
            }
        }
        
        YAHOO.hippo.HippoAjax = new YAHOO.hippo.HippoAjaxImpl();
        
        var tmpFunc = Wicket.Ajax.Call.prototype.processComponent;
        Wicket.Ajax.Call.prototype.processComponent = function(steps, node) {
            var compId = node.getAttribute("id");
            var el = YAHOO.util.Dom.get(compId);

            var els = YAHOO.util.Dom.getElementsBy(function(node) {
                return !YAHOO.lang.isUndefined(node.HippoDestroyID);
            }, null, el);
            
            for(var i=0; i<els.length; i++) {
                YAHOO.hippo.HippoAjax.callDestroyFunction(els[i].HippoDestroyID);
            }
            YAHOO.util.Event.purgeElement(el, false);
            tmpFunc(steps, node);
        }

    })();

    YAHOO.register("hippoajax", YAHOO.hippo.HippoAjax, {
        version: "2.7.0", build: "1799"
    });
}

var HippoAjax = YAHOO.hippo.HippoAjax;
