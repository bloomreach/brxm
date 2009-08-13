
/**
 * @description
 * <p>
 * Provides an interface for components to register on handling WicketAjax 
 * component update
 * </p>
 * @namespace YAHOO.hippo
 * @requires yahoo
 * @module hippoajax
 * @beta
 */

YAHOO.namespace('hippo');

if (!YAHOO.hippo.HippoAjax) { // Ensure only one hippo ajax exists
    ( function() {

        YAHOO.hippo.HippoAjax = {};

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