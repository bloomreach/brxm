
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

    })();

    YAHOO.register("hippoajax", YAHOO.hippo.HippoAjax, {
        version: "2.7.0", build: "1799"
    });
}

var HippoAjax = YAHOO.hippo.HippoAjax;

//Fix for IE's, comment first line in eval text

// Process a script element (both inline and external)
Wicket.Head.Contributor.prototype.processScript = function(steps, node) {
    steps.push(function(notify) {       
        // if element in same id is already in document, 
        // or element with same src attribute is in document, skip it
        if (Wicket.DOM.containsElement(node) ||
            Wicket.Head.containsElement(node, "src")) {
            notify(); 
            return;
        }
        
        // determine whether it is external javascript (has src attribute set)
        var src = node.getAttribute("src");
        
        if (src != null && src != "") {
            // load the external javascript using Wicket.Ajax.Request
            
            // callback when script is loaded
            var onLoad = function(content) {                    
                Wicket.Head.addJavascript(content, null, src);
                Wicket.Ajax.invokePostCallHandlers();

                // continue to next step
                notify();
            }
            // we need to schedule the request as timeout
            // calling xml http request from another request call stack doesn't work
            window.setTimeout(function() {
                var req = new Wicket.Ajax.Request(src, onLoad, false, false);
                req.debugContent = false;
                if (Wicket.Browser.isKHTML())
                    // konqueror can't process the ajax response asynchronously, therefore the 
                    // javascript loading must be also synchronous
                    req.async = false;
                // get the javascript
                req.get();                  
            },1);
        } else {
            // serialize the element content to string
            var text = Wicket.DOM.serializeNodeChildren(node);
            
            var id = node.getAttribute("id");
            
            if (typeof(id) == "string" && id.length > 0) {                  
                // add javascript to document head
                Wicket.Head.addJavascript(text, id);
            } else {
                try {
                    eval('//' + text);
                } catch (e) {
                    Wicket.Log.error(e);
                }
            }
            
            // continue to next step
            notify();
        }
    });                 
}

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
    YAHOO.util.Event.purgeElement(el, true)

    tmpFunc(steps, node);
}
