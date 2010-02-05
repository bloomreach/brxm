
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
        
            var start = new Date();
            var els = YAHOO.util.Dom.getElementsBy(function(node) {
                return !YAHOO.lang.isUndefined(node.HippoDestroyID);
            }, null, el);
            
            for(var i=0; i<els.length; i++) {
                YAHOO.hippo.HippoAjax.callDestroyFunction(els[i].HippoDestroyID);
            }
            var cleanupTook = 'Cleanup took ' + (new Date().getTime() - start.getTime()) + 'ms';
            YAHOO.log(cleanupTook, 'info', 'HippoAjax');
        
            start = new Date();
            YAHOO.util.Event.purgeElement(el, false);
        
            var purgeTook = 'Purge took ' + (new Date().getTime() - start.getTime()) + 'ms for element ' + el.id;
            YAHOO.log(purgeTook, 'info', 'HippoAjax');
            tmpFunc(steps, node);
        }


    })();

    YAHOO.register("hippoajax", YAHOO.hippo.HippoAjax, {
        version: "2.7.0", build: "1799"
    });
}

var HippoAjax = YAHOO.hippo.HippoAjax;
